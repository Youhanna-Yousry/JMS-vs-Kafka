import jakarta.jms.*;
import org.apache.activemq.ActiveMQConnectionFactory;

import java.nio.file.Files;
import java.nio.file.Paths;


public class App {

    private static final String DESTINATION_NAME = "my-jms-topic"; // Update with desired queue/topic name
    private static final String MESSAGE_PATH = "src/main/resources/message.txt";
    private static final String ACTIVEMQ_URL = "tcp://localhost:61616"; // Update with your ActiveMQ broker URL
    private static final int NUMBER_OF_MESSAGES = 10000;

    public static void main(String[] args) throws Exception {
        // Create a connection to the ActiveMQ broker
        ConnectionFactory connectionFactory = new ActiveMQConnectionFactory(ACTIVEMQ_URL);
        Connection connection = connectionFactory.createConnection();
        connection.start();

        // Create a session
        Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);

        // Create the JMS destination (queue or topic)
        Destination destination = session.createTopic(DESTINATION_NAME);

        // Producer (run in a separate thread for asynchronous behavior)
        Thread producerThread = new Thread(() -> {
            try {
                produce(session, destination);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        // Consumer (run in a separate thread for asynchronous behavior)
        Thread consumerThread = new Thread(() -> {
            try {
                consume(session, destination);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        // Start producer and consumer threads
        producerThread.start();
        consumerThread.start();

        // Wait for producer and consumer threads to finish
        producerThread.join();
        consumerThread.join();

        // Close resources
        session.close();
        connection.close();
    }

    private static void produce(Session session, Destination destination) throws Exception {
        // Read message from file
        byte[] message = Files.readAllBytes(Paths.get(MESSAGE_PATH));
        BytesMessage bytesMessage = session.createBytesMessage();
        bytesMessage.writeBytes(message);

        // Create a message producer
        MessageProducer producer = session.createProducer(destination);

        // Total time taken to send all messages
        long totalResponseTime = 0;

        // send messages to the consumer such that its count = NUMBER_OF_MESSAGES
        for (int i = 0; i < NUMBER_OF_MESSAGES; i++) {
            long beforeSend = System.currentTimeMillis();
            producer.send(bytesMessage);
            totalResponseTime += System.currentTimeMillis() - beforeSend;
        }

        System.out.println("Average response time for producer: " + (double) totalResponseTime / NUMBER_OF_MESSAGES + " ms");

        producer.close();
    }

    private static void consume(Session session, Destination destination) throws Exception {
        // Create a message consumer
        MessageConsumer consumer = session.createConsumer(destination);

        // Count of received messages
        int receivedMessages = 0;

        // Total time taken to receive all messages
        long totalResponseTime = 0;

        // Consume messages
        while (receivedMessages < NUMBER_OF_MESSAGES) {
            long beforeReceive = System.currentTimeMillis();
            Message message = consumer.receive();
            totalResponseTime += System.currentTimeMillis() - beforeReceive;
            if (!(message instanceof BytesMessage)) {
                throw new IllegalArgumentException("Expected BytesMessage");
            }
            ++receivedMessages;
        }

        System.out.println("Average response time for consumer: " + (double) totalResponseTime / NUMBER_OF_MESSAGES + " ms");

        consumer.close();
    }
}
