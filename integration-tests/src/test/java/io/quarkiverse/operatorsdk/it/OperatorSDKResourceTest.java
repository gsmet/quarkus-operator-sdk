package io.quarkiverse.operatorsdk.it;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.kubernetes.client.KubernetesServerTestResource;

@QuarkusTest
@QuarkusTestResource(KubernetesServerTestResource.class)
public class OperatorSDKResourceTest {

    @Test
    void shouldNotValidateCRs() {
        given().when().get("/operator/config").then().statusCode(200).body(
                "validate", equalTo(false));
    }

    @Test
    void shouldHavePropertiesDefinedReconciliationThreads() {
        given().when().get("/operator/config").then().statusCode(200).body(
                "maxThreads", equalTo(10));
    }

    @Test
    void shouldHavePropertiesDefinedTerminationTimeout() {
        given().when().get("/operator/config").then().statusCode(200).body(
                "timeout", equalTo(20));
    }

    @Test
    void controllerShouldExist() {
        // first check that we're not always returning true for any controller name :)
        given().when().get("/operator/does_not_exist").then().statusCode(200).body(is("false"));

        // given the name of the TestController, the app should reply true meaning that it is indeed
        // injected
        given().when().get("/operator/" + TestController.NAME).then().statusCode(200)
                .body(is("true"));
    }

    @Test
    void configurationForControllerShouldExist() {
        // check that the config for the test controller can be retrieved and is conform to our
        // expectations
        final var resourceName = ChildTestResource.class.getCanonicalName();
        given()
                .when()
                .get("/operator/" + TestController.NAME + "/config")
                .then()
                .statusCode(200)
                .body(
                        "customResourceClass", equalTo(resourceName),
                        "name", equalTo(TestController.NAME));
    }

    @Test
    void applicationPropertiesShouldOverrideDefaultAndAnnotation() {
        given()
                .when()
                .get("/operator/" + ConfiguredController.NAME + "/config")
                .then()
                .statusCode(200)
                .body(
                        "finalizer", equalTo("from-property/finalizer"),
                        "namespaces", hasItem("bar"));

        given()
                .when()
                .get("/operator/" + ApplicationScopedController.NAME + "/config")
                .then()
                .statusCode(200)
                .body("namespaces", hasItem("default"));
    }

    @Test
    void delayedControllerShouldWaitForEventToRegister() {
        // first check that the delayed controller is not registered, though it should be a known controller
        given().when().get("/operator/registered/" + DelayedController.NAME).then().statusCode(200).body(is("false"));
        given().when().get("/operator/" + DelayedController.NAME).then().statusCode(200).body(is("true"));
        given()
                .when()
                .get("/operator/" + DelayedController.NAME + "/config")
                .then()
                .statusCode(200)
                .body("delayed", equalTo(true));

        // call the register endpoint to trigger the event that the DelayedController is waiting for
        given().when().post("/operator/register").then().statusCode(204);

        // and check that the controller is now registered
        given().when().get("/operator/registered/" + DelayedController.NAME).then().statusCode(200).body(is("true"));
    }
}
