import { LocationsService } from '../src/locations/locations.service';
import { ForbiddenException, BadRequestException } from '@nestjs/common';

describe('LocationsService Authorization & Validation Unit Tests', () => {
  let service: LocationsService;
  let mockLocationRepo: any;
  let mockSharingRepo: any;
  let mockMemberRepo: any;
  let mockUserRepo: any;
  let mockGateway: any;

  beforeEach(() => {
    mockLocationRepo = {
      create: jest.fn((dto) => dto),
      save: jest.fn((dto) => Promise.resolve({ id: 'loc-1', ...dto })),
      findOne: jest.fn(),
      find: jest.fn(),
    };
    mockSharingRepo = {
      findOne: jest.fn(),
      find: jest.fn(),
      create: jest.fn((dto) => dto),
      save: jest.fn((dto) => Promise.resolve(dto)),
    };
    mockMemberRepo = {
      findOne: jest.fn(),
    };
    mockUserRepo = {
      findOne: jest.fn(),
    };
    mockGateway = {
      broadcastLocationUpdate: jest.fn(),
      broadcastSharingStatus: jest.fn(),
    };

    service = new LocationsService(
      mockLocationRepo,
      mockSharingRepo,
      mockMemberRepo,
      mockUserRepo,
      mockGateway,
    );
  });

  it('rejects location recording if user is NOT an active member of group', async () => {
    mockMemberRepo.findOne.mockResolvedValue(null);

    await expect(
      service.recordLocation('unauthorized-user', {
        groupId: 'group-1',
        latitude: 13.0827,
        longitude: 80.2707,
        recordedAt: new Date().toISOString(),
      }),
    ).rejects.toThrow(ForbiddenException);
  });

  it('rejects location recording if location sharing is disabled by user', async () => {
    // User is member
    mockMemberRepo.findOne.mockResolvedValue({ id: 'mem-1', status: 'ACTIVE' });
    // But sharing is disabled
    mockSharingRepo.findOne.mockResolvedValue(null);

    await expect(
      service.recordLocation('user-1', {
        groupId: 'group-1',
        latitude: 13.0827,
        longitude: 80.2707,
        recordedAt: new Date().toISOString(),
      }),
    ).rejects.toThrow(ForbiddenException);
  });

  it('rejects invalid coordinates with out-of-bounds latitude/longitude', async () => {
    mockMemberRepo.findOne.mockResolvedValue({ id: 'mem-1', status: 'ACTIVE' });
    mockSharingRepo.findOne.mockResolvedValue({ enabled: true });

    await expect(
      service.recordLocation('user-1', {
        groupId: 'group-1',
        latitude: 95.0, // Invalid > 90
        longitude: 80.2707,
        recordedAt: new Date().toISOString(),
      }),
    ).rejects.toThrow(BadRequestException);
  });

  it('records location and broadcasts to group when authorized', async () => {
    mockMemberRepo.findOne.mockResolvedValue({ id: 'mem-1', status: 'ACTIVE' });
    mockSharingRepo.findOne.mockResolvedValue({ enabled: true, lastUpdatedAt: new Date() });
    mockUserRepo.findOne.mockResolvedValue({ id: 'user-1', name: 'Arun' });

    const result = await service.recordLocation('user-1', {
      groupId: 'group-1',
      latitude: 13.0827,
      longitude: 80.2707,
      accuracy: 10,
      recordedAt: new Date().toISOString(),
    });

    expect(result.latitude).toBe(13.0827);
    expect(mockGateway.broadcastLocationUpdate).toHaveBeenCalledWith(
      'group-1',
      expect.objectContaining({ userId: 'user-1', userName: 'Arun' }),
    );
  });

  it('rejects unauthorized user from viewing group location history', async () => {
    mockMemberRepo.findOne.mockResolvedValue(null); // Not a member

    await expect(
      service.getLocationHistory('intruder-user', 'group-1'),
    ).rejects.toThrow(ForbiddenException);
  });
});
