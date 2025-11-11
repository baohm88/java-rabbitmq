package com.example.crawlerwithq;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.nio.charset.StandardCharsets;
import java.util.HashSet;

public class CrawlerProducer {
    // Đặt tên queue là một hằng số để dễ dàng sử dụng lại và tránh gõ sai
    private final static String QUEUE_NAME = "hello-queue";

    public static void main(String[] argv) throws Exception {
        // 1. Tạo một ConnectionFactory
        //    ConnectionFactory là lớp giúp tạo kết nối đến RabbitMQ server.
        ConnectionFactory factory = new ConnectionFactory();

        // Cấu hình thông tin kết nối. Mặc định là localhost, port 5672, user/pass là guest/guest
        factory.setHost("localhost");
        // factory.setPort(5672);
        // factory.setUsername("guest");
        // factory.setPassword("guest");

        // 2. Tạo kết nối và channel
        //    Sử dụng try-with-resources để đảm bảo kết nối (Connection) và channel
        //    sẽ tự động được đóng lại sau khi sử dụng xong, kể cả khi có lỗi xảy ra.
        try (Connection connection = factory.newConnection();
             Channel channel = connection.createChannel()) {

            // 3. Khai báo một queue
            //    channel.queueDeclare là một thao tác idempotent, nghĩa là:
            //    - Nếu queue chưa tồn tại, nó sẽ được tạo.
            //    - Nếu queue đã tồn tại, lệnh này sẽ không làm gì cả.
            //    Điều này đảm bảo queue luôn tồn tại trước khi chúng ta gửi tin nhắn.
            //    - durable = false: queue sẽ bị mất nếu RabbitMQ server khởi động lại.
            //    - exclusive = false: queue có thể được truy cập bởi nhiều kết nối.
            //    - autoDelete = false: queue sẽ không bị xóa khi consumer cuối cùng ngắt kết nối.
            //    - arguments = null: không có tham số đặc biệt nào.
            channel.queueDeclare(QUEUE_NAME, false, false, false, null);
            String message = "Hello World!";
            Document document = Jsoup.connect("https://vnexpress.net/the-thao").get();
            Elements elements = document.select("h3.title-news a[href]");
            HashSet<String> links = new HashSet<>();
            for (Element element : elements) {
                String href = element.attr("href");
                if(href.contains("vnexpress.net")){
                    links.add(href);
                }
            }
            for (String link : links) {
                channel.basicPublish("", QUEUE_NAME, null, link.getBytes(StandardCharsets.UTF_8));
            }
        }
    }
}