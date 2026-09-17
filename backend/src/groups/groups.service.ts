import {
  Injectable,
  NotFoundException,
  ForbiddenException,
  BadRequestException,
  ConflictException,
} from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository } from 'typeorm';
import { Group } from '../entities/group.entity';
import { GroupMember, GroupRole, MemberStatus } from '../entities/group-member.entity';
import { LocationSharing } from '../entities/location-sharing.entity';
import { User } from '../entities/user.entity';
import { CreateGroupDto, JoinGroupDto } from './groups.dto';

@Injectable()
export class GroupsService {
  constructor(
    @InjectRepository(Group)
    private readonly groupRepository: Repository<Group>,
    @InjectRepository(GroupMember)
    private readonly memberRepository: Repository<GroupMember>,
    @InjectRepository(LocationSharing)
    private readonly sharingRepository: Repository<LocationSharing>,
    @InjectRepository(User)
    private readonly userRepository: Repository<User>,
  ) {}

  private generateInviteCode(): string {
    const chars = 'ABCDEFGHJKLMNPQRSTUVWXYZ23456789';
    let code = '';
    for (let i = 0; i < 7; i++) {
      code += chars.charAt(Math.floor(Math.random() * chars.length));
    }
    return code;
  }

  async createGroup(userId: string, dto: CreateGroupDto) {
    let inviteCode = this.generateInviteCode();
    // Ensure code is unique
    while (await this.groupRepository.findOne({ where: { inviteCode } })) {
      inviteCode = this.generateInviteCode();
    }

    const group = this.groupRepository.create({
      name: dto.name,
      description: dto.description || '',
      inviteCode,
      createdBy: userId,
      isActive: true,
    });

    const saved = await this.groupRepository.save(group);

    // Add creator as OWNER
    const member = this.memberRepository.create({
      groupId: saved.id,
      userId,
      role: GroupRole.OWNER,
      status: MemberStatus.ACTIVE,
      approvedAt: new Date(),
    });
    await this.memberRepository.save(member);

    return saved;
  }

  async getUserGroups(userId: string) {
    const memberships = await this.memberRepository.find({
      where: { userId, status: MemberStatus.ACTIVE },
      relations: ['group'],
    });

    const groupIds = memberships.map((m) => m.groupId);
    if (groupIds.length === 0) return [];

    // Fetch counts and member details
    const groupsWithCounts = await Promise.all(
      memberships.map(async (m) => {
        const memberCount = await this.memberRepository.count({
          where: { groupId: m.groupId, status: MemberStatus.ACTIVE },
        });
        const activeSharersCount = await this.sharingRepository.count({
          where: { groupId: m.groupId, enabled: true },
        });

        return {
          ...m.group,
          myRole: m.role,
          memberCount,
          activeSharersCount,
        };
      }),
    );

    return groupsWithCounts;
  }

  async getGroupDetails(userId: string, groupId: string) {
    const membership = await this.memberRepository.findOne({
      where: { groupId, userId, status: MemberStatus.ACTIVE },
    });
    if (!membership) {
      throw new ForbiddenException('You are not an authorized member of this group');
    }

    const group = await this.groupRepository.findOne({ where: { id: groupId } });
    if (!group) {
      throw new NotFoundException('Group not found');
    }

    const members = await this.memberRepository.find({
      where: { groupId },
      relations: ['user'],
    });

    // Also get sharing status for each member
    const sharingStatuses = await this.sharingRepository.find({
      where: { groupId },
    });
    const sharingMap = new Map<string, LocationSharing>();
    sharingStatuses.forEach((s) => sharingMap.set(s.userId, s));

    const memberDetails = members.map((m) => {
      const sharing = sharingMap.get(m.userId);
      return {
        id: m.id,
        userId: m.userId,
        name: m.user?.name || 'Unknown',
        email: m.user?.email || '',
        role: m.role,
        status: m.status,
        joinedAt: m.joinedAt,
        isSharing: sharing ? sharing.enabled : false,
        lastSeenAt: sharing ? sharing.lastUpdatedAt : null,
      };
    });

    return {
      group,
      myRole: membership.role,
      members: memberDetails,
    };
  }

  async joinGroup(userId: string, dto: JoinGroupDto) {
    const group = await this.groupRepository.findOne({
      where: { inviteCode: dto.inviteCode.toUpperCase().trim(), isActive: true },
    });
    if (!group) {
      throw new NotFoundException('Invalid or expired invite code');
    }

    const existing = await this.memberRepository.findOne({
      where: { groupId: group.id, userId },
    });

    if (existing) {
      if (existing.status === MemberStatus.ACTIVE) {
        throw new ConflictException('You are already an active member of this group');
      }
      if (existing.status === MemberStatus.PENDING) {
        throw new BadRequestException('Your join request is already pending approval');
      }
      // If was previously removed, re-join as active or pending
      existing.status = MemberStatus.ACTIVE;
      existing.joinedAt = new Date();
      await this.memberRepository.save(existing);
      return { group, status: MemberStatus.ACTIVE };
    }

    const member = this.memberRepository.create({
      groupId: group.id,
      userId,
      role: GroupRole.MEMBER,
      status: MemberStatus.ACTIVE,
      approvedAt: new Date(),
    });

    await this.memberRepository.save(member);
    return { group, status: MemberStatus.ACTIVE };
  }

  async approveMember(adminUserId: string, groupId: string, targetUserId: string) {
    await this.assertAdminOrOwner(adminUserId, groupId);

    const member = await this.memberRepository.findOne({
      where: { groupId, userId: targetUserId },
    });
    if (!member) {
      throw new NotFoundException('Member request not found');
    }

    member.status = MemberStatus.ACTIVE;
    member.approvedAt = new Date();
    await this.memberRepository.save(member);

    return { approved: true, memberId: member.id };
  }

  async removeMember(adminUserId: string, groupId: string, targetUserId: string) {
    // If leaving self, allowed
    if (adminUserId !== targetUserId) {
      await this.assertAdminOrOwner(adminUserId, groupId);
    }

    const member = await this.memberRepository.findOne({
      where: { groupId, userId: targetUserId },
    });
    if (!member) {
      throw new NotFoundException('Member not found');
    }

    if (member.role === GroupRole.OWNER && adminUserId !== targetUserId) {
      throw new ForbiddenException('Cannot remove the group owner');
    }

    // Stop location sharing if active
    const sharing = await this.sharingRepository.findOne({
      where: { groupId, userId: targetUserId },
    });
    if (sharing) {
      sharing.enabled = false;
      sharing.stoppedAt = new Date();
      await this.sharingRepository.save(sharing);
    }

    await this.memberRepository.remove(member);
    return { removed: true, userId: targetUserId };
  }

  async assertActiveMember(userId: string, groupId: string): Promise<GroupMember> {
    const member = await this.memberRepository.findOne({
      where: { groupId, userId, status: MemberStatus.ACTIVE },
    });
    if (!member) {
      throw new ForbiddenException('Access denied: not an active member of this group');
    }
    return member;
  }

  private async assertAdminOrOwner(userId: string, groupId: string) {
    const member = await this.assertActiveMember(userId, groupId);
    if (member.role !== GroupRole.OWNER && member.role !== GroupRole.ADMIN) {
      throw new ForbiddenException('Only group owners and admins can perform this action');
    }
  }
}
