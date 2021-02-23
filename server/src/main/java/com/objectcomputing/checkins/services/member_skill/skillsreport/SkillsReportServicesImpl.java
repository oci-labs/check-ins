package com.objectcomputing.checkins.services.member_skill.skillsreport;

import com.objectcomputing.checkins.services.member_skill.MemberSkillRepository;
import com.objectcomputing.checkins.services.memberprofile.MemberProfileRepository;
import com.objectcomputing.checkins.services.member_skill.MemberSkill;
import com.objectcomputing.checkins.exceptions.BadArgException;

import javax.inject.Singleton;
import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.Set;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

@Singleton
public class SkillsReportServicesImpl implements SkillsReportServices {
    private final MemberSkillRepository memberSkillRepo;
    private final MemberProfileRepository memberProfileRepo;
    private final HashMap<String, Integer> skillLevels;

    public SkillsReportServicesImpl(MemberSkillRepository memberSkillRepo,
                                    MemberProfileRepository memberProfileRepo) {
        this.memberSkillRepo = memberSkillRepo;
        this.memberProfileRepo = memberProfileRepo;

        skillLevels = new HashMap<>();
        skillLevels.put("interested", 0);
        skillLevels.put("novice", 1);
        skillLevels.put("intermediate", 2);
        skillLevels.put("advanced", 3);
        skillLevels.put("expert", 4);
    }

    public SkillsReportResponseDTO report(@NotNull SkillsReportRequestDTO request) {
        final List<SkillLevelDTO> skills = request.getSkills();
        if (skills == null) {
            throw new BadArgException("Invalid list of requested skills");
        }

        final Set<UUID> members = request.getMembers();
        final Boolean inclusive = request.isInclusive();
        SkillsReportResponseDTO response = new SkillsReportResponseDTO();

        List<TeamMemberSkillDTO> potentialMembers = getPotentialQualifyingMembers(skills);
        if (members == null || members.isEmpty()) {
            if (inclusive == null || !inclusive) {
                response.setTeamMembers(potentialMembers);
            } else {
                List<TeamMemberSkillDTO> qualifiedMembers = getMembersSatisfyingAllSkills(potentialMembers, skills);
                response.setTeamMembers(qualifiedMembers);
            }
        } else {
            if (inclusive == null || !inclusive) {
                List<TeamMemberSkillDTO> qualifiedMembers = removeMembersNotRequested(potentialMembers, members);
                response.setTeamMembers(qualifiedMembers);
            } else {
                List<TeamMemberSkillDTO> membersInList = removeMembersNotRequested(potentialMembers, members);
                List<TeamMemberSkillDTO> qualifiedMembers = getMembersSatisfyingAllSkills(membersInList, skills);
                response.setTeamMembers(qualifiedMembers);
            }
        }

        return response;
    }

    @NotNull
    private List<TeamMemberSkillDTO> getPotentialQualifyingMembers(@NotNull List<SkillLevelDTO> skills) {
        // Get all member_skill entries that satisfy a requested skill
        List<MemberSkill> entries = new ArrayList<>();

        for (SkillLevelDTO skill : skills) {
            if (skill.getId() == null) {
                throw new BadArgException("Invalid requested skill ID");
            }

            List<MemberSkill> temp = memberSkillRepo.findBySkillid(skill.getId());
            if (skill.getLevel() != null) {
                for (MemberSkill memSkill : temp) {
                    if (isSkillLevelSatisfied(memSkill.getSkilllevel(), skill.getLevel())) {
                        entries.add(memSkill);
                    }
                }
            } else {
                // The input doesn't specify a required level, so all members have this skill are added
                entries.addAll(temp);
            }
        }

        // Collect all entries belong to each team member
        HashMap<UUID, TeamMemberSkillDTO> map = new HashMap<>();
        for (MemberSkill ms : entries) {
            final UUID memberId = ms.getMemberid();
            SkillLevelDTO skill = new SkillLevelDTO();
            skill.setId(ms.getSkillid());
            skill.setLevel(ms.getSkilllevel());

            if (map.containsKey(memberId)) {
                TeamMemberSkillDTO dto = map.get(memberId);
                dto.getSkills().add(skill);
            } else {
                TeamMemberSkillDTO dto = new TeamMemberSkillDTO();
                dto.setId(memberId);

                String memberName = memberProfileRepo.findNameById(memberId);
                dto.setName(memberName);

                List<SkillLevelDTO> memberSkills = new ArrayList<>();
                memberSkills.add(skill);
                dto.setSkills(memberSkills);

                map.put(memberId, dto);
            }
        }

        return new ArrayList<>(map.values());
    }

    @NotNull
    private List<TeamMemberSkillDTO> getMembersSatisfyingAllSkills(@NotNull List<TeamMemberSkillDTO> potentialMembers,
                                                                   @NotNull List<SkillLevelDTO> requestedSkills) {
        List<TeamMemberSkillDTO> ret = new ArrayList<>();
        for (TeamMemberSkillDTO member : potentialMembers) {
            List<SkillLevelDTO> memberSkills = member.getSkills();
            boolean lackSomeSkill = false;

            for (SkillLevelDTO reqSkill : requestedSkills) {
                boolean found = false;
                for (SkillLevelDTO memSkill : memberSkills) {
                    if (memSkill.getId() == reqSkill.getId()) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    lackSomeSkill = true;
                    break;
                }
            }

            if (!lackSomeSkill) {
                ret.add(member);
            }
        }

        return ret;
    }

    @NotNull
    private List<TeamMemberSkillDTO> removeMembersNotRequested(@NotNull List<TeamMemberSkillDTO> potentialMembers,
                                                               @NotNull Set<UUID> requestedMembers) {
        List<TeamMemberSkillDTO> ret = new ArrayList<>();
        for (TeamMemberSkillDTO member : potentialMembers) {
            if (requestedMembers.contains(member.getId())) {
                ret.add(member);
            }
        }

        return ret;
    }

    private boolean isSkillLevelSatisfied(String first, String second) {
        String firstLc = first.toLowerCase();
        String secondLc = second.toLowerCase();
        if (!skillLevels.containsKey(firstLc) || !skillLevels.containsKey(secondLc)) {
            throw new BadArgException(String.format("Compare invalid skill level: %s and %s", first, second));
        }

        return skillLevels.get(firstLc) >= skillLevels.get(secondLc);
    }
}
