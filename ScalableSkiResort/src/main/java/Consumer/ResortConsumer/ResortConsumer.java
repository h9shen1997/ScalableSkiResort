package Consumer.ResortConsumer;

import Consumer.LiftRideConsumer.LiftRideConsumer;
import Database.LiftRideModel.LiftRide;
import Database.ResortDAO.ResortDAORedis;
import com.google.gson.Gson;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ResortConsumer {

  private static final String RESORT_QUEUE_NAME = "ski_queue";
  private static final int NUM_OF_CONSUMER_THREAD = 100;
  private static final Gson gson = new Gson();

  public static void main(String[] args) throws Exception {
    ConnectionFactory factory = new ConnectionFactory();
    factory.setHost("172.31.4.81");
    factory.setUsername("admin");
    factory.setPassword("rabbitmq123");

    ResortDAORedis resortDAO = new ResortDAORedis();

    final Connection connection = factory.newConnection();

    Runnable consumerThread = () -> {
      try {
        Channel channel = connection.createChannel();
        channel.queueDeclare(RESORT_QUEUE_NAME, true, false, false, null);
        channel.basicQos(20);
        System.out.println(" [*] Thread waiting for messages. To exit press CTRL+C");

        DeliverCallback deliverCallback = (consumerTag, delivery) -> {
          String payload = new String(delivery.getBody(), StandardCharsets.UTF_8);
          LiftRide liftRide = gson.fromJson(payload, LiftRide.class);
          resortDAO.createResortInfo(liftRide);
          channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
        };
        channel.basicConsume(RESORT_QUEUE_NAME, false, deliverCallback, consumerTag -> {
        });
      } catch (IOException e) {
        Logger.getLogger(LiftRideConsumer.class.getName()).log(Level.SEVERE, null, e);
      }
    };

    for (int i = 0; i < NUM_OF_CONSUMER_THREAD; i++) {
      new Thread(consumerThread).start();
    }
  }

}
