package com.objectcomputing.checkins.services.pulseresponse;

import io.micronaut.test.annotation.MicronautTest;
import io.micronaut.validation.validator.Validator;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;
import javax.validation.ConstraintViolation;
import java.util.Set;
import java.util.UUID;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

@MicronautTest
public class PulseReponseCreateDTOTest {

    @Inject
    private Validator validator;

    @Test
    void testDTOInstantiation() {
        PulseResponseCreateDTO dto = new PulseResponseCreateDTO();
        assertNull(dto.getSubmissionDate());
        assertNull(dto.getUpdatedDate());
        assertNull(dto.getTeamMemberId());
        assertNull(dto.getInternalFeelings());
        assertNull(dto.getExternalFeelings());
    }

    @Test
    void testConstraintViolation() {
        PulseResponseCreateDTO dto = new PulseResponseCreateDTO();

        Set<ConstraintViolation<PulseResponseCreateDTO>> violations = validator.validate(dto);
        assertEquals(violations.size(), 5);
        for (ConstraintViolation<PulseResponseCreateDTO> violation : violations) {
            assertEquals(violation.getMessage(), "must not be null");
        }
    }

    @Test
    void testPopulatedDTO() {
        PulseResponseCreateDTO dto = new PulseResponseCreateDTO();

        UUID teamMemberId = UUID.randomUUID();

        dto.setSubmissionDate(LocalDate.of(2019, 1, 01));
        assertEquals(dto.getSubmissionDate(), LocalDate.of(2019, 1, 01));

        dto.setUpdatedDate(LocalDate.of(2019, 1, 01));
        assertEquals(dto.getUpdatedDate(), LocalDate.of(2019, 1, 01));

        dto.setTeamMemberId(teamMemberId);
        assertEquals(dto.getTeamMemberId(), teamMemberId);

        dto.setInternalFeelings("ExampleId");
        assertEquals("ExampleId", dto.getInternalFeelings());

        dto.setExternalFeelings("ExampleId");
        assertEquals("ExampleId", dto.getExternalFeelings());

        Set<ConstraintViolation<PulseResponseCreateDTO>> violations = validator.validate(dto);
        assertTrue(violations.isEmpty());
    }
}
