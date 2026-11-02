package net.explorviz.demo;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.List;
import net.explorviz.snapshot.model.Snapshot;
import net.explorviz.snapshot.resources.SnapshotResource;
import net.explorviz.snapshot.service.SnapshotService;
import net.explorviz.userapi.model.UserApi;
import net.explorviz.userapi.resources.UserApiResource;
import net.explorviz.userapi.service.UserApiService;
import org.bson.Document;
import org.junit.jupiter.api.Test;

class DemoModeReadTest {
  private void enable(Object resource) throws Exception {
    DemoMode mode = new DemoMode();
    mode.enabled = true;
    var field = resource.getClass().getDeclaredField("demoMode");
    field.setAccessible(true);
    field.set(resource, mode);
  }

  @Test
  void readingSnapshotsDoesNotDeleteExpiredRecords() throws Exception {
    SnapshotService service = mock(SnapshotService.class);
    Snapshot expired = mock(Snapshot.class);
    when(expired.getDeleteAt()).thenReturn(1L);
    when(expired.getSubscribedUsers()).thenReturn(new Document());
    when(service.getAllSnapshots()).thenReturn(List.of(expired));
    SnapshotResource resource = new SnapshotResource(service);
    enable(resource);
    assertNotNull(resource.getSnapshotByValue("user"));
    verify(service, never()).deleteByValue(any(), any(), anyBoolean());
  }

  @Test
  void openingSharedSnapshotDoesNotSubscribeViewer() throws Exception {
    SnapshotService service = mock(SnapshotService.class);
    SnapshotResource resource = new SnapshotResource(service);
    enable(resource);
    resource.getSnapshot("owner", 1L, true, "viewer");
    verify(service, never()).addNewSubscriber(any(), any(), any());
    verify(service).getSnapshot("owner", 1L, true);
  }

  @Test
  void readingApiTokensDoesNotDeleteExpiredRecords() throws Exception {
    UserApiService service = mock(UserApiService.class);
    UserApi expired = mock(UserApi.class);
    when(expired.getExpires()).thenReturn(1L);
    when(service.getOwningTokens("user")).thenReturn(new ArrayList<>(List.of(expired)));
    UserApiResource resource = new UserApiResource(service);
    enable(resource);
    assertEquals(1, resource.getUserByValue("user").size());
    verify(service, never()).deleteByValue(any(), any());
  }
}
