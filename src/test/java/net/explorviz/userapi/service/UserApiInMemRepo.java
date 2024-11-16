package net.explorviz.userapi.service;

import net.explorviz.userapi.model.UserApi;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class UserApiInMemRepo {

  private final List<UserApi> userApis = new ArrayList<>();

  public void addApi(final UserApi userApi) {
    this.userApis.add(userApi);
  }

  public Collection<UserApi> findForUser(final String uid) {
    return this.userApis.stream().filter(a -> a.getUid().equals(uid)).collect(Collectors.toList());
  }

  public Collection<UserApi> findForUserAndToken(final String uid, final String token) {
    return this.userApis.stream().filter(a -> a.getUid().equals(uid) && a.getToken().equals(token)).collect(
        Collectors.toList());
  }

  public long deleteByValue(final String uid, final String token) {
    final boolean d =
        this.userApis.removeIf(a -> a.getUid().equals(uid) && a.getToken().equals(token));
    return d ? 1L : 0L;
  }

  public int size() {
    return this.userApis.size();
  }

}
