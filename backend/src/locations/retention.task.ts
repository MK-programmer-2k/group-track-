import { Injectable, Logger } from '@nestjs/common';
import { Cron, CronExpression } from '@nestjs/schedule';
import { LocationsService } from './locations.service';

@Injectable()
export class RetentionTask {
  private readonly logger = new Logger(RetentionTask.name);

  constructor(private readonly locationsService: LocationsService) {}

  // Run daily at midnight to enforce retention policy
  @Cron(CronExpression.EVERY_DAY_AT_MIDNIGHT)
  async handleRetentionCleanup() {
    const retentionDays = parseInt(process.env.LOCATION_RETENTION_DAYS || '30', 10);
    this.logger.log(`Running scheduled location retention cleanup (${retentionDays} days)...`);
    await this.locationsService.cleanupOldLocations(retentionDays);
  }
}
