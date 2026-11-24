package net.explorviz.persistence;

import com.mongodb.MongoException;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Accumulators;
import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.model.Sorts;
import io.quarkus.runtime.StartupEvent;
import io.quarkus.scheduler.Scheduled;
import io.quarkus.scheduler.Scheduled.ConcurrentExecution;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.interceptor.Interceptor;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import net.explorviz.demo.DemoMode;
import net.explorviz.snapshot.persistence.SnapshotRepository;
import net.explorviz.token.persistence.LandscapeTokenRepository;
import net.explorviz.userapi.persistence.UserApiRepository;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Creates the indexes of all collections, as Panache does not support declaring them. */
@ApplicationScoped
public class DatabaseIndexes {

  private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseIndexes.class);
  private static final String ID = "_id";

  @Inject /* default */ DemoMode demoMode;
  @Inject /* default */ LandscapeTokenRepository tokens;
  @Inject /* default */ SnapshotRepository snapshots;
  @Inject /* default */ UserApiRepository userApis;

  @ConfigProperty(name = "explorviz.mongodb.indexes.enabled")
  /* default */ boolean enabled;

  private volatile boolean created;

  /** Runs before other startup observers, as these insert data. */
  /* default */ void onStart(
      @Observes @Priority(Interceptor.Priority.LIBRARY_BEFORE) final StartupEvent ev) {
    this.createIfMissing();
  }

  /** Retries in case the database was not available at startup. */
  @Scheduled(every = "1m", delayed = "1m", concurrentExecution = ConcurrentExecution.SKIP)
  /* default */ void createIfMissing() {
    if (!this.enabled || this.created || this.demoMode.isEnabled()) {
      return;
    }
    try {
      this.create();
      this.created = true;
    } catch (final MongoException e) {
      LOGGER.error("Creating database indexes failed, trying again later", e);
    }
  }

  /** Creates all indexes, does nothing for indexes that already exist. */
  public void create() {
    final MongoCollection<Document> tokenCollection = documents(this.tokens.mongoCollection());
    createUnique(tokenCollection, Set.of(), "value");
    tokenCollection.createIndex(Indexes.ascending("owner"));
    tokenCollection.createIndex(Indexes.ascending("sharedUsers"));
    tokenCollection.createIndex(
        Indexes.ascending(LandscapeTokenRepository.LEASE_UNTIL), new IndexOptions().sparse(true));

    final MongoCollection<Document> snapshotCollection =
        documents(this.snapshots.mongoCollection());
    createUnique(snapshotCollection, Set.of(), "owner", "createdAt", "isShared");
    snapshotCollection.createIndex(Indexes.ascending("subscribedUsers.subscriberList"));
    snapshotCollection.createIndex(Indexes.ascending("deleteAt"));

    final MongoCollection<Document> userApiCollection = documents(this.userApis.mongoCollection());
    // Former versions stored the initial API token again on every start
    createUnique(userApiCollection, Set.of("createdAt"), "uid", "token");
    userApiCollection.createIndex(Indexes.ascending("expires"));
  }

  private static MongoCollection<Document> documents(final MongoCollection<?> collection) {
    return collection.withDocumentClass(Document.class);
  }

  /**
   * Creates a unique index. Redundant documents that would prevent this are deleted. If there are
   * documents with the same key but different contents, the index is not created, since it is
   * unknown which of them should be kept.
   */
  private static void createUnique(
      final MongoCollection<Document> collection,
      final Set<String> irrelevantFields,
      final String... keys) {
    try {
      createUnique(collection, keys);
    } catch (final MongoException e) {
      if (!DuplicateKeys.isDuplicateKey(e)) {
        throw e;
      }
      final int conflicts = deleteRedundant(collection, irrelevantFields, keys);
      if (conflicts > 0) {
        LOGGER.error(
            "Cannot enforce unique {} in collection {}: {} keys have documents with different "
                + "contents. Please remove duplicates manually and restart the service.",
            List.of(keys),
            collection.getNamespace().getCollectionName(),
            conflicts);
        return;
      }
      createUnique(collection, keys);
    }
  }

  private static void createUnique(
      final MongoCollection<Document> collection, final String... keys) {
    collection.createIndex(Indexes.ascending(keys), new IndexOptions().unique(true));
  }

  private static int deleteRedundant(
      final MongoCollection<Document> collection,
      final Set<String> irrelevantFields,
      final String... keys) {
    final Document groupKey = new Document();
    for (final String key : keys) {
      groupKey.append(key, "$" + key);
    }
    final List<Bson> duplicates =
        List.of(
            Aggregates.group(
                groupKey, Accumulators.push("ids", "$" + ID), Accumulators.sum("count", 1)),
            Aggregates.match(Filters.gt("count", 1)));

    int conflicts = 0;
    for (final Document group : collection.aggregate(duplicates).allowDiskUse(true)) {
      final List<Document> documents =
          collection
              .find(Filters.in(ID, group.getList("ids", Object.class)))
              .sort(Sorts.ascending(ID))
              .into(new ArrayList<>());
      boolean conflict = false;
      for (final Document candidate : documents.subList(1, documents.size())) {
        if (contents(candidate, irrelevantFields)
            .equals(contents(documents.getFirst(), irrelevantFields))) {
          collection.deleteOne(Filters.eq(ID, candidate.get(ID)));
          LOGGER.info(
              "Deleted redundant document {} in collection {}",
              candidate.get(ID),
              collection.getNamespace().getCollectionName());
        } else {
          conflict = true;
        }
      }
      if (conflict) {
        conflicts++;
      }
    }
    return conflicts;
  }

  private static Document contents(final Document document, final Set<String> irrelevantFields) {
    final Document contents = new Document(document);
    contents.remove(ID);
    contents.keySet().removeAll(irrelevantFields);
    return contents;
  }
}
