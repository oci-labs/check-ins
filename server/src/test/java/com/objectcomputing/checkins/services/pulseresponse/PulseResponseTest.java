package com.objectcomputing.checkins.services.pulseresponse;

import io.micronaut.test.annotation.MicronautTest;
import io.micronaut.validation.validator.Validator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import javax.inject.Inject;
import javax.validation.ConstraintViolation;
import java.util.Set;
import java.util.UUID;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertTrue;

@MicronautTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class PulseResponseTest {

    @Inject
    private Validator validator;

    @Test
    void testPulseResponseInstantiation() {
        LocalDate submissionDate= LocalDate.of(2019, 1, 01);
        LocalDate updatedDate= LocalDate.of(2019, 1, 01);
        final UUID teamMemberId = UUID.randomUUID();
        final String internalFeelings  = "exampleId";
        final String externalFeelings  = "exampleId2";
        PulseResponse pulseResponse = new PulseResponse(submissionDate,updatedDate, teamMemberId, internalFeelings, externalFeelings);
        assertEquals(teamMemberId, pulseResponse.getTeamMemberId());
        assertEquals(internalFeelings , pulseResponse.getInternalFeelings ());
        assertEquals(externalFeelings , pulseResponse.getExternalFeelings ());
    }

    @Test
    void testConstraintViolation() {
        LocalDate submissionDate= LocalDate.of(2019, 1, 01);
        LocalDate updatedDate= LocalDate.of(2019, 1, 01);
        final UUID teamMemberId = UUID.randomUUID();
        final String internalFeelings  = "exampleId";
        final String externalFeelings  = "exampleId2";
        PulseResponse pulseResponse = new PulseResponse(submissionDate,updatedDate, teamMemberId, internalFeelings,externalFeelings);

        pulseResponse.setInternalFeelings (null);
        pulseResponse.setExternalFeelings (null);

        Set<ConstraintViolation<PulseResponse>> violations = validator.validate(pulseResponse);
        assertEquals(2, violations.size());
        for (ConstraintViolation<PulseResponse> violation : violations) {
            assertEquals(violation.getMessage(), "must not be null");
        }
    }

    @Test
    void testEquals() {
        final UUID id = UUID.randomUUID();
        LocalDate submissionDate= LocalDate.of(2019, 1, 01);
        LocalDate updatedDate= LocalDate.of(2019, 1, 01);
        final UUID teamMemberId = UUID.randomUUID();
        final String internalFeelings  = "exampleId";
        final String externalFeelings   = "exampleId2";

        PulseResponse pulseResponse1 = new PulseResponse(id,submissionDate,updatedDate, teamMemberId, internalFeelings, externalFeelings );
        PulseResponse pulseResponse2 = new PulseResponse(id,submissionDate,updatedDate, teamMemberId, internalFeelings, externalFeelings );
        assertEquals(pulseResponse1, pulseResponse2);

        pulseResponse2.setId(null);
        assertNotEquals(pulseResponse1, pulseResponse2);

        pulseResponse2.setId(pulseResponse1.getId());
        assertEquals(pulseResponse1, pulseResponse2);

        pulseResponse2.setInternalFeelings ("exampleId2");
        pulseResponse2.setExternalFeelings ("exampleId3");
        assertNotEquals(pulseResponse1, pulseResponse2);
    }

    @Test
    void testToString() {
        final UUID id = UUID.randomUUID();
        LocalDate submissionDate= LocalDate.of(2019, 1, 01);
        LocalDate updatedDate= LocalDate.of(2019, 1, 01);
        final UUID teamMemberId = UUID.randomUUID();
        final String internalFeelings  = "exampleId";
        final String externalFeelings  = "exampleId2";
        PulseResponse pulseResponse = new PulseResponse(id,submissionDate,updatedDate, teamMemberId, internalFeelings, externalFeelings );

        String toString = pulseResponse.toString();
        assertTrue(toString.contains(teamMemberId.toString()));
        assertTrue(toString.contains(id.toString()));
        assertTrue(toString.contains(internalFeelings ));
        assertTrue(toString.contains(externalFeelings ));
    }
}
