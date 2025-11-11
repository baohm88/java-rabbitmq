Hệ thống thương mại điện tử, xử lý khi người dùng đặt hàng:

1.  **Lưu thông tin đơn hàng vào database.**
2.  **Gửi email xác nhận đơn hàng cho khách hàng.**
3.  **Cập nhật số lượng sản phẩm trong kho.**

Thay vì thực hiện các tác vụ này đồng bộ, chúng ta sẽ sử dụng RabbitMQ để xử lý chúng một cách bất đồng bộ, giúp giảm tải cho ứng dụng chính và cải thiện trải nghiệm người dùng.

**1. Mô Tả Luồng Xử Lý:**

*   **Web Application (Producer):** Khi người dùng đặt hàng, Web Application sẽ tạo một message chứa thông tin đơn hàng và gửi nó đến RabbitMQ exchange.
*   **Order Service (Consumer 1):** Nhận message từ RabbitMQ, lưu thông tin đơn hàng vào database.
*   **Email Service (Consumer 2):** Nhận message từ RabbitMQ, gửi email xác nhận đơn hàng cho khách hàng.
*   **Inventory Service (Consumer 3):** Nhận message từ RabbitMQ, cập nhật số lượng sản phẩm trong kho.

**2. Cấu Hình RabbitMQ:**

*   **Exchange:** `order.exchange` (topic exchange)
*   **Queue:**
    *   `order.create.queue` (dành cho Order Service)
    *   `order.email.queue` (dành cho Email Service)
    *   `order.inventory.queue` (dành cho Inventory Service)
*   **Routing Keys:**
    *   `order.created` (cho cả 3 queue)
* **Binding:**
    * Tất cả queue trên bind vào exchange `order.exchange` với routing key là `order.created`.

**3. Code Implementation:**

**a) Order Service (Consumer 1):**

```java
package com.example.ecommerce.orderservice;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final ObjectMapper objectMapper;

    public OrderService(OrderRepository orderRepository, ObjectMapper objectMapper) {
        this.orderRepository = orderRepository;
        this.objectMapper = objectMapper;
    }

    @RabbitListener(queues = "order.create.queue")
    public void receiveOrder(String message) throws IOException {
        System.out.println("Order Service Received: " + message);
        Order order = objectMapper.readValue(message, Order.class); // Chuyển message JSON thành đối tượng Order
        orderRepository.save(order);
        System.out.println("Order saved to database: " + order.getId());
    }
}

```

*   **`OrderService`**: Nhận message chứa thông tin đơn hàng.
*   **`ObjectMapper`**: Sử dụng để chuyển đổi message JSON thành đối tượng `Order`.
*   **`orderRepository.save(order)`**: Lưu thông tin đơn hàng vào database.

**b) Email Service (Consumer 2):**

```java
package com.example.ecommerce.emailservice;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @RabbitListener(queues = "order.email.queue")
    public void sendConfirmationEmail(String message) {
        System.out.println("Email Service Received: " + message);
        // Logic gửi email xác nhận đơn hàng (sử dụng thông tin trong message)
        System.out.println("Confirmation email sent!");
    }
}
```

*   **`EmailService`**: Nhận message chứa thông tin đơn hàng.
*   **Logic gửi email xác nhận**: Thay thế comment bằng logic gửi email thực tế, sử dụng thông tin trong message.

**c) Inventory Service (Consumer 3):**

```java
package com.example.ecommerce.inventoryservice;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class InventoryService {

    private final InventoryRepository inventoryRepository;
    private final ObjectMapper objectMapper;

    public InventoryService(InventoryRepository inventoryRepository, ObjectMapper objectMapper) {
        this.inventoryRepository = inventoryRepository;
        this.objectMapper = objectMapper;
    }

    @RabbitListener(queues = "order.inventory.queue")
    public void updateInventory(String message) throws IOException {
        System.out.println("Inventory Service Received: " + message);
        Order order = objectMapper.readValue(message, Order.class); // Chuyển message JSON thành đối tượng Order

        // Logic cập nhật số lượng sản phẩm trong kho
        System.out.println("Inventory updated for order: " + order.getId());
    }
}
```

*   **`InventoryService`**: Nhận message chứa thông tin đơn hàng.
*   **Logic cập nhật số lượng**: Thay thế comment bằng logic cập nhật số lượng sản phẩm trong kho.

**d) Web Application (Producer):**

```java
package com.example.ecommerce.webapp;

import com.example.ecommerce.model.Order;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
public class OrderController {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    private final String exchangeName = "order.exchange";
    private final String routingKey = "order.created";

    @PostMapping("/orders")
    public String createOrder(@RequestBody Order order) throws IOException {
        System.out.println("Received order request: " + order.toString());
        ObjectMapper objectMapper = new ObjectMapper();
        String orderJson = objectMapper.writeValueAsString(order);
        rabbitTemplate.convertAndSend(exchangeName, routingKey, orderJson);
        return "Order created successfully! (Message sent to queue)";
    }
}
```

*  **`OrderController`**: Nhận request tạo đơn hàng từ client.
*  **`rabbitTemplate.convertAndSend`**: Gửi message chứa thông tin đơn hàng (ở định dạng JSON) đến RabbitMQ exchange.

**e) RabbitMQ Configuration:**

```java
package com.example.ecommerce.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    @Bean
    public TopicExchange orderExchange() {
        return new TopicExchange("order.exchange");
    }

    @Bean
    public Queue orderCreateQueue() {
        return new Queue("order.create.queue");
    }

    @Bean
    public Queue orderEmailQueue() {
        return new Queue("order.email.queue");
    }

    @Bean
    public Queue orderInventoryQueue() {
        return new Queue("order.inventory.queue");
    }

    @Bean
    public Binding orderCreateBinding(Queue orderCreateQueue, TopicExchange orderExchange) {
        return BindingBuilder.bind(orderCreateQueue).to(orderExchange).with("order.created");
    }

    @Bean
    public Binding orderEmailBinding(Queue orderEmailQueue, TopicExchange orderExchange) {
        return BindingBuilder.bind(orderEmailQueue).to(orderExchange).with("order.created");
    }

    @Bean
    public Binding orderInventoryBinding(Queue orderInventoryQueue, TopicExchange orderExchange) {
        return BindingBuilder.bind(orderInventoryQueue).to(orderExchange).with("order.created");
    }
}
```

**f) Order Model (shared across services):**

```java
package com.example.ecommerce.model;

import java.io.Serializable;

public class Order implements Serializable {

    private Long id;
    private String customerName;
    private String orderDate;
    private Double totalAmount;

    // Constructors, Getters, and Setters
}
```

**g)  Repositories (OrderRepository and InventoryRepository):**

Bạn cần tạo các interface Repository (sử dụng Spring Data JPA hoặc một ORM khác) cho các entity `Order` và `Inventory`.

**4. Chạy Ứng Dụng:**

1.  Khởi động RabbitMQ server.
2.  Khởi động ứng dụng Web Application (Spring Boot).
3.  Khởi động Order Service, Email Service và Inventory Service (Spring Boot).
4.  Gửi request POST đến endpoint `/orders` của Web Application với JSON payload chứa thông tin đơn hàng.

**5. Kiểm Tra:**

*   Kiểm tra log của Order Service để xem đơn hàng đã được lưu vào database thành công.
*   Kiểm tra log của Email Service để xem email xác nhận đã được gửi thành công.
*   Kiểm tra log của Inventory Service để xem số lượng sản phẩm trong kho đã được cập nhật thành công.
*   Sử dụng RabbitMQ Management UI để theo dõi messages và queues.

**Lưu Ý:**

*   Trong ví dụ này, chúng ta sử dụng `TopicExchange` và cùng một `routingKey` cho tất cả các queue. Bạn có thể sử dụng các loại exchange và routing key khác nhau để định tuyến messages đến các queue khác nhau dựa trên các tiêu chí khác nhau.
*   Bạn cần implement logic gửi email thực tế trong `EmailService`.
*   Bạn cần implement logic cập nhật số lượng sản phẩm trong kho trong `InventoryService`.
*   Bạn cần tạo các entities `Order` và `Inventory` và các repositories tương ứng.
*   Cần thêm Jackson dependency để xử lý JSON

**Ưu điểm của cách tiếp cận này:**

*   **Tách rời (Decoupling):** Các services không cần biết về nhau, chỉ cần giao tiếp thông qua RabbitMQ.
*   **Khả năng mở rộng (Scalability):** Dễ dàng thêm nhiều consumers để tăng khả năng xử lý.
*   **Độ tin cậy (Reliability):** Nếu một service gặp sự cố, messages vẫn được lưu trữ trong RabbitMQ và sẽ được xử lý khi service hoạt động trở lại.
*   **Hiệu suất (Performance):** Giảm tải cho ứng dụng chính bằng cách xử lý các tác vụ bất đồng bộ.
