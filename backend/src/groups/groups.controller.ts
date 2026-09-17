import {
  Controller,
  Get,
  Post,
  Delete,
  Param,
  Body,
  UseGuards,
  Request,
} from '@nestjs/common';
import { GroupsService } from './groups.service';
import { CreateGroupDto, JoinGroupDto, ApproveMemberDto } from './groups.dto';
import { JwtAuthGuard } from '../auth/jwt-auth.guard';
import { ApiResponse } from '../common/response.dto';

@UseGuards(JwtAuthGuard)
@Controller('groups')
export class GroupsController {
  constructor(private readonly groupsService: GroupsService) {}

  @Post()
  async createGroup(@Request() req: any, @Body() dto: CreateGroupDto) {
    const group = await this.groupsService.createGroup(req.user.id, dto);
    return ApiResponse.ok(group, 'Group created successfully');
  }

  @Get()
  async getGroups(@Request() req: any) {
    const groups = await this.groupsService.getUserGroups(req.user.id);
    return ApiResponse.ok(groups, 'Groups fetched successfully');
  }

  @Get(':id')
  async getGroupDetails(@Request() req: any, @Param('id') id: string) {
    const details = await this.groupsService.getGroupDetails(req.user.id, id);
    return ApiResponse.ok(details, 'Group details fetched successfully');
  }

  @Post('join')
  async joinGroup(@Request() req: any, @Body() dto: JoinGroupDto) {
    const result = await this.groupsService.joinGroup(req.user.id, dto);
    return ApiResponse.ok(result, 'Joined group successfully');
  }

  @Post(':id/approve')
  async approveMember(
    @Request() req: any,
    @Param('id') groupId: string,
    @Body() dto: ApproveMemberDto,
  ) {
    const result = await this.groupsService.approveMember(req.user.id, groupId, dto.userId);
    return ApiResponse.ok(result, 'Member approved');
  }

  @Delete(':id/members/:userId')
  async removeMember(
    @Request() req: any,
    @Param('id') groupId: string,
    @Param('userId') targetUserId: string,
  ) {
    const result = await this.groupsService.removeMember(req.user.id, groupId, targetUserId);
    return ApiResponse.ok(result, 'Member removed');
  }
}
