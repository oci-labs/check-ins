package com.objectcomputing.checkins.services.pulseresponse;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class PulseResponseBadArgExceptionTest {

    @Test
    void testExceptionMessage() {
        final String message = "Hello world";
        PulseResponseBadArgException argException = new PulseResponseBadArgException(message);
        assertEquals(argException.getMessage(), message);
    }
}
