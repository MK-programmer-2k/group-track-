import {
  Controller,
  Get,
  Post,
  Body,
  Query,
  UseGuards,
  Request,
} from '@nestjs/common';
import { LocationsService } from './locations.service';
import {
  StartSharingDto,
  StopSharingDto,
  LocationPointDto,
  BatchLocationsDto,
} from './locations.dto';
import { JwtAuthGuard } from '../auth/jwt-auth.guard';
import { ApiResponse } from '../common/response.dto';

@UseGuards(JwtAuthGuard)
@Controller()
export class LocationsController {
  constructor(private readonly locationsService: LocationsService) {}

  @Post('location-sharing/start')
  async startSharing(@Request() req: any, @Body() dto: StartSharingDto) {
    const result = await this.locationsService.startSharing(req.user.id, dto);
    return ApiResponse.ok(result, 'Location sharing started');
  }

  @Post('location-sharing/stop')
  async stopSharing(@Request() req: any, @Body() dto: StopSharingDto) {
    const result = await this.locationsService.stopSharing(req.user.id, dto);
    return ApiResponse.ok(result, 'Location sharing stopped');
  }

  @Get('location-sharing/status')
  async getSharingStatus(@Request() req: any, @Query('groupId') groupId?: string) {
    const result = await this.locationsService.getSharingStatus(req.user.id, groupId);
    return ApiResponse.ok(result, 'Sharing status retrieved');
  }

  @Post('locations')
  async recordLocation(@Request() req: any, @Body() dto: LocationPointDto) {
    const result = await this.locationsService.recordLocation(req.user.id, dto);
    return ApiResponse.ok(result, 'Location recorded');
  }

  @Post('locations/batch')
  async recordBatch(@Request() req: any, @Body() dto: BatchLocationsDto) {
    const result = await this.locationsService.recordBatch(req.user.id, dto.locations);
    return ApiResponse.ok(result, `Batch synced: ${result.inserted} points recorded`);
  }

  @Get('locations/latest')
  async getLatestLocations(@Request() req: any, @Query('groupId') groupId: string) {
    const result = await this.locationsService.getLatestLocations(req.user.id, groupId);
    return ApiResponse.ok(result, 'Latest locations retrieved');
  }

  @Get('locations/history')
  async getLocationHistory(
    @Request() req: any,
    @Query('groupId') groupId: string,
    @Query('userId') targetUserId?: string,
    @Query('range') range: string = 'today',
  ) {
    const result = await this.locationsService.getLocationHistory(
      req.user.id,
      groupId,
      targetUserId,
      range,
    );
    return ApiResponse.ok(result, 'Location history retrieved');
  }
}
