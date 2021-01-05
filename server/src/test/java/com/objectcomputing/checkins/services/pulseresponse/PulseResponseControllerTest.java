package com.objectcomputing.checkins.services.pulseresponse;

import com.fasterxml.jackson.databind.JsonNode;
import com.objectcomputing.checkins.services.TestContainersSuite;
import com.objectcomputing.checkins.services.fixture.PulseResponseFixture;
import com.objectcomputing.checkins.services.fixture.MemberProfileFixture;

import com.objectcomputing.checkins.services.memberprofile.MemberProfile;
import io.micronaut.core.type.Argument;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

import static com.objectcomputing.checkins.services.role.RoleType.Constants.MEMBER_ROLE;
import static org.junit.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

public class PulseResponseControllerTest extends TestContainersSuite implements MemberProfileFixture, PulseResponseFixture {

    @Inject
    @Client("/services/pulse-response")
    private HttpClient client;

    @Test
    public void testCreateAPulseResponse(){
        MemberProfile memberProfile = createADefaultMemberProfile();

        PulseResponseCreateDTO pulseResponseCreateDTO = new PulseResponseCreateDTO();
        pulseResponseCreateDTO.setSubmissionDate(LocalDate.now());
        pulseResponseCreateDTO.setUpdatedDate(LocalDate.now());
        pulseResponseCreateDTO.setTeamMemberId(memberProfile.getId());
        pulseResponseCreateDTO.setInternalFeelings("internalfeelings");
        pulseResponseCreateDTO.setExternalFeelings("externalfeelings");

        final HttpRequest<PulseResponseCreateDTO> request = HttpRequest.POST("",pulseResponseCreateDTO).basicAuth(MEMBER_ROLE,MEMBER_ROLE);
        final HttpResponse<PulseResponse> response = client.toBlocking().exchange(request,PulseResponse.class);

        PulseResponse pulseResponseResponse = response.body();

        assertNotNull(pulseResponseResponse);
        assertEquals(HttpStatus.CREATED, response.getStatus());
        assertEquals(pulseResponseCreateDTO.getTeamMemberId(),pulseResponseResponse.getTeamMemberId());
        assertEquals(String.format("%s/%s", request.getPath(), pulseResponseResponse.getId()), response.getHeaders().get("location"));
    }

    @Test
    void testCreateAnInvalidPulseResponse() {
        PulseResponseCreateDTO pulseResponseCreateDTO = new PulseResponseCreateDTO();

        final HttpRequest<PulseResponseCreateDTO> request = HttpRequest.POST("",pulseResponseCreateDTO).basicAuth(MEMBER_ROLE,MEMBER_ROLE);
        HttpClientResponseException responseException = assertThrows(HttpClientResponseException.class,
                () -> client.toBlocking().exchange(request, Map.class));

        JsonNode body = responseException.getResponse().getBody(JsonNode.class).orElse(null);
        JsonNode errors = Objects.requireNonNull(body).get("_embedded").get("errors");
        JsonNode href = Objects.requireNonNull(body).get("_links").get("self").get("href");
        List<String> errorList = List.of(errors.get(0).get("message").asText(), errors.get(1).get("message").asText(),
        errors.get(2).get("message").asText(), errors.get(3).get("message").asText(), errors.get(4).get("message").asText())
                .stream().sorted().collect(Collectors.toList());
        assertEquals(5,errorList.size());
        assertEquals(request.getPath(),href.asText());
        assertEquals(HttpStatus.BAD_REQUEST, responseException.getStatus());
    }

    @Test
    void testCreatePulseResponseForNonExistingMember(){
        PulseResponseCreateDTO pulseResponseCreateDTO = new PulseResponseCreateDTO();
        pulseResponseCreateDTO.setSubmissionDate(LocalDate.now());
        pulseResponseCreateDTO.setUpdatedDate(LocalDate.now());
        pulseResponseCreateDTO.setTeamMemberId(UUID.randomUUID());
        pulseResponseCreateDTO.setInternalFeelings("internalfeelings");
        pulseResponseCreateDTO.setExternalFeelings("externalfeelings");

        HttpRequest<PulseResponseCreateDTO> request = HttpRequest.POST("",pulseResponseCreateDTO).basicAuth(MEMBER_ROLE,MEMBER_ROLE);
        HttpClientResponseException responseException = assertThrows(HttpClientResponseException.class,
                () -> client.toBlocking().exchange(request, Map.class));

        JsonNode body = responseException.getResponse().getBody(JsonNode.class).orElse(null);
        String error = Objects.requireNonNull(body).get("message").asText();
        String href = Objects.requireNonNull(body).get("_links").get("self").get("href").asText();

        assertEquals(request.getPath(),href);
        assertEquals(String.format("Member %s doesn't exists",pulseResponseCreateDTO.getTeamMemberId()),error);
    }

    @Test
    void testCreateANullPulseResponse() {
        final HttpRequest<String> request = HttpRequest.POST("","").basicAuth(MEMBER_ROLE,MEMBER_ROLE);
        HttpClientResponseException responseException = assertThrows(HttpClientResponseException.class,
                () -> client.toBlocking().exchange(request, Map.class));
        JsonNode body = responseException.getResponse().getBody(JsonNode.class).orElse(null);
        JsonNode errors = Objects.requireNonNull(body).get("message");
        JsonNode href = Objects.requireNonNull(body).get("_links").get("self").get("href");

        assertEquals("Required Body [pulseResponse] not specified",errors.asText());
        assertEquals(request.getPath(),href.asText());
        assertEquals(HttpStatus.BAD_REQUEST,responseException.getStatus());

    }

    @Test
    void testCreateAPulseResponseForInvalidDate() {
        MemberProfile memberProfile = createADefaultMemberProfile();
        PulseResponseCreateDTO pulseResponseCreateDTO = new PulseResponseCreateDTO();
        pulseResponseCreateDTO.setSubmissionDate(LocalDate.of(1965,11,12));
        pulseResponseCreateDTO.setUpdatedDate(LocalDate.of(1965,11,12));
        pulseResponseCreateDTO.setTeamMemberId(memberProfile.getId());
        pulseResponseCreateDTO.setInternalFeelings("internalfeelings");
        pulseResponseCreateDTO.setExternalFeelings("externalfeelings");

        final HttpRequest<PulseResponseCreateDTO> request = HttpRequest.POST("",pulseResponseCreateDTO).basicAuth(MEMBER_ROLE,MEMBER_ROLE);
        HttpClientResponseException responseException = assertThrows(HttpClientResponseException.class,
                () -> client.toBlocking().exchange(request, Map.class));
        JsonNode body = responseException.getResponse().getBody(JsonNode.class).orElse(null);
        String error = Objects.requireNonNull(body).get("message").asText();
        String href = Objects.requireNonNull(body).get("_links").get("self").get("href").asText();

        assertEquals(request.getPath(),href);
        assertEquals(String.format("Invalid date for pulseresponse submission date %s",pulseResponseCreateDTO.getTeamMemberId()),error);

    }

    @Test
    public void testGetFindByTeamMemberId() {

        MemberProfile memberProfile = createADefaultMemberProfile();


        PulseResponse pulseResponse  = createADefaultPulseResponse(memberProfile);

        final HttpRequest<?> request = HttpRequest.GET(String.format("/?teamMemberId=%s", pulseResponse.getTeamMemberId())).basicAuth(MEMBER_ROLE,MEMBER_ROLE);
        final HttpResponse<Set<PulseResponse>> response = client.toBlocking().exchange(request, Argument.setOf(PulseResponse.class));
        assertEquals(Set.of(pulseResponse), response.body());
        assertEquals(HttpStatus.OK,response.getStatus());

    }

// Find By findBySubmissionDateBetween returns empty array - when no data exists
@Test
public void testGetFindBySubmissionDateBetweenReturnsEmptyBody() {

    LocalDate testDateFrom = LocalDate.of(2019, 01, 01);  
    LocalDate testDateTo = LocalDate.of(2019, 02, 01);

    MemberProfile memberProfile = createADefaultMemberProfile();

    PulseResponse pulseResponse  = createADefaultPulseResponse(memberProfile);

    final HttpRequest<?> request = HttpRequest.GET(String.format("/?dateFrom=%tF&dateTo=%tF", testDateFrom, testDateTo)).basicAuth(MEMBER_ROLE,MEMBER_ROLE);
    final HttpResponse<Set<PulseResponse>> response = client.toBlocking().exchange(request, Argument.setOf(PulseResponse.class));
    assertEquals(HttpStatus.OK, response.getStatus());
    assertEquals(2, response.getContentLength());
}

// Find By findBySubmissionDateBetwe en
@Test
public void testGetFindByfindBySubmissionDateBetween() {

    MemberProfile memberProfile = createADefaultMemberProfile();

    PulseResponse pulseResponse  = createADefaultPulseResponse(memberProfile);

    LocalDate testDateFrom = LocalDate.of(2019, 01, 01);  
    LocalDate testDateTo = LocalDate.of(2021, 01, 01);

    final HttpRequest<?> request = HttpRequest.GET(String.format("/?dateFrom=%tF&dateTo=%tF", testDateFrom, testDateTo)).basicAuth(MEMBER_ROLE,MEMBER_ROLE);
    final HttpResponse<Set<PulseResponse>> response = client.toBlocking().exchange(request, Argument.setOf(PulseResponse.class));
    
    assertEquals(HttpStatus.OK, response.getStatus());
    assertNotEquals(2, response.getContentLength());
}

    @Test
    public void testGetFindById() {

        MemberProfile memberProfile = createADefaultMemberProfile();

        PulseResponse pulseResponse  = createADefaultPulseResponse(memberProfile);

        final HttpRequest<?> request = HttpRequest.GET(String.format("/%s", pulseResponse.getId())).basicAuth(MEMBER_ROLE,MEMBER_ROLE);
        final HttpResponse<Set<PulseResponse>> response = client.toBlocking().exchange(request, Argument.setOf(PulseResponse.class));
        assertEquals(Set.of(pulseResponse), response.body());
        assertEquals(HttpStatus.OK,response.getStatus());

    }

    @Test
    void testFindPulseResponseAllParams(){
        MemberProfile memberProfile = createADefaultMemberProfile();

        PulseResponse pulseResponse  = createADefaultPulseResponse(memberProfile);

        final HttpRequest<?> request = HttpRequest.GET(String.format("/?teamMemberId=%s", pulseResponse.getTeamMemberId())).basicAuth(MEMBER_ROLE,MEMBER_ROLE);
        final HttpResponse<Set<PulseResponse>> response = client.toBlocking().exchange(request, Argument.setOf(PulseResponse.class));

        assertEquals(Set.of(pulseResponse), response.body());
        assertEquals(HttpStatus.OK,response.getStatus());

    }

    @Test
    void testPulseResponseDoesNotExist() {

        final HttpRequest<?> request = HttpRequest.GET(String.format("/?teamMemberId=%s",UUID.randomUUID())).basicAuth(MEMBER_ROLE,MEMBER_ROLE);
        HttpResponse<Set<PulseResponse>> response = client.toBlocking().exchange(request, Argument.setOf(PulseResponse.class));

        assertEquals(Set.of(), response.body());
        assertEquals(HttpStatus.OK,response.getStatus());

    }

    @Test
    void testUpdatePulseResponse(){
        MemberProfile memberProfile = createADefaultMemberProfile();

        PulseResponse pulseResponse  = createADefaultPulseResponse(memberProfile);

        final HttpRequest<PulseResponse> request = HttpRequest.PUT("", pulseResponse)
                .basicAuth(MEMBER_ROLE, MEMBER_ROLE);
        final HttpResponse<PulseResponse> response = client.toBlocking().exchange(request, PulseResponse.class);

        assertEquals(pulseResponse, response.body());
        assertEquals(HttpStatus.OK, response.getStatus());
        assertEquals(String.format("%s/%s", request.getPath(), pulseResponse.getId()), response.getHeaders().get("location"));
    }

    @Test
    void testUpdateNonExistingPulseResponse(){
        MemberProfile memberProfile = createADefaultMemberProfile();

        PulseResponse pulseResponse  = createADefaultPulseResponse(memberProfile);
        pulseResponse.setId(UUID.randomUUID());

        final HttpRequest<PulseResponse> request = HttpRequest.PUT("", pulseResponse)
                .basicAuth(MEMBER_ROLE, MEMBER_ROLE);
        final HttpClientResponseException responseException = assertThrows(HttpClientResponseException.class, () ->
                client.toBlocking().exchange(request, Map.class));

        JsonNode body = responseException.getResponse().getBody(JsonNode.class).orElse(null);
        String error = Objects.requireNonNull(body).get("message").asText();
        String href = Objects.requireNonNull(body).get("_links").get("self").get("href").asText();

        assertEquals(String.format("Unable to find pulseresponse record with id %s", pulseResponse.getId()), error);
        assertEquals(request.getPath(), href);

    }

    @Test
    void testUpdateNotExistingMemberPulseResponse(){
        MemberProfile memberProfile = createADefaultMemberProfile();

        PulseResponse pulseResponse  = createADefaultPulseResponse(memberProfile);
        pulseResponse.setTeamMemberId(UUID.randomUUID());

        final HttpRequest<PulseResponse> request = HttpRequest.PUT("", pulseResponse)
                .basicAuth(MEMBER_ROLE, MEMBER_ROLE);
        final HttpClientResponseException responseException = assertThrows(HttpClientResponseException.class, () ->
                client.toBlocking().exchange(request, Map.class));

        JsonNode body = responseException.getResponse().getBody(JsonNode.class).orElse(null);
        String error = Objects.requireNonNull(body).get("message").asText();
        String href = Objects.requireNonNull(body).get("_links").get("self").get("href").asText();

        assertEquals(String.format("Member %s doesn't exist", pulseResponse.getTeamMemberId()), error);
        assertEquals(request.getPath(), href);

    }

    @Test
    void testUpdateNotMemberPulseResponseWithoutId(){
        MemberProfile memberProfile = createADefaultMemberProfile();

        PulseResponse pulseResponse  = createADefaultPulseResponse(memberProfile);
        pulseResponse.setId(null);

        final HttpRequest<PulseResponse> request = HttpRequest.PUT("", pulseResponse)
                .basicAuth(MEMBER_ROLE, MEMBER_ROLE);
        final HttpClientResponseException responseException = assertThrows(HttpClientResponseException.class, () ->
                client.toBlocking().exchange(request, Map.class));

        JsonNode body = responseException.getResponse().getBody(JsonNode.class).orElse(null);
        String error = Objects.requireNonNull(body).get("message").asText();
        String href = Objects.requireNonNull(body).get("_links").get("self").get("href").asText();

        assertEquals(String.format("Unable to find pulseresponse record with id null", pulseResponse.getId()), error);
        assertEquals(request.getPath(), href);

    }

    @Test
    void testUpdateUnAuthorized() {
        PulseResponse pulseResponse = new PulseResponse(LocalDate.now(),LocalDate.now(),UUID.randomUUID(),"internalfeeling","externalfeeling");

        final HttpRequest<PulseResponse> request = HttpRequest.PUT("", pulseResponse);
        HttpClientResponseException responseException = assertThrows(HttpClientResponseException.class, () ->
                client.toBlocking().exchange(request, String.class));

        assertEquals(HttpStatus.UNAUTHORIZED, responseException.getStatus());
        assertEquals("Unauthorized", responseException.getMessage());

    }

    @Test
    void testUpdateANullPulseResponse() {
        final HttpRequest<String> request = HttpRequest.PUT("","").basicAuth(MEMBER_ROLE,MEMBER_ROLE);
        HttpClientResponseException responseException = assertThrows(HttpClientResponseException.class,
                () -> client.toBlocking().exchange(request, Map.class));
        JsonNode body = responseException.getResponse().getBody(JsonNode.class).orElse(null);
        JsonNode errors = Objects.requireNonNull(body).get("message");
        JsonNode href = Objects.requireNonNull(body).get("_links").get("self").get("href");

        assertEquals("Required Body [pulseResponse] not specified",errors.asText());
        assertEquals(request.getPath(),href.asText());
        assertEquals(HttpStatus.BAD_REQUEST,responseException.getStatus());

    }

    @Test
    void testUpdateInvalidDatePulseResponse(){
        MemberProfile memberProfile = createADefaultMemberProfile();

        PulseResponse pulseResponse  = createADefaultPulseResponse(memberProfile);
        pulseResponse.setSubmissionDate(LocalDate.of(1965,12,11));

        final HttpRequest<PulseResponse> request = HttpRequest.PUT("", pulseResponse)
                .basicAuth(MEMBER_ROLE, MEMBER_ROLE);
        final HttpClientResponseException responseException = assertThrows(HttpClientResponseException.class, () ->
                client.toBlocking().exchange(request, Map.class));

        JsonNode body = responseException.getResponse().getBody(JsonNode.class).orElse(null);
        String error = Objects.requireNonNull(body).get("message").asText();
        String href = Objects.requireNonNull(body).get("_links").get("self").get("href").asText();

        assertEquals(String.format("Invalid date for pulseresponse submission date %s", pulseResponse.getTeamMemberId()), error);
        assertEquals(request.getPath(), href);

    }
}