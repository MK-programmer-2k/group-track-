import {
  Injectable,
  ForbiddenException,
  BadRequestException,
  Logger,
} from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository, Between, In, LessThan } from 'typeorm';
import { LocationRecord } from '../entities/location.entity';
import { LocationSharing } from '../entities/location-sharing.entity';
import { GroupMember, MemberStatus } from '../entities/group-member.entity';
import { User } from '../entities/user.entity';
import { RealtimeGateway } from '../realtime/realtime.gateway';
import {
  StartSharingDto,
  StopSharingDto,
  LocationPointDto,
  SharingDuration,
} from './locations.dto';

@Injectable()
export class LocationsService {
  private readonly logger = new Logger(LocationsService.name);

  constructor(
    @InjectRepository(LocationRecord)
    private readonly locationRepository: Repository<LocationRecord>,
    @InjectRepository(LocationSharing)
    private readonly sharingRepository: Repository<LocationSharing>,
    @InjectRepository(GroupMember)
    private readonly memberRepository: Repository<GroupMember>,
    @InjectRepository(User)
    private readonly userRepository: Repository<User>,
    private readonly realtimeGateway: RealtimeGateway,
  ) {}

  async startSharing(userId: string, dto: StartSharingDto) {
    // Verify membership
    await this.assertMember(userId, dto.groupId);

    let expiresAt: Date | null = null;
    const now = new Date();
    const duration = dto.duration || SharingDuration.UNTIL_STOP;

    if (duration === SharingDuration.ONE_HOUR) {
      expiresAt = new Date(now.getTime() + 60 * 60 * 1000);
    } else if (duration === SharingDuration.FOUR_HOURS) {
      expiresAt = new Date(now.getTime() + 4 * 60 * 60 * 1000);
    } else if (duration === SharingDuration.END_OF_DAY) {
      expiresAt = new Date(now);
      expiresAt.setHours(23, 59, 59, 999);
    }

    let sharing = await this.sharingRepository.findOne({
      where: { userId, groupId: dto.groupId },
    });

    if (!sharing) {
      sharing = this.sharingRepository.create({
        userId,
        groupId: dto.groupId,
      });
    }

    sharing.enabled = true;
    sharing.sharingDuration = duration;
    sharing.expiresAt = expiresAt;
    sharing.startedAt = now;
    sharing.stoppedAt = null;
    sharing.lastUpdatedAt = now;

    const saved = await this.sharingRepository.save(sharing);

    this.realtimeGateway.broadcastSharingStatus(dto.groupId, {
      userId,
      groupId: dto.groupId,
      enabled: true,
      duration,
      expiresAt,
    });

    return saved;
  }

  async stopSharing(userId: string, dto: StopSharingDto) {
    const sharing = await this.sharingRepository.findOne({
      where: { userId, groupId: dto.groupId },
    });

    if (sharing) {
      sharing.enabled = false;
      sharing.stoppedAt = new Date();
      await this.sharingRepository.save(sharing);
    }

    this.realtimeGateway.broadcastSharingStatus(dto.groupId, {
      userId,
      groupId: dto.groupId,
      enabled: false,
    });

    return { stopped: true };
  }

  async getSharingStatus(userId: string, groupId?: string) {
    if (groupId) {
      const status = await this.sharingRepository.findOne({
        where: { userId, groupId },
      });
      return status || { enabled: false, groupId, userId };
    }

    const statuses = await this.sharingRepository.find({
      where: { userId },
    });
    return statuses;
  }

  async recordLocation(userId: string, dto: LocationPointDto) {
    // 1. Verify active membership
    await this.assertMember(userId, dto.groupId);

    // 2. Check if user has active sharing enabled for this group
    const sharing = await this.sharingRepository.findOne({
      where: { userId, groupId: dto.groupId, enabled: true },
    });

    if (!sharing) {
      throw new ForbiddenException('Location sharing is not enabled for this group');
    }

    // Check expiration
    if (sharing.expiresAt && new Date() > new Date(sharing.expiresAt)) {
      sharing.enabled = false;
      sharing.stoppedAt = new Date();
      await this.sharingRepository.save(sharing);
      this.realtimeGateway.broadcastSharingStatus(dto.groupId, {
        userId,
        groupId: dto.groupId,
        enabled: false,
        reason: 'EXPIRED',
      });
      throw new ForbiddenException('Location sharing duration has expired');
    }

    // 3. Coordinate validation
    if (
      dto.latitude < -90 ||
      dto.latitude > 90 ||
      dto.longitude < -180 ||
      dto.longitude > 180
    ) {
      throw new BadRequestException('Invalid geographic coordinates');
    }

    const recordedAt = new Date(dto.recordedAt);

    // 4. Save location record
    const record = this.locationRepository.create({
      userId,
      groupId: dto.groupId,
      latitude: dto.latitude,
      longitude: dto.longitude,
      accuracy: dto.accuracy || null,
      altitude: dto.altitude || null,
      speed: dto.speed || null,
      bearing: dto.bearing || null,
      recordedAt,
    });

    const saved = await this.locationRepository.save(record);

    // Update sharing last update
    sharing.lastUpdatedAt = recordedAt;
    await this.sharingRepository.save(sharing);

    const user = await this.userRepository.findOne({ where: { id: userId } });

    // 5. Broadcast to authorized group room via WebSocket
    const broadcastPayload = {
      id: saved.id,
      userId,
      userName: user ? user.name : 'Member',
      groupId: dto.groupId,
      latitude: dto.latitude,
      longitude: dto.longitude,
      accuracy: dto.accuracy,
      speed: dto.speed,
      bearing: dto.bearing,
      recordedAt: saved.recordedAt,
      isLive: true,
    };

    this.realtimeGateway.broadcastLocationUpdate(dto.groupId, broadcastPayload);

    return saved;
  }

  async recordBatch(userId: string, locations: LocationPointDto[]) {
    if (!locations || locations.length === 0) return { inserted: 0 };

    let successCount = 0;
    for (const loc of locations) {
      try {
        await this.recordLocation(userId, loc);
        successCount++;
      } catch (err) {
        this.logger.warn(`Batch point skipped for user ${userId}: ${err.message}`);
      }
    }

    return { inserted: successCount, total: locations.length };
  }

  async getLatestLocations(currentUserId: string, groupId: string) {
    // Ensure caller is active member of group
    await this.assertMember(currentUserId, groupId);

    // Find all members who currently have location sharing enabled in this group
    const activeShares = await this.sharingRepository.find({
      where: { groupId, enabled: true },
    });

    if (activeShares.length === 0) {
      return [];
    }

    const sharerUserIds = activeShares.map((s) => s.userId);

    // Fetch latest location for each active sharer
    const latestLocations = await Promise.all(
      sharerUserIds.map(async (uid) => {
        const latest = await this.locationRepository.findOne({
          where: { userId: uid, groupId },
          order: { recordedAt: 'DESC' },
        });

        const user = await this.userRepository.findOne({ where: { id: uid } });
        const shareMeta = activeShares.find((s) => s.userId === uid);

        if (!latest && !user) return null;

        const lastSeen = latest ? latest.recordedAt : shareMeta?.lastUpdatedAt;
        const now = Date.now();
        const diffSeconds = lastSeen ? Math.floor((now - new Date(lastSeen).getTime()) / 1000) : 999999;
        const isOnline = diffSeconds < 120; // 2 minutes threshold for online

        return {
          userId: uid,
          userName: user?.name || 'Member',
          groupId,
          latitude: latest ? latest.latitude : 0.0,
          longitude: latest ? latest.longitude : 0.0,
          accuracy: latest?.accuracy || null,
          speed: latest?.speed || null,
          bearing: latest?.bearing || null,
          recordedAt: latest?.recordedAt || shareMeta?.lastUpdatedAt,
          isLive: shareMeta?.enabled || false,
          isOnline,
          diffSeconds,
        };
      }),
    );

    return latestLocations.filter((l) => l !== null && (l.latitude !== 0 || l.longitude !== 0));
  }

  async getLocationHistory(
    currentUserId: string,
    groupId: string,
    targetUserId?: string,
    timeRange: string = 'today',
  ) {
    await this.assertMember(currentUserId, groupId);

    const now = new Date();
    let fromDate = new Date();

    if (timeRange === 'yesterday') {
      fromDate.setDate(now.getDate() - 1);
      fromDate.setHours(0, 0, 0, 0);
      const toDate = new Date(fromDate);
      toDate.setHours(23, 59, 59, 999);
      return this.queryHistory(groupId, targetUserId, fromDate, toDate);
    } else if (timeRange === '7days') {
      fromDate.setDate(now.getDate() - 7);
    } else {
      // 'today'
      fromDate.setHours(0, 0, 0, 0);
    }

    return this.queryHistory(groupId, targetUserId, fromDate, now);
  }

  private async queryHistory(
    groupId: string,
    targetUserId: string | undefined,
    from: Date,
    to: Date,
  ) {
    const whereClause: any = {
      groupId,
      recordedAt: Between(from, to),
    };

    if (targetUserId) {
      whereClause.userId = targetUserId;
    }

    const records = await this.locationRepository.find({
      where: whereClause,
      order: { recordedAt: 'ASC' },
      take: 1000,
    });

    return records;
  }

  async cleanupOldLocations(retentionDays: number = 30): Promise<number> {
    const cutoff = new Date();
    cutoff.setDate(cutoff.getDate() - retentionDays);

    const result = await this.locationRepository.delete({
      recordedAt: LessThan(cutoff),
    });

    this.logger.log(
      `Retention cleanup completed: removed ${result.affected || 0} locations older than ${retentionDays} days`,
    );
    return result.affected || 0;
  }

  private async assertMember(userId: string, groupId: string) {
    const member = await this.memberRepository.findOne({
      where: { userId, groupId, status: MemberStatus.ACTIVE },
    });
    if (!member) {
      throw new ForbiddenException('You must be an active member of this group to access location services');
    }
  }
}
