package com.grpc.helloworld;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * gRPC Hello World 服务器
 */
public class HelloWorldServer {
    private static final Logger logger = Logger.getLogger(HelloWorldServer.class.getName());

    private Server server;

    /**
     * 启动服务器
     */
    private void start() throws IOException {
        /* 监听端口为 50051 */
        int port = 50051;
        server = ServerBuilder.forPort(port)
                .addService(new GreeterImpl())
                .build()
                .start();
        
        logger.info("服务器已启动，监听端口: " + port);
        
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            // 在JVM关闭时使用stderr，因为logger可能已经被关闭
            System.err.println("*** 正在关闭gRPC服务器，因为JVM正在关闭");
            try {
                HelloWorldServer.this.stop();
            } catch (InterruptedException e) {
                e.printStackTrace(System.err);
            }
            System.err.println("*** 服务器已关闭");
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
     * 等待服务器关闭，直到外部关闭
     */
    private void blockUntilShutdown() throws InterruptedException {
        if (server != null) {
            server.awaitTermination();
        }
    }

    private static class GreeterImpl extends GreeterGrpc.GreeterImplBase {
        @Override
        public void sayHello(HelloRequest request, StreamObserver<HelloReply> responseObserver) {
            try {
                String name = request.getName();
                logger.info("收到问候请求，姓名: " + name);

                // 创建响应
                HelloReply reply = HelloReply.newBuilder()
                        .setMessage("Hello, " + name + "!")
                        .build();

                // 发送一条响应
                responseObserver.onNext(reply);
                // 完成响应
                responseObserver.onCompleted();

                logger.info("已发送问候响应: " + reply.getMessage());
            } catch (Exception e) {
                // 发送异常响应
                responseObserver.onError(Status.INTERNAL
                        .withDescription("服务器内部错误")
                        .withCause(e)
                        .asRuntimeException());
            }
        }
    }

    /**
     * 主方法：启动服务器
     */
    public static void main(String[] args) throws IOException, InterruptedException {
        final HelloWorldServer server = new HelloWorldServer();
        server.start();
        server.blockUntilShutdown();
    }
}
