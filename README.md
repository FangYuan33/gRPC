## 深入浅出 gRPC

gRPC 是一个高性能、开源和通用的 RPC 框架，最初由 Google 开发。它使用 HTTP/2 协议作为传输层，并支持多种编程语言。gRPC 允许客户端和服务器之间进行高效的通信，特别适合微服务架构。在 gRPC 中，服务定义是通过 **Protocol Buffers（protobuf）** 来描述的。本篇文章以用 Java 编写 RPC 接口为例，介绍 gRPC 相关的基本概念和使用方法：既可以简单地了解到 gRPC 的使用，也可以深入细节中熟悉更多相关的内容，代码示例参考 [grpc-java-example](https://github.com/FangYuan33/gRPC)。

### protobuf 定义

protobuf 是一种语言无关、平台无关、可扩展的结构化数据序列化机制，它类似于 JSON 或 XML，但更高效、更紧凑。protobuf 通过定义服务和消息类型，gRPC 可以自动生成客户端和服务器端的代码。以下是一个简单的 protobuf 定义示例：

```protobuf
syntax = "proto3";

option java_multiple_files = true;
option java_package = "com.grpc.helloworld";
option java_outer_classname = "HelloWorldProto";
option objc_class_prefix = "HLW";

package com.grpc.helloworld;

// The greeting service definition.
service Greeter {
  // Sends a greeting
  rpc SayHello (HelloRequest) returns (HelloReply) {}
}

// The request message containing the user's name.
message HelloRequest {
  string name = 1;
}

// The response message containing the greetings
message HelloReply {
  string message = 1;
}
```

根据以上 protobuf 定义，我们可以看到一个简单的 gRPC 服务 `Greeter`，它包含一个方法 `SayHello`，该方法接受一个 `HelloRequest` 消息并返回一个 `HelloReply` 消息。`HelloRequest` 包含一个字符串字段 `name`，而 `HelloReply` 包含一个字符串字段 `message`。

当然，protobuf 还支持定义更复杂的类型和服务，包括流式 RPC、嵌套消息、枚举等，如下所示：

1. **枚举类型 (Enum)**

```protobuf
enum UserStatus {
  // 枚举的第一个值必须是0
  UNKNOWN = 0;     
  ACTIVE = 1;
  INACTIVE = 2;
}
```

枚举用于定义一组预定义的常量值，第一个枚举值必须是 0（作为默认值），可以嵌套在消息内部定义。

2. **重复字段 (Repeated Fields)**

```protobuf
// 相当于 List<PhoneNumber>
repeated PhoneNumber phones = 5;      
// 相当于 List<String>
repeated string tags = 6;             
```

`repeated` 关键字表示该字段可以重复出现多次，在 Java 中生成为 `List<T>` 类型，相当于数组或列表。

3. **映射类型 (Map)**

```protobuf
// 键值对映射
map<string, string> metadata = 9;     
map<string, int32> scores = 10;
```

`map<K, V>` 语法定义键值对映射，键的类型可以是除了浮点数和 bytes 之外的任何标量类型，值可以是任何类型（标量、消息、枚举）。

4. **可选字段 (Optional Fields)**

```protobuf
message User {
  // 隐式存在语义
  string name = 1;              
  // 显式存在语义
  optional string email = 2;    
}
```

在 proto3 中，使用 `optional` 关键字明确标记可选字段，**提供显式的字段存在检查，区别于默认的隐式存在语义**。

**在 Java 中的表现**：

```java
// 隐式存在语义的字段
User user = User.newBuilder().build();
// 输出: ""（空字符串，默认值）
System.out.println(user.getName());
// 无法区分是用户主动设置为空字符串，还是没有设置

// 显式存在语义的字段，输出: false（明确知道没有设置）
System.out.println(user.hasEmail());       
User userWithEmail = User.newBuilder().setEmail("").build();
// 输出: true（明确知道设置了，即使是空值）
System.out.println(userWithEmail.hasEmail()); 
```

5. **Oneof 字段**

```protobuf
oneof sort_criteria {
  string sort_by_field = 4;
  Priority sort_by_priority = 5;
}
```

`oneof` 表示同时只能设置其中一个字段，类似于 union 类型，节省内存和网络带宽。

6. **嵌套消息**

```protobuf
message User {
  Profile profile = 14;
  
  // 嵌套定义
  message Profile {    
    string bio = 1;
    int32 age = 2;
  }
}
```

消息可以嵌套定义在其他消息内部，提供更好的代码组织和命名空间隔离。

7. **Well-Known Types**

```protobuf
google.protobuf.Timestamp created_at = 12;
google.protobuf.Any payload = 2;
```

Well-Known Types 是 Protocol Buffers 提供的一组标准类型，包含常用的时间戳、持续时间、任意类型消息等。这些类型可以直接使用，无需重新定义，包含：

- `Timestamp`: 时间戳
- `Duration`: 时间间隔
- `Any`: 可以包含任意类型的消息
- `Empty`: 空消息

8. **流式 RPC 类型**

```protobuf
service UserService {
  // 服务器流式 - 一个请求，多个响应
  rpc ListUsers(ListUsersRequest) returns (stream User);
  
  // 客户端流式 - 多个请求，一个响应  
  rpc BatchCreateUsers(stream CreateUserRequest) returns (BatchOperationResponse);
  
  // 双向流式 - 多个请求，多个响应
  rpc ChatWithUsers(stream ChatMessage) returns (stream ChatMessage);
}
```

流式 RPC 允许客户端和服务器之间进行多次消息交换，支持以上注释中标记的三种类型。

**定义 protobuf 注意**：

1. 字段名使用 `snake_case`；消息名使用 `PascalCase`；服务名使用 `PascalCase`；枚举值使用 `UPPER_SNAKE_CASE`
2. **字段编号 1-15**：使用 1 个字节编码（推荐用于最常用字段）
3. **字段编号 16-2047**：使用 2 个字节编码
4. **字段编号不需要连续**：可以跳号，如 `1, 3, 5, 100` 都是合法的
5. **保留编号范围**：19000-19999 为 Protocol Buffers 内部保留，不能使用
6. **字段编号不能重复使用**：一旦使用过的编号，即使删除字段也不能再次使用

**那么为什么删除字段后要保留编号呢？** 因为要保留向后兼容性，确保旧版本客户端仍能与新版本服务器正常通信：

```protobuf
// 版本 1
message User {
  string name = 1;
  // 后来要删除的字段
  string email = 2;    
  int32 age = 3;
}

// 版本 2 - 错误的做法
message User {
  string name = 1;
  // 删除了 email 字段
  int32 age = 3;
  // ❌ 重用了编号2，这会导致问题！
  string phone = 2;    
}
```

旧版本客户端发送包含 `email`（编号2，string类型）的消息，而新版本服务器按 `phone`（编号2，string类型）解析，虽然类型相同，但语义完全不同，导致数据混乱。正确的做法是 **保留已删除字段的编号，禁止重用**：

```protobuf
// 版本 2 - 正确的做法
message User {
  string name = 1;
  // 保留编号2，禁止重用
  reserved 2;          
   // 也可以按字段名保留
  // 或者 reserved "email"; 
  int32 age = 3;
  // ✅ 使用新的编号
  string phone = 4;
}
```

> `reserved` 关键字用于保留编号或字段名，禁止重用，确保向后兼容性。

### 编译 protobuf

第一小节中简单的 protobuf 定义保存为 `helloworld.proto` 文件，并保存在 `src/main/proto` 目录下，借助 Maven 的 `protobuf-maven-plugin` 插件，我们可以自动编译 protobuf 文件为 Java 代码：

```xml
<build>
    <extensions>
        <extension>
            <groupId>kr.motd.maven</groupId>
            <artifactId>os-maven-plugin</artifactId>
            <version>1.7.1</version>
        </extension>
    </extensions>
    <plugins>
        <plugin>
            <groupId>org.xolstice.maven.plugins</groupId>
            <artifactId>protobuf-maven-plugin</artifactId>
            <version>0.6.1</version>
            <configuration>
                <protocArtifact>com.google.protobuf:protoc:3.25.5:exe:${os.detected.classifier}</protocArtifact>
                <pluginId>grpc-java</pluginId>
                <pluginArtifact>io.grpc:protoc-gen-grpc-java:1.74.0:exe:${os.detected.classifier}</pluginArtifact>
            </configuration>
            <executions>
                <execution>
                    <goals>
                        <goal>compile</goal>
                        <goal>compile-custom</goal>
                    </goals>
                </execution>
            </executions>
        </plugin>
    </plugins>
</build>
```

在 `pom.xml` 中添加以上配置后，运行以下命令即可编译 protobuf 文件：

```bash
mvn clean compile
```

编译完成后可以在 `target/generated-sources/protobuf/java` 目录下找到生成的 Java 代码 `com/grpc/helloworld/GreeterGrpc.java`。

### 搭建 gRPC 服务端和客户端

在 `GreeterGrpc` 类中可以找到 `com.grpc.helloworld.GreeterGrpc.GreeterImplBase` 类，它是 `Greeter` 服务的实现基类，包含了 `SayHello` 方法的定义，我们可以继承这个类来实现具体的业务逻辑：

```java
    private static class GreeterImpl extends GreeterGrpc.GreeterImplBase {
        @Override
        public void sayHello(HelloRequest request, StreamObserver<HelloReply> responseObserver) {
            String name = request.getName();
            logger.info("收到问候请求，姓名: " + name);

            // 创建响应
            HelloReply reply = HelloReply.newBuilder()
                    .setMessage("Hello, " + name + "!")
                    .build();

            // 发送响应
            responseObserver.onNext(reply);
            responseObserver.onCompleted();

            logger.info("已发送问候响应: " + reply.getMessage());
        }
    }
```

接口业务逻辑实现完成后，服务端需要启动 gRPC 服务器来监听客户端请求。我们可以使用 `ServerBuilder` 来构建和启动服务器：

```java
    private static void runServer() throws IOException {
        // 创建 gRPC 服务器
        Server server = ServerBuilder.forPort(50051)
                // 添加服务实现
                .addService(new GreeterImpl()) 
                .build()
                .start();

        logger.info("gRPC 服务器已启动，监听端口: " + server.getPort());

        // 添加钩子以优雅关闭服务器
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("正在关闭 gRPC 服务器...");
            server.shutdown();
            logger.info("gRPC 服务器已关闭");
        }));

        // 阻塞等待服务器关闭
        server.awaitTermination();
    }
```

这样服务端启动完成后，客户端就可以调用这个服务了。客户端调用这个服务的逻辑更加简单，我们可以使用 `GreeterGrpc.newBlockingStub(channel)` 创建一个阻塞式的客户端存根（stub），然后调用 `sayHello` 方法：

```java
    private static void runClient() {
        // 创建 gRPC 通道
        ManagedChannel channel = ManagedChannelBuilder.forAddress("localhost", 50051)
                .usePlaintext()
                .build();

        // 创建客户端存根
        GreeterGrpc.GreeterBlockingStub stub = GreeterGrpc.newBlockingStub(channel);

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
```

现在我们已经完成了 gRPC 服务的基本实现，那接下来让我们想一想“为什么要使用 gRPC 呢”：

在前文中我们已经了解到了 protobuf 协议的优点，像“体积小、解析快、平台语言无关和类型安全”等等，因为 gRPC 基于 protobuf 协议，所以它也继承了这些优点。除此之外，gRPC 基于 **HTTP/2 协议**，它相比于 HTTP/1.1 更加高效，感兴趣的同学可以简单参考下 [菜鸟教程 - HTTP/2 协议](https://www.runoob.com/http/http2-tutorial.html) 内容。因为 gRPC 的性能优势，它也就非常适合用于 **微服务架构中的服务间通信**。在官方文档中有如下图示：

![](image.png)

如果团队中基于不同的编程语言开发微服务，那么 gRPC 跨语言调用的特点也能派上用场了。

### Nacos 对 gRPC 的使用



