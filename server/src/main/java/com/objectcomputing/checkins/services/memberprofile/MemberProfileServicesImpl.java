package com.objectcomputing.checkins.services.memberprofile;

import com.objectcomputing.checkins.exceptions.AlreadyExistsException;
import com.objectcomputing.checkins.exceptions.BadArgException;
import com.objectcomputing.checkins.exceptions.NotFoundException;
import com.objectcomputing.checkins.exceptions.PermissionException;
import com.objectcomputing.checkins.services.checkins.CheckInServices;
import com.objectcomputing.checkins.services.member_skill.MemberSkillServices;
import com.objectcomputing.checkins.services.memberprofile.currentuser.CurrentUserServices;
import com.objectcomputing.checkins.services.role.Role;
import com.objectcomputing.checkins.services.role.RoleServices;
import com.objectcomputing.checkins.services.role.RoleType;
import com.objectcomputing.checkins.services.team.member.TeamMemberServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import javax.inject.Singleton;
import javax.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.*;

import static com.objectcomputing.checkins.util.Util.nullSafeUUIDToString;

@Singleton
public class MemberProfileServicesImpl implements MemberProfileServices {

    private static final Logger LOG = LoggerFactory.getLogger(MemberProfileServicesImpl.class);
    private final MemberProfileRepository memberProfileRepository;
    private final CurrentUserServices currentUserServices;
    private final RoleServices roleServices;
    private final CheckInServices checkInServices;
    private final MemberSkillServices memberSkillServices;
    private final TeamMemberServices teamMemberServices;

    public MemberProfileServicesImpl(MemberProfileRepository memberProfileRepository,
                                     CurrentUserServices currentUserServices,
                                     RoleServices roleServices,
                                     CheckInServices checkInServices,
                                     MemberSkillServices memberSkillServices,
                                     TeamMemberServices teamMemberServices) {
        this.memberProfileRepository = memberProfileRepository;
        this.currentUserServices = currentUserServices;
        this.roleServices = roleServices;
        this.checkInServices = checkInServices;
        this.memberSkillServices = memberSkillServices;
        this.teamMemberServices = teamMemberServices;
    }

    @Override
    public MemberProfile getById(@NotNull UUID id) {
        Optional<MemberProfile> memberProfile = memberProfileRepository.findById(id);
        if (memberProfile.isEmpty()) {
            throw new NotFoundException("No member profile for id");
        }
        return memberProfile.get();
    }

    @Override
    public Set<MemberProfile> findByValues(@Nullable String name,
                                           @Nullable String title,
                                           @Nullable UUID pdlId,
                                           @Nullable String workEmail,
                                           @Nullable UUID supervisorId) {
        return new HashSet<>(memberProfileRepository.search(name, title, nullSafeUUIDToString(pdlId),
                workEmail, nullSafeUUIDToString(supervisorId)));
    }

    @Override
    public MemberProfile saveProfile(MemberProfile memberProfile) {
        MemberProfile emailProfile = memberProfileRepository.findByWorkEmail(memberProfile.getWorkEmail()).orElse(null);

        if (emailProfile != null && emailProfile.getId() != null && !Objects.equals(memberProfile.getId(), emailProfile.getId())) {
            throw new AlreadyExistsException(String.format("Email %s already exists in database",
                    memberProfile.getWorkEmail()));
        }

        if (memberProfile.getId() == null) {
            return memberProfileRepository.save(memberProfile);
        }

        return memberProfileRepository.update(memberProfile);
    }

    @Override
    public Boolean deleteProfile(@NotNull UUID id) {
        if (!currentUserServices.isAdmin()) {
            throw new PermissionException("Requires admin privileges");
        }

        // try to delete user - default behavior
        MemberProfile memberProfile = memberProfileRepository.findById(id).orElse(null);
        Set<Role> userRoles = (memberProfile != null) ? roleServices.findByFields(RoleType.PDL, id) : Collections.emptySet();

        if (memberProfile == null) {
            throw new NotFoundException("No member profile for id");
        } else if (!checkInServices.findByFields(id, null, null).isEmpty()) {
            LOG.info("User %s cannot be deleted since Checkin record(s) exist", memberProfile.getName());
        } else if (!memberSkillServices.findByFields(id, null).isEmpty()) {
            LOG.info("User %s cannot be deleted since MemberSkill record(s) exist", memberProfile.getName());
        } else if (!teamMemberServices.findByFields(null, id, null).isEmpty()) {
            LOG.info("User %s cannot be deleted since TeamMember record(s) exist", memberProfile.getName());
        } else if (!userRoles.isEmpty()) {
            LOG.info("User %s cannot be deleted since user has PDL role", memberProfile.getName());
        } else {
            // delete the user
            memberProfileRepository.deleteById(id);
            return true;
        }

        // Terminate the user if user is not deleted
        // Update PDL ID for all associated members before termination
        List<MemberProfile> pdlFor = memberProfileRepository.search(null, null, nullSafeUUIDToString(id), null, null);
        for (MemberProfile member : pdlFor) {
            member.setPdlId(null);
            memberProfileRepository.update(member);
        }

        // Terminate user
        memberProfile.setTerminationDate(LocalDate.now());
        memberProfile.setPdlId(null);
        memberProfileRepository.update(memberProfile);
        return true;
    }

    @Override
    public MemberProfile findByName(@NotNull String name) {
        List<MemberProfile> searchResult = memberProfileRepository.search(name, null, null, null, null);
        if (searchResult.size() != 1) {
            throw new BadArgException("Expected exactly 1 result. Found " + searchResult.size());
        }
        return searchResult.get(0);
    }
}
