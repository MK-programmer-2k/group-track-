import { IsNotEmpty, IsOptional, IsString, MaxLength } from 'class-validator';

export class CreateGroupDto {
  @IsNotEmpty()
  @IsString()
  @MaxLength(100)
  name: string;

  @IsOptional()
  @IsString()
  description?: string;
}

export class JoinGroupDto {
  @IsNotEmpty()
  @IsString()
  inviteCode: string;
}

export class ApproveMemberDto {
  @IsNotEmpty()
  @IsString()
  userId: string;
}

export class UpdateMemberRoleDto {
  @IsNotEmpty()
  @IsString()
  role: 'ADMIN' | 'MEMBER';
}
