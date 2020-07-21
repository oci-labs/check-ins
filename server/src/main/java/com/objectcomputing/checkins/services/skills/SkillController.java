package com.objectcomputing.checkins.services.skills;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.*;
import io.micronaut.security.annotation.Secured;
import io.micronaut.security.rules.SecurityRule;
import io.swagger.v3.oas.annotations.tags.Tag;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;

@Controller("/skill")
@Secured(SecurityRule.IS_ANONYMOUS)
@Produces(MediaType.APPLICATION_JSON)
@Tag(name="skill")
public class SkillController {

    @Inject
    private SkillServices skillServices;

    public void setSkillServices(SkillServices skillServices) {
        this.skillServices = skillServices;
    }

    /**
     * Create and save a new skill.
     *
     * @param skill
     * @return
     */

    @Post(value = "/")
    public HttpResponse<Skill> createASkill(@Body @Valid Skill skill) {
        Skill newSkill = skillServices.saveSkill(skill);

        if (newSkill == null) {
            return HttpResponse.status(HttpStatus.valueOf(409), "already exists");
        } else {
            return HttpResponse
                    .created(newSkill)
                    .headers(headers -> headers.location(location(newSkill.getSkillid())));
        }
    }

    /**
     * Load the current skills into checkinsdb.
     *
     * @param skillslist
     * @return
     */

    @Post("/loadskills")
    @Consumes(MediaType.APPLICATION_JSON)
    public void loadSkills(@Body Skill[] skillslist) {

        skillServices.loadSkills(skillslist);

    }

    /**
     * Find and read a skill given its id.
     *
     * @param skillid
     * @return
     */

    @Get("/{skillid}")
    public Skill getById(UUID skillid) {
        Skill found = skillServices.readSkill(skillid);
        return found;

    }

    /**
     * Find and read a skill or skills given its id, name, or pending status.
     *
     * @param name
     * @param pending
     * @return
     */

    @Get("/{?name,pending}")
    public List<Skill> findByValue(@Nullable String name, @Nullable Boolean pending) {

        List<Skill> found = skillServices.findByValue(name, pending);
        return found;

    }

    /**
     * Update the pending status of a skill.
     * @param skill
     * @return
     */
    @Put("/")
    public HttpResponse<?> update(@Body @Valid Skill skill) {

        if(null != skill.getSkillid()) {
            Skill updatedSkill = skillServices.update(skill);
            return HttpResponse
                    .ok()
                    .headers(headers -> headers.location(location(updatedSkill.getSkillid())))
                    .body(updatedSkill);
        }

        return HttpResponse.badRequest();
    }

    protected URI location(UUID uuid) {
        return URI.create("/skill/" + uuid);
    }

}