import { Controller, Get, Post, Param, Body, UseGuards, Request } from '@nestjs/common';
import { AdminService } from './admin.service';
import { JwtAuthGuard } from '../auth/jwt-auth.guard';
import { ApiResponse } from '../common/response.dto';
import { GroupRole } from '../entities/group-member.entity';

@UseGuards(JwtAuthGuard)
@Controller('admin')
export class AdminController {
  constructor(private readonly adminService: AdminService) {}

  @Get('groups/:id/dashboard')
  async getDashboard(@Request() req: any, @Param('id') groupId: string) {
    const stats = await this.adminService.getGroupAdminDashboard(req.user.id, groupId);
    return ApiResponse.ok(stats, 'Admin dashboard retrieved');
  }

  @Post('groups/:id/members/:userId/role')
  async updateRole(
    @Request() req: any,
    @Param('id') groupId: string,
    @Param('userId') targetUserId: string,
    @Body('role') role: GroupRole,
  ) {
    const result = await this.adminService.changeMemberRole(req.user.id, groupId, targetUserId, role);
    return ApiResponse.ok(result, 'Role updated successfully');
  }
}
