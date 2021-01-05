package com.objectcomputing.checkins.services.team.member;

import com.objectcomputing.checkins.services.exceptions.BadArgException;
import com.objectcomputing.checkins.services.memberprofile.MemberProfile;
import com.objectcomputing.checkins.services.memberprofile.MemberProfileRepository;
import com.objectcomputing.checkins.services.memberprofile.currentuser.CurrentUserServices;
import com.objectcomputing.checkins.services.team.Team;
import com.objectcomputing.checkins.services.team.TeamRepository;

import javax.annotation.Nullable;
import javax.inject.Singleton;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Singleton
public class TeamMemberServicesImpl implements TeamMemberServices {

    private final TeamRepository teamRepo;
    private final TeamMemberRepository teamMemberRepo;
    private final MemberProfileRepository memberRepo;
    private final CurrentUserServices currentUserServices;

    public TeamMemberServicesImpl(TeamRepository teamRepo,
                                  TeamMemberRepository teamMemberRepo,
                                  MemberProfileRepository memberRepo,
                                  CurrentUserServices currentUserServices) {
        this.teamRepo = teamRepo;
        this.teamMemberRepo = teamMemberRepo;
        this.memberRepo = memberRepo;
        this.currentUserServices = currentUserServices;
    }

    public TeamMember save(@Valid @NotNull TeamMember teamMember) {
        MemberProfile currentUser = currentUserServices.getCurrentUser();
        boolean isAdmin = currentUserServices.isAdmin();

        final UUID teamId = teamMember.getTeamid();
        final UUID memberId = teamMember.getMemberid();
        Optional<Team> team = teamRepo.findById(teamId);
        if(team.isEmpty()) {
            throw new BadArgException(String.format("Team %s doesn't exist", teamId));
        }

        Set<TeamMember> teamLeads = this.findByFields(teamId, null, true);

        if(teamMember.getId() != null) {
            throw new BadArgException(String.format("Found unexpected id %s for team member", teamMember.getId()));
        } else if(memberRepo.findById(memberId).isEmpty()) {
            throw new BadArgException(String.format("Member %s doesn't exist", memberId));
        } else if(teamMemberRepo.findByTeamidAndMemberid(teamMember.getTeamid(), teamMember.getMemberid()).isPresent()) {
            throw new BadArgException(String.format("Member %s already exists in team %s", memberId, teamId));
        } else if(!isAdmin && teamLeads.stream().noneMatch(o -> o.getMemberid().equals(currentUser.getId()))) {
            throw new BadArgException("You are not authorized to perform this operation");
        }

        return teamMemberRepo.save(teamMember);
    }

    public TeamMember read(@NotNull UUID id) {
        return teamMemberRepo.findById(id).orElse(null);
    }

    public TeamMember update(@NotNull @Valid TeamMember teamMember) {
        MemberProfile currentUser = currentUserServices.getCurrentUser();
        boolean isAdmin = currentUserServices.isAdmin();

        final UUID id = teamMember.getId();
        final UUID teamId = teamMember.getTeamid();
        final UUID memberId = teamMember.getMemberid();
        Optional<Team> team = teamRepo.findById(teamId);

        if(team.isEmpty()) {
            throw new BadArgException(String.format("Team %s doesn't exist", teamId));
        }

        Set<TeamMember> teamLeads = this.findByFields(teamId, null, true);

        if (id == null || teamMemberRepo.findById(id).isEmpty()) {
            throw new BadArgException(String.format("Unable to locate teamMember to update with id %s", id));
        } else if (memberRepo.findById(memberId).isEmpty()) {
            throw new BadArgException(String.format("Member %s doesn't exist", memberId));
        } else if(teamMemberRepo.findByTeamidAndMemberid(teamMember.getTeamid(), teamMember.getMemberid()).isEmpty()) {
            throw new BadArgException(String.format("Member %s is not part of team %s", memberId, teamId));
        } else if(!isAdmin && teamLeads.stream().noneMatch(o -> o.getMemberid().equals(currentUser.getId()))) {
            throw new BadArgException("You are not authorized to perform this operation");
        }

        return teamMemberRepo.update(teamMember);
    }

    public Set<TeamMember> findByFields(@Nullable UUID teamid, @Nullable UUID memberid, @Nullable Boolean lead) {
        Set<TeamMember> teamMembers = new HashSet<>();
        teamMemberRepo.findAll().forEach(teamMembers::add);

        if (teamid != null) {
            teamMembers.retainAll(teamMemberRepo.findByTeamid(teamid));
        }
        if (memberid != null) {
            teamMembers.retainAll(teamMemberRepo.findByMemberid(memberid));
        }
        if (lead != null) {
            teamMembers.retainAll(teamMemberRepo.findByLead(lead));
        }

        return teamMembers;
    }
}