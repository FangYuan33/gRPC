package com.grpc.user;

import io.grpc.stub.StreamObserver;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;

/**
 * UserService 的服务端实现
 * 
 * 关键点：
 * 1. 继承 UserServiceGrpc.UserServiceImplBase
 * 2. 实现 proto 文件中定义的所有 RPC 方法
 * 3. 每个方法的签名都由 protobuf 编译器自动生成
 */
public class UserServiceImpl extends UserServiceGrpc.UserServiceImplBase {
    
    private static final Logger logger = Logger.getLogger(UserServiceImpl.class.getName());
    
    // 模拟数据库存储
    private final Map<Long, User> userDatabase = new HashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);
    
    public UserServiceImpl() {
        // 初始化一些测试数据
        initTestData();
    }
    
    /**
     * 实现 GetUser RPC 方法
     * <p>
     * 方法签名自动生成：
     * - 参数1: GetUserRequest request - 请求对象
     * - 参数2: StreamObserver<GetUserResponse> responseObserver - 响应观察者
     */
    @Override
    public void getUser(GetUserRequest request, StreamObserver<GetUserResponse> responseObserver) {
        logger.info("收到 GetUser 请求，用户ID: " + request.getUserId());
        
        try {
            long userId = request.getUserId();
            User user = userDatabase.get(userId);
            
            GetUserResponse.Builder responseBuilder = GetUserResponse.newBuilder();
            
            if (user != null) {
                responseBuilder.setUser(user).setFound(true);
                logger.info("找到用户: " + user.getName());
            } else {
                responseBuilder.setFound(false);
                logger.info("用户不存在: " + userId);
            }
            
            // 发送响应
            responseObserver.onNext(responseBuilder.build());
            responseObserver.onCompleted();
            
        } catch (Exception e) {
            logger.severe("GetUser 处理失败: " + e.getMessage());
            responseObserver.onError(e);
        }
    }
    
    /**
     * 实现 CreateUser RPC 方法
     */
    @Override
    public void createUser(CreateUserRequest request, StreamObserver<CreateUserResponse> responseObserver) {
        logger.info("收到 CreateUser 请求，用户名: " + request.getName());
        
        try {
            // 验证输入
            if (request.getName().trim().isEmpty()) {
                CreateUserResponse response = CreateUserResponse.newBuilder()
                    .setSuccess(false)
                    .setMessage("用户名不能为空")
                    .build();
                
                responseObserver.onNext(response);
                responseObserver.onCompleted();
                return;
            }
            
            // 创建新用户
            long newId = idGenerator.getAndIncrement();
            User newUser = User.newBuilder()
                .setId(newId)
                .setName(request.getName())
                .setEmail(request.getEmail())
                .setAge(request.getAge())
                .setCreatedAt(System.currentTimeMillis())
                .build();
            
            // 保存到"数据库"
            userDatabase.put(newId, newUser);
            
            // 构建响应
            CreateUserResponse response = CreateUserResponse.newBuilder()
                .setUser(newUser)
                .setSuccess(true)
                .setMessage("用户创建成功")
                .build();
            
            logger.info("用户创建成功，ID: " + newId + ", 姓名: " + newUser.getName());
            
            responseObserver.onNext(response);
            responseObserver.onCompleted();
            
        } catch (Exception e) {
            logger.severe("CreateUser 处理失败: " + e.getMessage());
            responseObserver.onError(e);
        }
    }
    
    /**
     * 实现 ListUsers RPC 方法（服务器流式）
     * 
     * 注意：这是流式 RPC，可以发送多个响应
     */
    @Override
    public void listUsers(ListUsersRequest request, StreamObserver<User> responseObserver) {
        logger.info("收到 ListUsers 请求，页面大小: " + request.getPageSize());
        
        try {
            int pageSize = request.getPageSize();
            if (pageSize <= 0) {
                pageSize = 10; // 默认页面大小
            }
            
            // 模拟分页逻辑
            int count = 0;
            for (User user : userDatabase.values()) {
                if (count >= pageSize) {
                    break;
                }
                
                // 发送单个用户（流式响应）
                responseObserver.onNext(user);
                count++;
                
                logger.info("发送用户: " + user.getName());
                
                // 模拟一些延迟
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
            
            // 完成流式响应
            responseObserver.onCompleted();
            logger.info("ListUsers 完成，共发送 " + count + " 个用户");
            
        } catch (Exception e) {
            logger.severe("ListUsers 处理失败: " + e.getMessage());
            responseObserver.onError(e);
        }
    }
    
    /**
     * 初始化测试数据
     */
    private void initTestData() {
        User user1 = User.newBuilder()
            .setId(1L)
            .setName("张三")
            .setEmail("zhangsan@example.com")
            .setAge(25)
            .setCreatedAt(System.currentTimeMillis() - 86400000) // 1天前
            .build();
            
        User user2 = User.newBuilder()
            .setId(2L)
            .setName("李四")
            .setEmail("lisi@example.com")
            .setAge(30)
            .setCreatedAt(System.currentTimeMillis() - 172800000) // 2天前
            .build();
            
        User user3 = User.newBuilder()
            .setId(3L)
            .setName("王五")
            .setEmail("wangwu@example.com")
            .setAge(28)
            .setCreatedAt(System.currentTimeMillis() - 259200000) // 3天前
            .build();
        
        userDatabase.put(1L, user1);
        userDatabase.put(2L, user2);
        userDatabase.put(3L, user3);
        idGenerator.set(4L); // 下一个ID从4开始
        
        logger.info("初始化了 " + userDatabase.size() + " 个测试用户");
    }
}
