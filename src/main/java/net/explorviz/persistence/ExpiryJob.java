package net.explorviz.persistence;

import io.quarkus.scheduler.Scheduled;
import io.quarkus.scheduler.Scheduled.ConcurrentExecution;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import net.explorviz.demo.DemoMode;
import net.explorviz.snapshot.persistence.SnapshotRepository;
import net.explorviz.userapi.persistence.UserApiRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Deletes expired snapshots and API tokens. */
@ApplicationScoped
public class ExpiryJob {

  private static final Logger LOGGER = LoggerFactory.getLogger(ExpiryJob.class);

  @Inject /* default */ DemoMode demoMode;
  @Inject /* default */ SnapshotRepository snapshots;
  @Inject /* default */ UserApiRepository userApis;

  /** Deletes everything that has expired by now. */
  @Scheduled(every = "${explorviz.expiry.interval}", concurrentExecution = ConcurrentExecution.SKIP)
  public void deleteExpired() {
    if (this.demoMode.isEnabled()) {
      return;
    }
    final long now = System.currentTimeMillis();
    final long deletedSnapshots = this.snapshots.deleteExpired(now);
    final long deletedUserApis = this.userApis.deleteExpired(now);
    if (deletedSnapshots + deletedUserApis > 0) {
      LOGGER.info(
          "Deleted {} expired snapshots and {} expired API tokens",
          deletedSnapshots,
          deletedUserApis);
    }
  }
}
