package com.objectcomputing.checkins.services.team;


import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Error;
import io.micronaut.http.annotation.*;
import io.micronaut.http.hateoas.JsonError;
import io.micronaut.http.hateoas.Link;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.security.annotation.Secured;
import io.micronaut.security.rules.SecurityRule;
import io.netty.channel.EventLoopGroup;
import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;
import io.swagger.v3.oas.annotations.tags.Tag;

import javax.annotation.Nullable;
import javax.inject.Named;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.net.URI;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;

@Controller("/services/team")
@Secured(SecurityRule.IS_AUTHENTICATED)
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "team")
public class TeamController {

    private final TeamServices teamService;
    private final EventLoopGroup eventLoopGroup;
    private final ExecutorService ioExecutorService;

    public TeamController(TeamServices teamService,
                          EventLoopGroup eventLoopGroup,
                          @Named(TaskExecutors.IO) ExecutorService ioExecutorService) {
        this.teamService = teamService;
        this.eventLoopGroup = eventLoopGroup;
        this.ioExecutorService = ioExecutorService;
    }

    @Error(exception = TeamNotFoundException.class)
    public HttpResponse<?> handleNotFound(HttpRequest<?> request, TeamNotFoundException e) {
        JsonError error = new JsonError(e.getMessage())
                .link(Link.SELF, Link.of(request.getUri()));

        return HttpResponse.<JsonError>notFound()
                .body(error);
    }

    /**
     * Create and save a new team
     *
     * @param team, {@link TeamCreateDTO}
     * @return {@link HttpResponse<TeamResponseDTO>}
     */
    @Post()
    public Single<HttpResponse<TeamResponseDTO>> createATeam(@Body @Valid TeamCreateDTO team, HttpRequest<TeamCreateDTO> request) {

        return Single.fromCallable(() -> teamService.save(team))
                .observeOn(Schedulers.from(eventLoopGroup))
                .map(createdTeam -> (HttpResponse<TeamResponseDTO>) HttpResponse
                        .created(createdTeam)
                        .headers(headers -> headers.location(URI.create(String.format("%s/%s", request.getPath(), createdTeam.getId())))))
                .subscribeOn(Schedulers.from(ioExecutorService));
    }

    /**
     * Get team based on id
     *
     * @param id of team
     * @return {@link TeamResponseDTO team matching id}
     */

    @Get("/{id}")
    public Single<HttpResponse<TeamResponseDTO>> readTeam(@NotNull UUID id) {
        return Single.fromCallable(() ->teamService.read(id))
                .observeOn(Schedulers.from(eventLoopGroup))
                .map(team -> (HttpResponse<TeamResponseDTO>)HttpResponse.ok(team))
                .subscribeOn(Schedulers.from(ioExecutorService));
    }

    /**
     * Find team(s) given a combination of the following parameters
     *
     * @param name,     name of the team
     * @param memberid, {@link UUID} of the member you wish to inquire in to which teams they are a part of
     * @return {@link List< TeamResponseDTO > list of teams}, return all teams when no parameters filled in else
     * return all teams that match all of the filled in params
     */

    @Get("/{?name,memberid}")
    public Single<HttpResponse<Set<TeamResponseDTO>>> findTeams(@Nullable String name, @Nullable UUID memberid) {
        return Single.fromCallable(() -> teamService.findByFields(name, memberid))
                .observeOn(Schedulers.from(eventLoopGroup))
                .map(teams -> (HttpResponse<Set<TeamResponseDTO>>)HttpResponse.ok(teams))
                .subscribeOn(Schedulers.from(ioExecutorService));
    }

    /**
     * Update team.
     *
     * @param team, {@link TeamUpdateDTO}
     * @return {@link HttpResponse< TeamResponseDTO >}
     */
    @Put()
    public Single<HttpResponse<TeamResponseDTO>> update(@Body @Valid TeamUpdateDTO team, HttpRequest<TeamUpdateDTO> request) {
        return Single.fromCallable(() -> teamService.update(team))
                .observeOn(Schedulers.from(eventLoopGroup))
                .map(updated -> (HttpResponse<TeamResponseDTO>)HttpResponse
                        .ok()
                        .headers(headers -> headers.location(URI.create(String.format("%s/%s", request.getUri(), team.getId()))))
                        .body(updated))
                .subscribeOn(Schedulers.from(ioExecutorService));

    }

    /**
     * Delete Team
     *
     * @param id, id of {@link TeamUpdateDTO} to delete
     * @return
     */
    @Delete("/{id}")
    public Single<HttpResponse> deleteTeam(@NotNull UUID id) {
        return Single.fromCallable(() -> teamService.delete(id))
                .observeOn(Schedulers.from(eventLoopGroup))
                .map(success -> (HttpResponse)HttpResponse.ok())
                .subscribeOn(Schedulers.from(ioExecutorService));
    }
}