/*
 * Copyright 2019 JSquad AB
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

package se.jsquad.rest;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.AnnotationConfigWebContextLoader;
import org.springframework.test.context.web.WebAppConfiguration;
import se.jsquad.batch.SlowMockBatch;
import se.jsquad.batch.SlowMockBatchImpl;
import se.jsquad.batch.status.BatchStatus;
import se.jsquad.batch.status.Status;
import se.jsquad.configuration.ApplicationConfiguration;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(SpringExtension.class)
@WebAppConfiguration
@ContextConfiguration(classes = ApplicationConfiguration.class, loader = AnnotationConfigWebContextLoader.class)
class OpenBankRestControllerTest {
    @Autowired
    private OpenBankRestController openBankRestController;

    @Autowired
    private SlowMockBatch slowMockBatch;

    @Test
    public void testGetSlowMockBatchStatus() throws NoSuchFieldException, IllegalAccessException {
        // Given
        int seconds = 0;

        Field field = ((SlowMockBatchImpl) slowMockBatch).getClass().getDeclaredField("sleepTime");
        field.setAccessible(true);

        field.set(((SlowMockBatchImpl) slowMockBatch), seconds);

        // When
        ResponseEntity responseEntity = openBankRestController.getOpenBankBatchStatus();
        BatchStatus batchStatus = (BatchStatus) responseEntity.getBody();

        // Then
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(Status.SUCCESS, batchStatus.getStatus());
        assertEquals("Batch job went just fine.", batchStatus.getMessage());
    }

}
