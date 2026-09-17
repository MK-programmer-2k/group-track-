import { Injectable, ForbiddenException, NotFoundException } from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository } from 'typeorm';
import { Group } from '../entities/group.entity';
import { GroupMember, GroupRole, MemberStatus } from '../entities/group-member.entity';
import { LocationSharing } from '../entities/location-sharing.entity';

@Injectable()
export class AdminService {
  constructor(
    @InjectRepository(Group)
    private readonly groupRepository: Repository<Group>,
    @InjectRepository(GroupMember)
    private readonly memberRepository: Repository<GroupMember>,
    @InjectRepository(LocationSharing)
    private readonly sharingRepository: Repository<LocationSharing>,
  ) {}

  async getGroupAdminDashboard(adminUserId: string, groupId: string) {
    const adminMember = await this.memberRepository.findOne({
      where: { groupId, userId: adminUserId, status: MemberStatus.ACTIVE },
    });

    if (!adminMember || (adminMember.role !== GroupRole.OWNER && adminMember.role !== GroupRole.ADMIN)) {
      throw new ForbiddenException('Admin access required');
    }

    const group = await this.groupRepository.findOne({ where: { id: groupId } });
    if (!group) throw new NotFoundException('Group not found');

    const totalMembers = await this.memberRepository.count({
      where: { groupId, status: MemberStatus.ACTIVE },
    });

    const pendingRequests = await this.memberRepository.count({
      where: { groupId, status: MemberStatus.PENDING },
    });

    const activeSharers = await this.sharingRepository.count({
      where: { groupId, enabled: true },
    });

    return {
      groupName: group.name,
      inviteCode: group.inviteCode,
      totalMembers,
      pendingRequests,
      activeSharers,
      myRole: adminMember.role,
    };
  }

  async changeMemberRole(adminUserId: string, groupId: string, targetUserId: string, newRole: GroupRole) {
    const admin = await this.memberRepository.findOne({
      where: { groupId, userId: adminUserId, status: MemberStatus.ACTIVE },
    });
    if (!admin || admin.role !== GroupRole.OWNER) {
      throw new ForbiddenException('Only the group owner can change member roles');
    }

    const member = await this.memberRepository.findOne({
      where: { groupId, userId: targetUserId },
    });
    if (!member) throw new NotFoundException('Member not found');

    member.role = newRole;
    await this.memberRepository.save(member);
    return { success: true, targetUserId, newRole };
  }
}
