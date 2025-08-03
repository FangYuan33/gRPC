package com.grpc.user;

import io.grpc.Channel;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * UserService gRPC 客户端
 * 演示如何调用不同类型的 RPC 方法
 */
public class UserServiceClient {
    private static final Logger logger = Logger.getLogger(UserServiceClient.class.getName());
    
    private final UserServiceGrpc.UserServiceBlockingStub blockingStub;
    private final UserServiceGrpc.UserServiceStub asyncStub;
    
    /**
     * 构造客户端
     */
    public UserServiceClient(Channel channel) {
        // 创建阻塞存根（同步调用）
        blockingStub = UserServiceGrpc.newBlockingStub(channel);
        // 创建异步存根（异步调用）
        asyncStub = UserServiceGrpc.newStub(channel);
    }
    
    /**
     * 测试 GetUser RPC（一元 RPC）
     */
    public void testGetUser(long userId) {
        logger.info("=== 测试 GetUser RPC ===");
        logger.info("请求获取用户 ID: " + userId);
        
        GetUserRequest request = GetUserRequest.newBuilder()
                .setUserId(userId)
                .build();
        
        try {
            GetUserResponse response = blockingStub.getUser(request);
            
            if (response.getFound()) {
                User user = response.getUser();
                logger.info("✅ 找到用户:");
                logger.info("  ID: " + user.getId());
                logger.info("  姓名: " + user.getName());
                logger.info("  邮箱: " + user.getEmail());
                logger.info("  年龄: " + user.getAge());
                logger.info("  创建时间: " + new java.util.Date(user.getCreatedAt()));
            } else {
                logger.info("❌ 用户不存在: " + userId);
            }
        } catch (StatusRuntimeException e) {
            logger.log(Level.WARNING, "GetUser RPC 调用失败: {0}", e.getStatus());
        }
        
        System.out.println();
    }
    
    /**
     * 测试 CreateUser RPC（一元 RPC）
     */
    public void testCreateUser(String name, String email, int age) {
        logger.info("=== 测试 CreateUser RPC ===");
        logger.info("创建用户: " + name + ", " + email + ", " + age + "岁");
        
        CreateUserRequest request = CreateUserRequest.newBuilder()
                .setName(name)
                .setEmail(email)
                .setAge(age)
                .build();
        
        try {
            CreateUserResponse response = blockingStub.createUser(request);
            
            if (response.getSuccess()) {
                User user = response.getUser();
                logger.info("✅ 用户创建成功:");
                logger.info("  ID: " + user.getId());
                logger.info("  姓名: " + user.getName());
                logger.info("  邮箱: " + user.getEmail());
                logger.info("  年龄: " + user.getAge());
                logger.info("  创建时间: " + new java.util.Date(user.getCreatedAt()));
            } else {
                logger.info("❌ 用户创建失败: " + response.getMessage());
            }
        } catch (StatusRuntimeException e) {
            logger.log(Level.WARNING, "CreateUser RPC 调用失败: {0}", e.getStatus());
        }
        
        System.out.println();
    }
    
    /**
     * 测试 ListUsers RPC（服务器流式 RPC）
     */
    public void testListUsers(int pageSize) throws InterruptedException {
        logger.info("=== 测试 ListUsers RPC (服务器流式) ===");
        logger.info("请求获取用户列表，页面大小: " + pageSize);
        
        ListUsersRequest request = ListUsersRequest.newBuilder()
                .setPageSize(pageSize)
                .build();
        
        final CountDownLatch finishLatch = new CountDownLatch(1);
        
        StreamObserver<User> responseObserver = new StreamObserver<>() {
            private int count = 0;

            @Override
            public void onNext(User user) {
                count++;
                logger.info("📝 接收到用户 #" + count + ":");
                logger.info("  ID: " + user.getId());
                logger.info("  姓名: " + user.getName());
                logger.info("  邮箱: " + user.getEmail());
                logger.info("  年龄: " + user.getAge());
            }

            @Override
            public void onError(Throwable t) {
                logger.log(Level.WARNING, "ListUsers RPC 调用失败", t);
                finishLatch.countDown();
            }

            @Override
            public void onCompleted() {
                logger.info("✅ ListUsers 完成，共接收到 " + count + " 个用户");
                finishLatch.countDown();
            }
        };
        
        // 发起异步流式调用
        asyncStub.listUsers(request, responseObserver);
        
        // 等待调用完成
        if (!finishLatch.await(30, TimeUnit.SECONDS)) {
            logger.warning("ListUsers RPC 超时");
        }
        
        System.out.println();
    }
    
    /**
     * 主方法：运行客户端测试
     */
    public static void main(String[] args) throws Exception {
        String target = "localhost:50052"; // UserService 服务器端口
        
        ManagedChannel channel = ManagedChannelBuilder.forTarget(target)
                .usePlaintext()
                .build();
        
        try {
            UserServiceClient client = new UserServiceClient(channel);
            
            logger.info("🚀 开始测试 UserService...");
            System.out.println();
            
            // 1. 测试获取存在的用户
            client.testGetUser(1L);
            
            // 2. 测试获取不存在的用户
            client.testGetUser(999L);
            
            // 3. 测试创建用户
            client.testCreateUser("赵六", "zhaoliu@example.com", 35);
            
            // 4. 再次获取新创建的用户
            client.testGetUser(4L);
            
            // 5. 测试流式获取用户列表
            client.testListUsers(5);
            
            logger.info("🎉 所有测试完成！");
            
        } finally {
            channel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
        }
    }
}
