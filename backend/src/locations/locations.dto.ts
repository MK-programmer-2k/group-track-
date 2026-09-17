import {
  IsBoolean,
  IsEnum,
  IsNotEmpty,
  IsNumber,
  IsOptional,
  IsString,
  Max,
  Min,
  ValidateNested,
} from 'class-validator';
import { Type } from 'class-transformer';

export enum SharingDuration {
  UNTIL_STOP = 'UNTIL_STOP',
  ONE_HOUR = 'ONE_HOUR',
  FOUR_HOURS = 'FOUR_HOURS',
  END_OF_DAY = 'END_OF_DAY',
}

export class StartSharingDto {
  @IsNotEmpty()
  @IsString()
  groupId: string;

  @IsOptional()
  @IsEnum(SharingDuration)
  duration?: SharingDuration;
}

export class StopSharingDto {
  @IsNotEmpty()
  @IsString()
  groupId: string;
}

export class LocationPointDto {
  @IsNotEmpty()
  @IsString()
  groupId: string;

  @IsNumber()
  @Min(-90)
  @Max(90)
  latitude: number;

  @IsNumber()
  @Min(-180)
  @Max(180)
  longitude: number;

  @IsOptional()
  @IsNumber()
  @Min(0)
  @Max(500)
  accuracy?: number;

  @IsOptional()
  @IsNumber()
  altitude?: number;

  @IsOptional()
  @IsNumber()
  @Min(0)
  speed?: number;

  @IsOptional()
  @IsNumber()
  @Min(0)
  @Max(360)
  bearing?: number;

  @IsNotEmpty()
  @IsString()
  recordedAt: string;
}

export class BatchLocationsDto {
  @ValidateNested({ each: true })
  @Type(() => LocationPointDto)
  locations: LocationPointDto[];
}
