package com.objectcomputing.checkins.services.skills;

import com.objectcomputing.checkins.services.exceptions.BadArgException;
import com.objectcomputing.checkins.services.exceptions.PermissionException;
import com.objectcomputing.checkins.services.memberprofile.currentuser.CurrentUserServices;

import javax.inject.Singleton;
import javax.validation.constraints.NotNull;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Singleton
public class SkillServicesImpl implements SkillServices {

    private final SkillRepository skillRepository;
    private final CurrentUserServices currentUserServices;

    public SkillServicesImpl(SkillRepository skillRepository,
                             CurrentUserServices currentUserServices) {
        this.skillRepository = skillRepository;
        this.currentUserServices = currentUserServices;
    }

    public Skill save(Skill skill) {
        Skill newSkill = null;
        if (skill != null) {

            if (skill.getId() != null) {
                throw new BadArgException(String.format("Found unexpected id %s for skill, please try updating instead.",
                        skill.getId()));
            } else if (skillRepository.findByName(skill.getName()).isPresent()) {
                throw new SkillAlreadyExistsException(String.format("Skill %s already exists. ", skill.getName()));
            }

            newSkill = skillRepository.save(skill);
        }
        return newSkill;

    }

    public Skill readSkill(@NotNull UUID id) {
        return skillRepository.findById(id).orElse(null);
    }

    public Set<Skill> findByValue(String name, Boolean pending) {
        Set<Skill> skillList = new HashSet<>();
        skillRepository.findAll().forEach(skillList::add);

        if (name != null) {
            skillList.retainAll(findByNameLike(name));
        }
        if (pending != null) {
            skillList.retainAll(skillRepository.findByPending(pending));
        }

        return skillList;
    }

    public void delete(@NotNull UUID id) {
        if (!currentUserServices.isAdmin()) {
            throw new PermissionException("You do not have permission to access this resource");
        }
        skillRepository.deleteById(id);
    }

    protected List<Skill> findByNameLike(String name) {
        String wildcard = "%" + name + "%";
        List<Skill> skillList = skillRepository.findByNameIlike(wildcard);

        return skillList;

    }

    public Skill update(@NotNull Skill skill) {

        Skill newSkill = null;
        if (!currentUserServices.isAdmin()) {
            throw new PermissionException("You do not have permission to access this resource");
        }

        if (skill.getId() != null && skillRepository.findById(skill.getId()).isPresent()) {
            newSkill = skillRepository.update(skill);
        } else {
            throw new BadArgException(String.format("Skill %s does not exist, cannot update", skill.getId()));
        }

        return newSkill;

    }

}
