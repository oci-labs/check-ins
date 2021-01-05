package com.objectcomputing.checkins.services.team.member;


import com.fasterxml.jackson.databind.JsonNode;
import com.objectcomputing.checkins.services.TestContainersSuite;
import com.objectcomputing.checkins.services.fixture.MemberProfileFixture;
import com.objectcomputing.checkins.services.fixture.TeamFixture;
import com.objectcomputing.checkins.services.fixture.TeamMemberFixture;
import com.objectcomputing.checkins.services.memberprofile.MemberProfile;
import com.objectcomputing.checkins.services.team.Team;
import io.micronaut.core.type.Argument;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.MutableHttpRequest;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;
import java.util.*;
import java.util.stream.Collectors;

import static com.objectcomputing.checkins.services.role.RoleType.Constants.ADMIN_ROLE;
import static com.objectcomputing.checkins.services.role.RoleType.Constants.MEMBER_ROLE;
import static org.junit.jupiter.api.Assertions.*;

class TeamMemberControllerTest extends TestContainersSuite implements TeamFixture, MemberProfileFixture, TeamMemberFixture {

    @Inject
    @Client("/services/team/member")
    HttpClient client;

    @Test
    void testCreateATeamMemberByAdmin() {
        Team team = createDeafultTeam();
        MemberProfile memberProfile = createADefaultMemberProfile();

        TeamMemberCreateDTO teamMemberCreateDTO = new TeamMemberCreateDTO(team.getId(), memberProfile.getId(), false);
        final HttpRequest<TeamMemberCreateDTO> request = HttpRequest.POST("", teamMemberCreateDTO).basicAuth("test@test.com", ADMIN_ROLE);
        final HttpResponse<TeamMember> response = client.toBlocking().exchange(request, TeamMember.class);

        TeamMember teamMember = response.body();

        assertEquals(teamMemberCreateDTO.getMemberid(), teamMember.getMemberid());
        assertEquals(HttpStatus.CREATED, response.getStatus());
        assertEquals(String.format("%s/%s", request.getPath(), teamMember.getId()), response.getHeaders().get("location"));
    }

    @Test
    void testCreateATeamMemberByTeamLead() {
        Team team = createDeafultTeam();

        // Create a team lead and add him to the team
        MemberProfile memberProfileOfTeamLead = createADefaultMemberProfile();
        createLeadTeamMember(team, memberProfileOfTeamLead);

        // Create a member and add him to team
        MemberProfile memberProfileOfUser = createAnUnrelatedUser();
        TeamMemberCreateDTO teamMemberCreateDTO = new TeamMemberCreateDTO(team.getId(), memberProfileOfUser.getId(), false);
        final HttpRequest<TeamMemberCreateDTO> request = HttpRequest.POST("", teamMemberCreateDTO).basicAuth(memberProfileOfTeamLead.getWorkEmail(), MEMBER_ROLE);
        final HttpResponse<TeamMember> response = client.toBlocking().exchange(request, TeamMember.class);

        TeamMember teamMember = response.body();

        assertEquals(teamMemberCreateDTO.getMemberid(), teamMember.getMemberid());
        assertEquals(HttpStatus.CREATED, response.getStatus());
        assertEquals(String.format("%s/%s", request.getPath(), teamMember.getId()), response.getHeaders().get("location"));
    }

    @Test
    void testCreateATeamMemberThrowsExceptionForNotAdminAndNotTeamLead() {
        Team team = createDeafultTeam();

        // Create a user (not team lead) and add him to the team
        MemberProfile memberProfileOfTeammate = createADefaultMemberProfile();
        createDeafultTeamMember(team, memberProfileOfTeammate);

        // Create a member and add him to team
        MemberProfile memberProfileOfUser = createAnUnrelatedUser();
        TeamMemberCreateDTO teamMemberCreateDTO = new TeamMemberCreateDTO(team.getId(), memberProfileOfUser.getId(), false);
        final HttpRequest<TeamMemberCreateDTO> request = HttpRequest.POST("", teamMemberCreateDTO).basicAuth(memberProfileOfTeammate.getWorkEmail(), MEMBER_ROLE);
        HttpClientResponseException responseException = assertThrows(HttpClientResponseException.class,
                                                        () -> client.toBlocking().exchange(request, Map.class));

        JsonNode body = responseException.getResponse().getBody(JsonNode.class).orElse(null);
        String error = Objects.requireNonNull(body).get("message").asText();
        String href = Objects.requireNonNull(body).get("_links").get("self").get("href").asText();

        assertEquals(request.getPath(), href);
        assertEquals("You are not authorized to perform this operation", error);
        assertEquals(HttpStatus.BAD_REQUEST, responseException.getStatus());
    }

    @Test
    void testCreateAnInvalidTeamMember() {
        TeamMemberCreateDTO dto = new TeamMemberCreateDTO(null, null, null);

        final HttpRequest<TeamMemberCreateDTO> request = HttpRequest.POST("", dto).basicAuth(MEMBER_ROLE, MEMBER_ROLE);
        HttpClientResponseException responseException = assertThrows(HttpClientResponseException.class,
                () -> client.toBlocking().exchange(request, Map.class));

        JsonNode body = responseException.getResponse().getBody(JsonNode.class).orElse(null);
        JsonNode errors = Objects.requireNonNull(body).get("_embedded").get("errors");
        JsonNode href = Objects.requireNonNull(body).get("_links").get("self").get("href");
        List<String> errorList = List.of(errors.get(0).get("message").asText(), errors.get(1).get("message").asText())
                .stream().sorted().collect(Collectors.toList());
        assertEquals("teamMember.memberid: must not be null", errorList.get(0));
        assertEquals("teamMember.teamid: must not be null", errorList.get(1));
        assertEquals(request.getPath(), href.asText());
        assertEquals(HttpStatus.BAD_REQUEST, responseException.getStatus());

    }

    @Test
    void testCreateANullTeamMember() {

        final HttpRequest<String> request = HttpRequest.POST("", "").basicAuth(MEMBER_ROLE, MEMBER_ROLE);
        HttpClientResponseException responseException = assertThrows(HttpClientResponseException.class,
                () -> client.toBlocking().exchange(request, Map.class));

        JsonNode body = responseException.getResponse().getBody(JsonNode.class).orElse(null);
        JsonNode errors = Objects.requireNonNull(body).get("message");
        JsonNode href = Objects.requireNonNull(body).get("_links").get("self").get("href");
        assertEquals("Required Body [teamMember] not specified", errors.asText());
        assertEquals(request.getPath(), href.asText());
        assertEquals(HttpStatus.BAD_REQUEST, responseException.getStatus());
    }

    @Test
    void testCreateATeamMemberWithNonExistingTeam() {

        Team team = createDeafultTeam();
        MemberProfile memberProfile = createADefaultMemberProfile();

        TeamMemberCreateDTO teamMemberResponseDTO = new TeamMemberCreateDTO(UUID.randomUUID(), memberProfile.getId(), false);

        final HttpRequest<TeamMemberCreateDTO> request = HttpRequest.POST("", teamMemberResponseDTO).basicAuth(MEMBER_ROLE, MEMBER_ROLE);
        HttpClientResponseException responseException = assertThrows(HttpClientResponseException.class,
                () -> client.toBlocking().exchange(request, Map.class));

        JsonNode body = responseException.getResponse().getBody(JsonNode.class).orElse(null);
        String error = Objects.requireNonNull(body).get("message").asText();
        String href = Objects.requireNonNull(body).get("_links").get("self").get("href").asText();

        assertEquals(request.getPath(), href);
        assertEquals(String.format("Team %s doesn't exist", teamMemberResponseDTO.getTeamid()),error);
    }

    @Test
    void testCreateATeamMemberWithNonExistingMember() {

        Team team = createDeafultTeam();
        MemberProfile memberProfile = createADefaultMemberProfile();

        TeamMemberCreateDTO requestDTO = new TeamMemberCreateDTO(team.getId(), UUID.randomUUID(), false);

        final HttpRequest<TeamMemberCreateDTO> request = HttpRequest.POST("", requestDTO).basicAuth(MEMBER_ROLE, MEMBER_ROLE);
        HttpClientResponseException responseException = assertThrows(HttpClientResponseException.class,
                () -> client.toBlocking().exchange(request, Map.class));

        JsonNode body = responseException.getResponse().getBody(JsonNode.class).orElse(null);
        String error = Objects.requireNonNull(body).get("message").asText();
        String href = Objects.requireNonNull(body).get("_links").get("self").get("href").asText();

        assertEquals(request.getPath(), href);
        assertEquals(String.format("Member %s doesn't exist", requestDTO.getMemberid()),error);
    }

    @Test
    void testCreateATeamMemberWithExistingMemberAndTeam() {

        Team team = createDeafultTeam();
        MemberProfile memberProfile = createADefaultMemberProfile();

        TeamMember teamMember = createDeafultTeamMember(team,memberProfile);

        TeamMemberCreateDTO teamMemberResponseDTO = new TeamMemberCreateDTO(teamMember.getTeamid(), memberProfile.getId(), false);

        final HttpRequest<TeamMemberCreateDTO> request = HttpRequest.POST("", teamMemberResponseDTO).basicAuth(MEMBER_ROLE, MEMBER_ROLE);
        HttpClientResponseException responseException = assertThrows(HttpClientResponseException.class,
                () -> client.toBlocking().exchange(request, Map.class));

        JsonNode body = responseException.getResponse().getBody(JsonNode.class).orElse(null);
        String error = Objects.requireNonNull(body).get("message").asText();
        String href = Objects.requireNonNull(body).get("_links").get("self").get("href").asText();

        assertEquals(request.getPath(), href);
        assertEquals(String.format("Member %s already exists in team %s", teamMemberResponseDTO.getMemberid(), teamMemberResponseDTO.getTeamid()),error);
    }

    @Test
    void testReadTeamMember() {
        Team team = createDeafultTeam();
        MemberProfile memberProfile = createADefaultMemberProfile();

        TeamMember teamMember = createDeafultTeamMember(team,memberProfile);
        final HttpRequest<?> request = HttpRequest.GET(String.format("/%s", teamMember.getId().toString())).basicAuth(MEMBER_ROLE, MEMBER_ROLE);
        final HttpResponse<TeamMember> response = client.toBlocking().exchange(request, TeamMember.class);

        assertEquals(teamMember, response.body());
        assertEquals(HttpStatus.OK, response.getStatus());
    }

    @Test
    void testReadTeamMemberNotFound() {

        final HttpRequest<?> request = HttpRequest.GET(String.format("/%s", UUID.randomUUID())).basicAuth(MEMBER_ROLE, MEMBER_ROLE);
        HttpClientResponseException responseException = assertThrows(HttpClientResponseException.class, () -> client.toBlocking().exchange(request, TeamMember.class));

        assertEquals(HttpStatus.NOT_FOUND, responseException.getStatus());
    }

    @Test
    void testFindAllTeamMembers() {
        Team team = createDeafultTeam();
        MemberProfile memberProfile = createADefaultMemberProfile();

        TeamMember teamMember = createDeafultTeamMember(team,memberProfile);

        final HttpRequest<?> request = HttpRequest.GET("/").basicAuth(MEMBER_ROLE, MEMBER_ROLE);
        final HttpResponse<Set<TeamMember>> response = client.toBlocking().exchange(request, Argument.setOf(TeamMember.class));

        assertEquals(Set.of(teamMember), response.body());
        assertEquals(HttpStatus.OK, response.getStatus());

    }

    @Test
    void testFindByTeamId() {
        Team team = createDeafultTeam();
        MemberProfile memberProfile = createADefaultMemberProfile();

        TeamMember teamMember = createDeafultTeamMember(team,memberProfile);

        final HttpRequest<?> request = HttpRequest.GET(String.format("/?teamid=%s", teamMember.getTeamid())).basicAuth(MEMBER_ROLE, MEMBER_ROLE);
        final HttpResponse<Set<TeamMember>> response = client.toBlocking().exchange(request, Argument.setOf(TeamMember.class));

        assertEquals(Set.of(teamMember), response.body());
        assertEquals(HttpStatus.OK, response.getStatus());

    }

    @Test
    void testFindByMemberId() {
        Team team = createDeafultTeam();
        MemberProfile memberProfile = createADefaultMemberProfile();

        TeamMember teamMember = createDeafultTeamMember(team,memberProfile);

        final HttpRequest<?> request = HttpRequest.GET(String.format("/?memberid=%s", teamMember.getMemberid())).basicAuth(MEMBER_ROLE, MEMBER_ROLE);
        final HttpResponse<Set<TeamMember>> response = client.toBlocking().exchange(request, Argument.setOf(TeamMember.class));

        assertEquals(Set.of(teamMember), response.body());
        assertEquals(HttpStatus.OK, response.getStatus());
    }

    @Test
    void testFindTeamMembers() {
        Team team = createDeafultTeam();
        MemberProfile memberProfile = createADefaultMemberProfile();

        TeamMember teamMember = createDeafultTeamMember(team,memberProfile);

        final HttpRequest<?> request = HttpRequest.GET(String.format("/?teamid=%s&memberid=%s", teamMember.getTeamid(),
                teamMember.getMemberid())).basicAuth(MEMBER_ROLE, MEMBER_ROLE);
        final HttpResponse<Set<TeamMember>> response = client.toBlocking().exchange(request, Argument.setOf(TeamMember.class));

        assertEquals(Set.of(teamMember), response.body());
        assertEquals(HttpStatus.OK, response.getStatus());
    }

    @Test
    void testFindTeamMembersAllParams() {
        Team team = createDeafultTeam();
        MemberProfile memberProfile = createADefaultMemberProfile();

        TeamMember teamMember = createDeafultTeamMember(team,memberProfile);

        final HttpRequest<?> request = HttpRequest.GET(String.format("/?teamid=%s&memberid=%s&lead=%s", teamMember.getTeamid(),
                teamMember.getMemberid(), teamMember.isLead())).basicAuth(MEMBER_ROLE, MEMBER_ROLE);
        final HttpResponse<Set<TeamMember>> response = client.toBlocking().exchange(request, Argument.setOf(TeamMember.class));

        assertEquals(Set.of(teamMember), response.body());
        assertEquals(HttpStatus.OK, response.getStatus());

    }

    @Test
    void testUpdateTeamMemberByAdmin() {
        Team team = createDeafultTeam();
        MemberProfile memberProfile = createADefaultMemberProfile();
        TeamMember teamMember = createDeafultTeamMember(team, memberProfile);

        TeamMemberUpdateDTO teamMemberUpdateDTO = new TeamMemberUpdateDTO(teamMember.getId(), teamMember.getTeamid(), teamMember.getMemberid(), true);
        final MutableHttpRequest<TeamMemberUpdateDTO> request = HttpRequest.PUT("", teamMemberUpdateDTO).basicAuth("test@test.com", ADMIN_ROLE);
        final HttpResponse<TeamMember> response = client.toBlocking().exchange(request, TeamMember.class);

        TeamMember result = response.body();
        assertNotNull(result);
        assertEquals(teamMember.getMemberid(), result.getMemberid());
        assertTrue(result.isLead());
        assertEquals(HttpStatus.OK, response.getStatus());
        assertEquals(String.format("%s/%s", request.getPath(), teamMember.getId()), response.getHeaders().get("location"));
    }

    @Test
    void testUpdateTeamMemberByTeamLead() {
        Team team = createDeafultTeam();

        // Create a team lead and add him to the team
        MemberProfile memberProfileOfTeamLead = createADefaultMemberProfile();
        createLeadTeamMember(team, memberProfileOfTeamLead);

        // Create a member and add him to team
        MemberProfile memberProfileOfUser = createAnUnrelatedUser();
        TeamMember teamMember = createDeafultTeamMember(team, memberProfileOfUser);

        // Update member
        TeamMemberUpdateDTO teamMemberUpdateDTO = new TeamMemberUpdateDTO(teamMember.getId(), teamMember.getTeamid(), teamMember.getMemberid(), true);
        final MutableHttpRequest<TeamMemberUpdateDTO> request = HttpRequest.PUT("", teamMemberUpdateDTO).basicAuth(memberProfileOfTeamLead.getWorkEmail(), MEMBER_ROLE);
        final HttpResponse<TeamMember> response = client.toBlocking().exchange(request, TeamMember.class);

        TeamMember result = response.body();
        assertNotNull(result);
        assertEquals(teamMember.getMemberid(), result.getMemberid());
        assertTrue(result.isLead());
        assertEquals(HttpStatus.OK, response.getStatus());
        assertEquals(String.format("%s/%s", request.getPath(), teamMember.getId()), response.getHeaders().get("location"));
    }

    @Test
    void testUpdateTeamMemberThrowsExceptionForNotAdminAndNotTeamLead() {
        Team team = createDeafultTeam();
        MemberProfile memberProfile = createADefaultMemberProfile();

        TeamMember teamMember = createDeafultTeamMember(team,memberProfile);

        final HttpRequest<TeamMember> request = HttpRequest.PUT("", teamMember).basicAuth(MEMBER_ROLE, MEMBER_ROLE);
        HttpClientResponseException responseException = assertThrows(HttpClientResponseException.class,
                                                        () -> client.toBlocking().exchange(request, Map.class));

        JsonNode body = responseException.getResponse().getBody(JsonNode.class).orElse(null);
        String error = Objects.requireNonNull(body).get("message").asText();
        String href = Objects.requireNonNull(body).get("_links").get("self").get("href").asText();

        assertEquals(request.getPath(), href);
        assertEquals("You are not authorized to perform this operation", error);
        assertEquals(HttpStatus.BAD_REQUEST, responseException.getStatus());
    }

    @Test
    void testUpdateAnInvalidTeamMember() {
        Team team = createDeafultTeam();
        MemberProfile memberProfile = createADefaultMemberProfile();

        TeamMember teamMember = createDeafultTeamMember(team,memberProfile);
        teamMember.setMemberid(null);
        teamMember.setTeamid(null);

        final HttpRequest<TeamMember> request = HttpRequest.PUT("", teamMember).basicAuth(MEMBER_ROLE, MEMBER_ROLE);
        HttpClientResponseException responseException = assertThrows(HttpClientResponseException.class,
                () -> client.toBlocking().exchange(request, Map.class));

        JsonNode body = responseException.getResponse().getBody(JsonNode.class).orElse(null);
        JsonNode errors = Objects.requireNonNull(body).get("_embedded").get("errors");
        JsonNode href = Objects.requireNonNull(body).get("_links").get("self").get("href");
        List<String> errorList = List.of(errors.get(0).get("message").asText(), errors.get(1).get("message").asText())
                .stream().sorted().collect(Collectors.toList());
        assertEquals("teamMember.memberid: must not be null", errorList.get(0));
        assertEquals("teamMember.teamid: must not be null", errorList.get(1));
        assertEquals(request.getPath(), href.asText());
        assertEquals(HttpStatus.BAD_REQUEST, responseException.getStatus());
    }

    @Test
    void testUpdateANullTeamMember() {
        final HttpRequest<String> request = HttpRequest.PUT("", "").basicAuth(MEMBER_ROLE, MEMBER_ROLE);
        HttpClientResponseException responseException = assertThrows(HttpClientResponseException.class,
                () -> client.toBlocking().exchange(request, Map.class));

        JsonNode body = responseException.getResponse().getBody(JsonNode.class).orElse(null);
        JsonNode errors = Objects.requireNonNull(body).get("message");
        JsonNode href = Objects.requireNonNull(body).get("_links").get("self").get("href");
        assertEquals("Required Body [teamMember] not specified", errors.asText());
        assertEquals(request.getPath(), href.asText());
        assertEquals(HttpStatus.BAD_REQUEST, responseException.getStatus());
    }


    @Test
    void testUpdateTeamMemberThrowException() {
        Team team = createDeafultTeam();
        MemberProfile memberProfile = createADefaultMemberProfile();

        TeamMember teamMember = createDeafultTeamMember(team,memberProfile);
        teamMember.setMemberid(UUID.randomUUID());
        teamMember.setTeamid(teamMember.getTeamid());

        final MutableHttpRequest<TeamMember> request = HttpRequest.PUT("", teamMember).basicAuth(MEMBER_ROLE, MEMBER_ROLE);
        HttpClientResponseException responseException = assertThrows(HttpClientResponseException.class, () ->
                client.toBlocking().exchange(request, Map.class));

        JsonNode body = responseException.getResponse().getBody(JsonNode.class).orElse(null);
        String error = Objects.requireNonNull(body).get("message").asText();
        String href = Objects.requireNonNull(body).get("_links").get("self").get("href").asText();

        assertEquals(String.format("Member %s doesn't exist",teamMember.getMemberid()),error);
        assertEquals(request.getPath(), href);
        assertEquals(HttpStatus.BAD_REQUEST, responseException.getStatus());

    }

    @Test
    void testUpdateTeamMemberThrowExceptionWithNoTeam() {
        Team team = createDeafultTeam();
        MemberProfile memberProfile = createADefaultMemberProfile();

        TeamMember teamMember = createDeafultTeamMember(team,memberProfile);
        teamMember.setMemberid(teamMember.getMemberid());
        teamMember.setTeamid(UUID.randomUUID());

        final MutableHttpRequest<TeamMember> request = HttpRequest.PUT("", teamMember).basicAuth(MEMBER_ROLE, MEMBER_ROLE);
        HttpClientResponseException responseException = assertThrows(HttpClientResponseException.class, () ->
                client.toBlocking().exchange(request, Map.class));

        JsonNode body = responseException.getResponse().getBody(JsonNode.class).orElse(null);
        String error = Objects.requireNonNull(body).get("message").asText();
        String href = Objects.requireNonNull(body).get("_links").get("self").get("href").asText();

        assertEquals(String.format("Team %s doesn't exist",teamMember.getTeamid()),error);
        assertEquals(request.getPath(), href);
        assertEquals(HttpStatus.BAD_REQUEST, responseException.getStatus());

    }

    @Test
    void testUpdateTeamMemberThrowExceptionWithInvalidId() {
        Team team = createDeafultTeam();
        MemberProfile memberProfile = createADefaultMemberProfile();

        TeamMember teamMember = createDeafultTeamMember(team,memberProfile);
        teamMember.setId(UUID.randomUUID());
        teamMember.setMemberid(teamMember.getMemberid());
        teamMember.setTeamid(teamMember.getTeamid());

        final MutableHttpRequest<TeamMember> request = HttpRequest.PUT("", teamMember).basicAuth(MEMBER_ROLE, MEMBER_ROLE);
        HttpClientResponseException responseException = assertThrows(HttpClientResponseException.class, () ->
                client.toBlocking().exchange(request, Map.class));

        JsonNode body = responseException.getResponse().getBody(JsonNode.class).orElse(null);
        String error = Objects.requireNonNull(body).get("message").asText();
        String href = Objects.requireNonNull(body).get("_links").get("self").get("href").asText();

        assertEquals(String.format("Unable to locate teamMember to update with id %s",teamMember.getId()),error);
        assertEquals(request.getPath(), href);
        assertEquals(HttpStatus.BAD_REQUEST, responseException.getStatus());
    }
}