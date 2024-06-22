package net.explorviz.userapi.resources;

import io.quarkus.test.junit.QuarkusMock;
import io.quarkus.test.junit.QuarkusTest;
import net.explorviz.userapi.model.UserApi;
import net.explorviz.userapi.persistence.UserApiRepository;
import net.explorviz.userapi.service.UserApiInMemRepo;
import net.explorviz.userapi.service.UserApiServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

@QuarkusTest
public class UserApiResourceTest {

  UserApiRepository repo;
  UserApiInMemRepo inMemRepo;
  UserApiServiceImpl userApiService;

  @BeforeEach
  void setUp() {

    this.repo = Mockito.mock(UserApiRepository.class);
    QuarkusMock.installMockForType(this.repo, UserApiRepository.class);

    this.inMemRepo = new UserApiInMemRepo();
    Mockito.doAnswer(invocation -> {
      this.inMemRepo.addApi(invocation.getArgument(0));
      return null;
    }).when(this.repo).persist(ArgumentMatchers.any(UserApi.class));

    Mockito.when(this.repo.findForUser(ArgumentMatchers.anyString()))
        .thenAnswer(invocation -> this.inMemRepo.findForUser(invocation.getArgument(0)));

    Mockito.when(this.repo.findForUserAndToken(ArgumentMatchers.anyString(),
        ArgumentMatchers.anyString()))
        .thenAnswer(invocation ->
            this.inMemRepo.findForUserAndToken(invocation.getArgument(0),
                invocation.getArgument(1)));
  }

  @Test
  public void testUserApiCreationEndpoint() {

    final String uid = "testuid";
    final String name = "testname";
    final String token ="testtoken";
    final String url = "testurl";
    final long createdAt = 1700000L;
    final long expires = 0L;

    this.userApiService = Mockito.mock(UserApiServiceImpl.class);
    QuarkusMock.installMockForType(this.userApiService, UserApiServiceImpl.class);
    Mockito.when(this.userApiService.tokenExists(ArgumentMatchers.anyString(),
            ArgumentMatchers.anyString()))
        .thenAnswer(invocation -> false);

    given().params("uId",uid,
            "name", name,
            "token", token,
            "hostUrl", url,
            "createdAt", createdAt,
            "expires", expires)
        .when().post("userapi/create/").then().statusCode(200);

  }

  @Test
  public void testUserApiCreationOfExistingEndpoint() {

    final String uid = "testuid";
    final String name = "testname";
    final String token ="testtoken";
    final String url = "testurl";
    final long createdAt = 1700000L;
    final long expires = 0L;

    Mockito.when(this.repo.findForUserAndToken(ArgumentMatchers.any(),
            ArgumentMatchers.any()))
        .thenAnswer(invocation -> this.inMemRepo.findForUserAndToken(uid, token));

    this.repo.persist(new UserApi(uid, name, token, url, createdAt, expires));

    given().params("uId",uid,
            "name", name,
            "token", token,
            "hostUrl", url,
            "createdAt", createdAt,
            "expires", expires)
        .when().post("userapi/create/").then().statusCode(422);

  }

  @Test
  public void testUserApiRetrieveEmpty() {
    given().when().get("userapi/").then().statusCode(200)
        .body("size()", is(0));
  }

  @Test
  public void testUserApiRetrieve() {
    final String uid = "testuid";
    final String name = "testname";
    final String token = "testtoken";
    final String hostUrl = "testurl";
    final long createdAt = 17L;
    final long expires = 0L;

    this.repo.persist(new UserApi(uid, name, token, hostUrl, createdAt, expires));

    given().params("uId", uid).when().get("userapi/").then()
        .statusCode(200)
        .body("size()", is(1)).body("[0].uid", is(uid)).body("[0].name", is(name))
        .body("[0].token", is(token)).body("[0].hostUrl", is(hostUrl));
  }

  @Test
  void deleteUserApi() {
    final String uid = "testuid";
    final String token = "testtoken";

    Mockito.when(this.repo.findForUserAndToken(ArgumentMatchers.any(),
            ArgumentMatchers.any()))
        .thenAnswer(invocation -> this.inMemRepo.findForUserAndToken(uid, token));
    Mockito.when(this.repo.delete(ArgumentMatchers.anyString(),
        ArgumentMatchers.<String>any()))
        .thenAnswer(invocation -> this.inMemRepo.deleteByValue(uid, token));

    this.repo.persist(new UserApi(uid, "test", token,
        "test", 170000000L, 0L));

    given().params("uId", uid,
    "token", token)
        .when().delete("userapi/delete/").then().statusCode(200);
  }

  @Test
  void deleteNonExistingUserApi() {
    final String uid = "testuid";
    final String token = "testtoken";

    Mockito.when(this.repo.findForUserAndToken(ArgumentMatchers.any(),
            ArgumentMatchers.any()))
        .thenAnswer(invocation -> this.inMemRepo.findForUserAndToken(uid, token));
    Mockito.when(this.repo.delete(ArgumentMatchers.anyString(),
            ArgumentMatchers.<String>any()))
        .thenAnswer(invocation -> this.inMemRepo.deleteByValue(uid, token));

    given().params("uId", uid,
            "token", token)
        .when().delete("userapi/delete/").then().statusCode(400);
  }

}
