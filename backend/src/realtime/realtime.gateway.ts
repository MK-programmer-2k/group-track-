import {
  WebSocketGateway,
  WebSocketServer,
  SubscribeMessage,
  OnGatewayConnection,
  OnGatewayDisconnect,
} from '@nestjs/websockets';
import { Server, Socket } from 'socket.io';
import { JwtService } from '@nestjs/jwt';
import { Injectable, Logger } from '@nestjs/common';

@WebSocketGateway({
  cors: {
    origin: '*',
  },
})
@Injectable()
export class RealtimeGateway implements OnGatewayConnection, OnGatewayDisconnect {
  @WebSocketServer()
  server: Server;

  private readonly logger = new Logger(RealtimeGateway.name);
  private userSockets = new Map<string, Set<string>>(); // userId -> socketIds

  constructor(private readonly jwtService: JwtService) {}

  async handleConnection(client: Socket) {
    try {
      const token =
        client.handshake.auth?.token ||
        client.handshake.headers?.authorization?.replace('Bearer ', '');

      if (!token) {
        client.disconnect();
        return;
      }

      const payload = this.jwtService.verify(token, {
        secret: process.env.JWT_SECRET || 'grouptrack_jwt_super_secret_production_key_2026',
      });

      client.data.user = payload;
      const userId = payload.sub;

      if (!this.userSockets.has(userId)) {
        this.userSockets.set(userId, new Set());
      }
      this.userSockets.get(userId)!.add(client.id);

      this.logger.log(`Socket connected: ${client.id} for user ${userId}`);
    } catch (e) {
      this.logger.warn(`Unauthorized socket connection: ${e.message}`);
      client.disconnect();
    }
  }

  handleDisconnect(client: Socket) {
    const userId = client.data?.user?.sub;
    if (userId && this.userSockets.has(userId)) {
      this.userSockets.get(userId)!.delete(client.id);
      if (this.userSockets.get(userId)!.size === 0) {
        this.userSockets.delete(userId);
      }
    }
    this.logger.log(`Socket disconnected: ${client.id}`);
  }

  @SubscribeMessage('join_group')
  handleJoinGroup(client: Socket, groupId: string) {
    client.join(`group:${groupId}`);
    this.logger.log(`Client ${client.id} joined group:${groupId}`);
    return { status: 'joined', groupId };
  }

  @SubscribeMessage('leave_group')
  handleLeaveGroup(client: Socket, groupId: string) {
    client.leave(`group:${groupId}`);
    this.logger.log(`Client ${client.id} left group:${groupId}`);
    return { status: 'left', groupId };
  }

  broadcastLocationUpdate(groupId: string, locationData: any) {
    if (this.server) {
      this.server.to(`group:${groupId}`).emit('location_update', locationData);
    }
  }

  broadcastSharingStatus(groupId: string, statusData: any) {
    if (this.server) {
      this.server.to(`group:${groupId}`).emit('sharing_status_update', statusData);
    }
  }
}
