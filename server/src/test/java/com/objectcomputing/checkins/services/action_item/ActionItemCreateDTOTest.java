package com.objectcomputing.checkins.services.action_item;

import io.micronaut.test.annotation.MicronautTest;
import io.micronaut.validation.validator.Validator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import javax.inject.Inject;
import javax.validation.ConstraintViolation;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@MicronautTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ActionItemCreateDTOTest {

    @Inject
    private Validator validator;

    @Test
    void testDTOInstantiation() {
        ActionItemCreateDTO dto = new ActionItemCreateDTO();
        assertNull(dto.getCheckinid());
        assertNull(dto.getCreatedbyid());
        assertNull(dto.getDescription());
    }

    @Test
    void testConstraintViolation() {
        ActionItemCreateDTO dto = new ActionItemCreateDTO();

        Set<ConstraintViolation<ActionItemCreateDTO>> violations = validator.validate(dto);
        assertEquals(violations.size(), 2);
        for (ConstraintViolation<ActionItemCreateDTO> violation : violations) {
            assertEquals(violation.getMessage(), "must not be null");
        }
    }

    @Test
    void testPopulatedDTO() {
        ActionItemCreateDTO dto = new ActionItemCreateDTO();

        UUID checkinId = UUID.randomUUID();
        UUID memberId = UUID.randomUUID();

        dto.setCheckinid(checkinId);
        assertEquals(dto.getCheckinid(), checkinId);

        dto.setCreatedbyid(memberId);
        assertEquals(dto.getCreatedbyid(), memberId);

        dto.setDescription("DNC");
        assertEquals("DNC", dto.getDescription());

        Set<ConstraintViolation<ActionItemCreateDTO>> violations = validator.validate(dto);
        assertTrue(violations.isEmpty());
    }
}
