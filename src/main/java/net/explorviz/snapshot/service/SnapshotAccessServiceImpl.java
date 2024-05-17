package net.explorviz.snapshot.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import net.explorviz.snapshot.model.Snapshot;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * Implementation of {@link SnapshotAccessService}.
 *
 * @see SnapshotAccessService
 */
@ApplicationScoped
public class SnapshotAccessServiceImpl implements SnapshotAccessService {

  @ConfigProperty(name = "quarkus.oidc.enabled", defaultValue = "true")
  Instance<Boolean> authEnabled;

  @Override
  public SnapshotPermission[] getPermissions(final Snapshot snapshot, final String uId){

    if (!this.authEnabled.get()){
      return new SnapshotPermission[] {SnapshotPermission.DELETE, SnapshotPermission.READ};
    }

    if (snapshot.getOwner().equals(uId)) {
      return new SnapshotPermission[] {SnapshotPermission.READ, SnapshotPermission.DELETE};
    }

    return new SnapshotPermission[] {};
  }
}
