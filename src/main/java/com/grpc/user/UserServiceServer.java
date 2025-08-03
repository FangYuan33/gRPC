package com.grpc.user;

import io.grpc.Server;
import io.grpc.ServerBuilder;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * UserService gRPC 服务器
 */
public class UserServiceServer {
    private static final Logger logger = Logger.getLogger(UserServiceServer.class.getName());
    
    private Server server;
    
    /**
     * 启动服务器
     */
    private void start() throws IOException {
        int port = 50052; // 使用不同的端口，避免与 HelloWorld 服务器冲突
        
        server = ServerBuilder.forPort(port)
                .addService(new UserServiceImpl()) // 添加我们的用户服务实现
                .build()
                .start();
        
        logger.info("UserService 服务器已启动，监听端口: " + port);
        logger.info("支持的 RPC 方法:");
        logger.info("  - GetUser: 获取单个用户信息");
        logger.info("  - CreateUser: 创建新用户");
        logger.info("  - ListUsers: 流式获取用户列表");
        
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.err.println("*** 正在关闭 UserService 服务器，因为JVM正在关闭");
            try {
                UserServiceServer.this.stop();
            } catch (InterruptedException e) {
                e.printStackTrace(System.err);
            }
            System.err.println("*** UserService 服务器已关闭");
        }));
    }
    
    /**
     * 停止服务器
     */
    private void stop() throws InterruptedException {
        if (server != null) {
            server.shutdown().awaitTermination(30, TimeUnit.SECONDS);
        }
    }
    
    /**
     * 等待服务器关闭
     */
    private void blockUntilShutdown() throws InterruptedException {
        if (server != null) {
            server.awaitTermination();
        }
    }
    
    /**
     * 主方法：启动服务器
     */
    public static void main(String[] args) throws IOException, InterruptedException {
        final UserServiceServer server = new UserServiceServer();
        server.start();
        server.blockUntilShutdown();
    }
}
