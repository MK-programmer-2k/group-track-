import { Module } from '@nestjs/common';
import { TypeOrmModule } from '@nestjs/typeorm';
import { AuthModule } from './auth/auth.module';
import { GroupsModule } from './groups/groups.module';
import { LocationsModule } from './locations/locations.module';
import { RealtimeModule } from './realtime/realtime.module';
import { AdminModule } from './admin/admin.module';
import { User } from './entities/user.entity';
import { Group } from './entities/group.entity';
import { GroupMember } from './entities/group-member.entity';
import { LocationSharing } from './entities/location-sharing.entity';
import { LocationRecord } from './entities/location.entity';

@Module({
  imports: [
    TypeOrmModule.forRoot({
      type: 'postgres',
      host: process.env.DATABASE_HOST || 'localhost',
      port: parseInt(process.env.DATABASE_PORT || '5432', 10),
      username: process.env.DATABASE_USER || 'grouptrack_user',
      password: process.env.DATABASE_PASSWORD || 'grouptrack_secure_pass',
      database: process.env.DATABASE_NAME || 'grouptrack_db',
      entities: [User, Group, GroupMember, LocationSharing, LocationRecord],
      synchronize: false, // Migrations are used for database schema
      logging: process.env.NODE_ENV === 'development',
    }),
    AuthModule,
    GroupsModule,
    LocationsModule,
    RealtimeModule,
    AdminModule,
  ],
})
export class AppModule {}
