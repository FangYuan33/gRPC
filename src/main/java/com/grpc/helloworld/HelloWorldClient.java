package com.grpc.helloworld;

import io.grpc.Channel;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;

import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * gRPC Hello World 客户端
 */
public class HelloWorldClient {
    private static final Logger logger = Logger.getLogger(HelloWorldClient.class.getName());

    private final GreeterGrpc.GreeterBlockingStub blockingStub;

    /**
     * 构造一个连接到服务器的客户端
     */
    public HelloWorldClient(Channel channel) {
        // 传入一个通道，而不是通道构建器，因为客户端代码可以重用该通道
        // 这里我们传入通道构建器，这样客户端代码就知道如何管理它
        blockingStub = GreeterGrpc.newBlockingStub(channel);
    }

    /**
     * 向服务器问好
     */
    public void greet(String name) {
        logger.info("向服务器发送问候请求，姓名: " + name + " ...");
        
        HelloRequest request = HelloRequest.newBuilder().setName(name).build();
        HelloReply response;
        
        try {
            response = blockingStub.sayHello(request);
        } catch (StatusRuntimeException e) {
            logger.log(Level.WARNING, "RPC 调用失败: {0}", e.getStatus());
            return;
        }
        
        logger.info("服务器响应: " + response.getMessage());
    }

    /**
     * Greet服务器，如果提供了有效的名称作为命令行参数；
     * 否则，问候"world"。
     */
    public static void main(String[] args) throws Exception {
        // 访问服务器的主机和端口
        String target = "localhost:50051";
        
        // 创建一个通信通道到服务器，称为Channel。通道是线程安全的
        // 并且可以重复使用。通常在应用程序开始时创建通道并重复使用
        // 直到应用程序关闭。
        ManagedChannel channel = ManagedChannelBuilder.forTarget(target)
                // 没有TLS的通道。为了在生产环境中使用，TLS应该
                // 用NettyChannelBuilder或GrpcSslContexts配置。
                .usePlaintext()
                .build();
        
        try {
            HelloWorldClient client = new HelloWorldClient(channel);
            
            // 测试不同的名称
            String[] testNames = {"小明", "小红", "World", "gRPC学习者"};
            
            for (String name : testNames) {
                client.greet(name);
                // 稍微延迟，让日志更清晰
                Thread.sleep(1000);
            }
            
        } finally {
            // ManagedChannel在不再需要时使用shutdown()关闭，否则可能泄漏资源。
            channel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
        }
    }
}
