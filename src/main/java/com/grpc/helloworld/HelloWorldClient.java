package com.grpc.helloworld;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

/**
 * gRPC Hello World 客户端
 */
public class HelloWorldClient {

    public static void main(String[] args) {
        // 创建 gRPC 通道
        ManagedChannel channel = ManagedChannelBuilder.forAddress("localhost", 50051)
                .usePlaintext()
                .build();

        // 创建客户端存根
        GreeterGrpc.GreeterBlockingStub stub = GreeterGrpc.newBlockingStub(channel);
        // 创建异步客户端存根
        // GreeterGrpc.GreeterStub stub = GreeterGrpc.newStub(channel);
        // 创建支持 Future 的异步客户端存根
        // GreeterGrpc.GreeterStub stub = GreeterGrpc.newFutureStub(channel);

        // 创建请求
        HelloRequest request = HelloRequest.newBuilder()
                .setName("World")
                .build();

        // 调用服务
        HelloReply response = stub.sayHello(request);

        // 输出响应
        System.out.println("收到响应: " + response.getMessage());

        // 关闭通道
        channel.shutdown();
    }
}
