# Hướng Dẫn Sử Dụng Spring Boot và RabbitMQ

## 1. Chuẩn Bị

*   **Cài đặt RabbitMQ:** Đảm bảo bạn đã cài đặt RabbitMQ và RabbitMQ Management Plugin (như đã hướng dẫn ở trên).
*   **IDE:** Sử dụng một IDE như IntelliJ IDEA, Eclipse hoặc VS Code.
*   **Java JDK:** Cài đặt Java JDK (phiên bản 8 trở lên).
*   **Maven hoặc Gradle:** Sử dụng Maven hoặc Gradle để quản lý dependencies.

## 2. Tạo Project Spring Boot

Sử dụng Spring Initializr ([https://start.spring.io/](https://start.spring.io/)) để tạo một project Spring Boot mới với các dependencies sau:

*   **Spring Web:** Để tạo các REST endpoints (nếu cần).
*   **AMQP:** Để làm việc với RabbitMQ.

**Cấu hình Maven (pom.xml):**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.2.0</version> <!-- Sử dụng phiên bản Spring Boot mới nhất -->
        <relativePath/> <!-- lookup parent from repository -->
    </parent>
    <groupId>com.example</groupId>
    <artifactId>rabbitmq-spring-boot</artifactId>
    <version>0.0.1-SNAPSHOT</version>
    <name>rabbitmq-spring-boot</name>
    <description>Demo Spring Boot with RabbitMQ</description>
    <properties>
        <java.version>17</java.version> <!-- Sử dụng Java 17 trở lên -->
    </properties>
    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-amqp</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.springframework.amqp</groupId>
            <artifactId>spring-rabbit-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>

</project>
```

**Cấu hình Gradle (build.gradle):**

```gradle
plugins {
    id 'java'
    id 'org.springframework.boot' version '3.2.0' // Sử dụng phiên bản Spring Boot mới nhất
    id 'io.spring.dependency-management' version '1.1.4'
}

group = 'com.example'
version = '0.0.1-SNAPSHOT'
sourceCompatibility = '17' // Sử dụng Java 17 trở lên

repositories {
    mavenCentral()
}

dependencies {
    implementation 'org.springframework.boot:spring-boot-starter-amqp'
    implementation 'org.springframework.boot:spring-boot-starter-web'
    testImplementation 'org.springframework.boot:spring-boot-starter-test'
    testImplementation 'org.springframework.amqp:spring-rabbit-test'
}

tasks.named('test') {
    useJUnitPlatform()
}
```

## 3. Cấu Hình Kết Nối RabbitMQ

Thêm các cấu hình sau vào file `application.properties` (hoặc `application.yml`):

```properties
spring.rabbitmq.host=localhost
spring.rabbitmq.port=5672
spring.rabbitmq.username=guest
spring.rabbitmq.password=guest
```

**Giải thích:**

*   `spring.rabbitmq.host`: Địa chỉ của RabbitMQ server.
*   `spring.rabbitmq.port`: Cổng của RabbitMQ server.
*   `spring.rabbitmq.username`: Username để kết nối đến RabbitMQ.
*   `spring.rabbitmq.password`: Password để kết nối đến RabbitMQ.

## 4. Tạo Producer (Publisher)

Tạo một class để gửi messages đến RabbitMQ.

```java
package com.example.rabbitmqspringboot;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class RabbitMQProducer {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    private final String exchange = "myExchange"; // Tên Exchange
    private final String routingKey = "foo.bar.baz"; // Routing Key

    public void sendMessage(String message) {
        System.out.println("Sending message to the queue : " + message);
        rabbitTemplate.convertAndSend(exchange, routingKey, message);
    }
}
```

**Giải thích:**

*   `@Service`: Đánh dấu class là một Spring bean.
*   `@Autowired private RabbitTemplate rabbitTemplate`: Inject `RabbitTemplate` để tương tác với RabbitMQ.
*   `exchange`: Tên của exchange.
*   `routingKey`: Routing key để định tuyến message.
*   `convertAndSend()`: Phương thức để gửi message đến RabbitMQ.

## 5. Tạo Consumer (Receiver)

Tạo một class để nhận messages từ RabbitMQ.

```java
package com.example.rabbitmqspringboot;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

@Service
public class RabbitMQConsumer {

    @RabbitListener(queues = "myQueue")
    public void receiveMessage(String message) {
        System.out.println("Received message : " + message);
        // Xử lý message ở đây (ví dụ: lưu vào database, gửi email,...)
    }
}
```

**Giải thích:**

*   `@Service`: Đánh dấu class là một Spring bean.
*   `@RabbitListener(queues = "myQueue")`: Đánh dấu phương thức là một listener cho queue `myQueue`.
*   `receiveMessage()`: Phương thức được gọi khi có message mới đến queue.

## 6. Cấu Hình RabbitMQ (Configuration)

Tạo một class cấu hình để khai báo exchange, queue và binding.

```java
package com.example.rabbitmqspringboot;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    @Bean
    public Queue queue() {
        return new Queue("myQueue", false); // durable = false
    }

    @Bean
    public TopicExchange exchange() {
        return new TopicExchange("myExchange");
    }

    @Bean
    public Binding binding(Queue queue, TopicExchange exchange) {
        return BindingBuilder.bind(queue).to(exchange).with("foo.bar.#");
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        return new RabbitTemplate(connectionFactory);
    }
}
```

**Giải thích:**

*   `@Configuration`: Đánh dấu class là một class cấu hình Spring.
*   `@Bean Queue queue()`: Khai báo một queue với tên `myQueue`.
    *   `durable = false`:  Queue sẽ bị xóa khi RabbitMQ server restart. Nếu bạn muốn queue tồn tại ngay cả khi restart, hãy đặt `durable = true`.
*   `@Bean TopicExchange exchange()`: Khai báo một topic exchange với tên `myExchange`.
*   `@Bean Binding binding()`: Tạo liên kết giữa queue và exchange với routing key `"foo.bar.#"`.
    *   Điều này có nghĩa là queue `myQueue` sẽ nhận tất cả các messages được gửi đến exchange `myExchange` với routing key bắt đầu bằng `"foo.bar."`.
*    `@Bean RabbitTemplate rabbitTemplate()`: Khai báo một `RabbitTemplate` để gửi messages.

## 7. Sử Dụng Producer

Tạo một REST controller để gửi messages đến RabbitMQ.

```java
package com.example.rabbitmqspringboot;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MessageController {

    @Autowired
    private RabbitMQProducer rabbitMQProducer;

    @GetMapping("/send")
    public String sendMessage(@RequestParam("message") String message){
        rabbitMQProducer.sendMessage(message);
        return "Message sent to the queue successfully";
    }
}
```

**Giải thích:**

*   `@RestController`: Đánh dấu class là một REST controller.
*   `@Autowired private RabbitMQProducer rabbitMQProducer`: Inject `RabbitMQProducer` để gửi messages.
*   `@GetMapping("/send")`: Định nghĩa một GET endpoint tại `/send`.
*   `@RequestParam("message") String message`: Lấy message từ query parameter.
*   `rabbitMQProducer.sendMessage(message)`: Gửi message đến RabbitMQ.

## 8. Chạy Ứng Dụng

1.  **Khởi động RabbitMQ server.**
2.  **Chạy ứng dụng Spring Boot.**
3.  **Truy cập URL sau trong trình duyệt:** `http://localhost:8080/send?message=HelloRabbitMQ`

Bạn sẽ thấy thông báo "Message sent to the queue successfully".  Kiểm tra console của ứng dụng Spring Boot, bạn sẽ thấy message "Received message : HelloRabbitMQ" được in ra.

## 9. Các Cấu Hình Nâng Cao

*   **Message Acknowledgement (ACK):**  Đảm bảo message được xử lý thành công trước khi xóa khỏi queue.
*   **Dead Letter Queue (DLQ):** Chuyển các message không thể xử lý đến một queue riêng.
*   **Message Persistence:** Lưu trữ messages trên đĩa để đảm bảo không bị mất khi RabbitMQ server restart.
*   **Connection Pooling:** Sử dụng connection pooling để cải thiện hiệu suất.
*   **Transactions:** Sử dụng transactions để đảm bảo tính nhất quán của dữ liệu.

**Ví dụ: Cấu Hình ACK và DLQ**

*(Đây chỉ là một ví dụ đơn giản, bạn cần điều chỉnh nó cho phù hợp với ứng dụng của mình)*

1.  **Khai báo DLQ:**

    ```java
    @Bean
    public Queue deadLetterQueue() {
        return new Queue("myDeadLetterQueue", false);
    }
    ```

2.  **Khai báo Exchange cho DLQ (nếu cần):**

    ```java
    @Bean
    public DirectExchange deadLetterExchange() {
        return new DirectExchange("myDeadLetterExchange");
    }
    ```

3.  **Binding DLQ to Exchange (nếu cần):**

    ```java
    @Bean
    public Binding deadLetterBinding(Queue deadLetterQueue, DirectExchange deadLetterExchange) {
        return BindingBuilder.bind(deadLetterQueue).to(deadLetterExchange).with("deadLetterRoutingKey");
    }
    ```

4.  **Cấu hình Queue ban đầu để chuyển message sang DLQ khi có lỗi:**

    ```java
    @Bean
    public Map<String, Object> queueArguments() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-dead-letter-exchange", "myDeadLetterExchange"); // Chuyển đến exchange DLQ
        args.put("x-dead-letter-routing-key", "deadLetterRoutingKey"); // Routing key cho DLQ
        return args;
    }

    @Bean
    public Queue queueWithDLQ() {
        return new Queue("myQueue", false, false, false, queueArguments());
    }
    ```

5.  **Cấu hình `SimpleRabbitListenerContainerFactory` để sử dụng `AcknowledgeMode.MANUAL`:**

    ```java
    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setAcknowledgeMode(AcknowledgeMode.MANUAL); // Quan trọng để ACK thủ công
        return factory;
    }
    ```

6.  **Trong Consumer, ACK message thủ công sau khi xử lý thành công:**

    ```java
    @RabbitListener(queues = "myQueue", containerFactory = "rabbitListenerContainerFactory")
    public void receiveMessage(String message, Channel channel, @Header(AmqpHeaders.DELIVERY_TAG) long tag) throws IOException {
        try {
            System.out.println("Received message : " + message);
            // Xử lý message ở đây
            // ...
            channel.basicAck(tag, false); // ACK message nếu xử lý thành công
        } catch (Exception e) {
            System.err.println("Error processing message: " + e.getMessage());
            channel.basicNack(tag, false, false); // NACK message (chuyển sang DLQ nếu cấu hình)
        }
    }
    ```

## 10. Kết Luận

Spring Boot cung cấp một cách dễ dàng và hiệu quả để tích hợp với RabbitMQ. Với các ví dụ và hướng dẫn trong tài liệu này, bạn có thể bắt đầu xây dựng các ứng dụng mạnh mẽ sử dụng RabbitMQ để xử lý tác vụ bất đồng bộ và giao tiếp giữa các microservices. Hãy nhớ tìm hiểu thêm về các cấu hình nâng cao để tối ưu hóa hiệu suất và độ tin cậy của ứng dụng của bạn. Chúc bạn thành công!
