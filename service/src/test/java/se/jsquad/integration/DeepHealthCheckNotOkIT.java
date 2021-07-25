/*
 * Copyright 2021 JSquad AB
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package se.jsquad.integration;

import io.kubernetes.client.openapi.ApiException;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import se.jsquad.api.health.DeepSystemStatusResponse;
import se.jsquad.api.health.HealthStatus;

import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static se.jsquad.integration.RyukIntegration.BASE_PATH_ACTUATOR;
import static se.jsquad.integration.RyukIntegration.OPENBANK_MONITORING;
import static se.jsquad.integration.RyukIntegration.PROTOCOL_HTTP;

public class DeepHealthCheckNotOkIT extends AbstractTestContainerSetup {
    @Test
    void testDeepHealthCheckNotOk() throws NoSuchMethodException, InvocationTargetException,
        IllegalAccessException, ApiException, MalformedURLException, URISyntaxException {
        // Given
        executeContainerPodCLI("openbankdb", "KILL");
        executeContainerPodCLI("securitydb", "KILL");
    
        Awaitility.await().pollDelay(Duration.ofSeconds(5)).atMost(Duration.ofMinutes(10)).until(() -> {
            Response response = RestAssured
                .given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .when()
                .get(toURI(BASE_PATH_ACTUATOR + "/deephealth", PROTOCOL_HTTP, OPENBANK_MONITORING)).andReturn();
            
            DeepSystemStatusResponse deepSystemStatusResponse = gson.fromJson(response.getBody().asString(),
                DeepSystemStatusResponse.class);
    
            return 200 == response.getStatusCode() && HealthStatus.DOWN.equals(deepSystemStatusResponse.getStatus())
                && HealthStatus.UP.equals(deepSystemStatusResponse.getService())
                && HealthStatus.DOWN.equals(deepSystemStatusResponse.getDependencies().getOpenbankDb());
        });

        // When
        Response response = RestAssured
                .given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .when()
                .get(toURI(BASE_PATH_ACTUATOR + "/deephealth", PROTOCOL_HTTP, OPENBANK_MONITORING)).andReturn();

        // Then
        DeepSystemStatusResponse deepSystemStatusResponse = gson.fromJson(response.getBody().asString(),
                DeepSystemStatusResponse.class);

        assertEquals(200, response.getStatusCode());
        assertEquals(HealthStatus.DOWN, deepSystemStatusResponse.getStatus());
        assertEquals(HealthStatus.UP, deepSystemStatusResponse.getService());
        assertEquals(HealthStatus.DOWN, deepSystemStatusResponse.getDependencies().getOpenbankDb());
        assertEquals(HealthStatus.DOWN, deepSystemStatusResponse.getDependencies().getSecurityDb());
    
        executeContainerPodCLI("openbank", "KILL");
        executeContainerPodCLI("openbankdb", "START");
        executeContainerPodCLI("securitydb", "START");
        executeContainerPodCLI("openbank", "START");
    
        Awaitility.await().pollDelay(Duration.ofSeconds(5)).atMost(Duration.ofMinutes(10)).until(() -> {
            try {
                Response response1 = RestAssured
                    .given()
                    .contentType(ContentType.JSON)
                    .accept(ContentType.JSON)
                    .when()
                    .get(toURI(BASE_PATH_ACTUATOR + "/deephealth", PROTOCOL_HTTP, OPENBANK_MONITORING)).andReturn();
    
                DeepSystemStatusResponse deepSystemStatusResponse1 = gson.fromJson(response1.getBody().asString(),
                    DeepSystemStatusResponse.class);
    
                return 200 == response1.getStatusCode() && HealthStatus.UP.equals(deepSystemStatusResponse1.getStatus())
                    && HealthStatus.UP.equals(deepSystemStatusResponse1.getService())
                    && HealthStatus.UP.equals(deepSystemStatusResponse1.getDependencies().getOpenbankDb());
            } catch (Exception e) {
                return false;
            }
        });
    }
}