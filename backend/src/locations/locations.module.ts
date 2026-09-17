import { Module } from '@nestjs/common';
import { TypeOrmModule } from '@nestjs/typeorm';
import { ScheduleModule } from '@nestjs/schedule';
import { LocationRecord } from '../entities/location.entity';
import { LocationSharing } from '../entities/location-sharing.entity';
import { GroupMember } from '../entities/group-member.entity';
import { User } from '../entities/user.entity';
import { LocationsService } from './locations.service';
import { LocationsController } from './locations.controller';
import { RetentionTask } from './retention.task';
import { RealtimeModule } from '../realtime/realtime.module';

@Module({
  imports: [
    TypeOrmModule.forFeature([LocationRecord, LocationSharing, GroupMember, User]),
    ScheduleModule.forRoot(),
    RealtimeModule,
  ],
  controllers: [LocationsController],
  providers: [LocationsService, RetentionTask],
  exports: [LocationsService],
})
export class LocationsModule {}
