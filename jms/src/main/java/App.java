import jakarta.jms.*;
import org.apache.activemq.ActiveMQConnectionFactory;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class App {

    private static final String DESTINATION_NAME = "my-jms-topic";
    private static final String MESSAGE_PATH = "src/main/resources/message.txt";
    private static final String ACTIVEMQ_URL = "tcp://localhost:61616";
    private static final int NUMBER_OF_MESSAGES = 10000;

    private static final TimeStamper timeStamper = new TimeStamper();

    private static void produce(Session session, Destination destination) throws Exception {
        // Read message from file
        byte[] message = Files.readAllBytes(Paths.get(MESSAGE_PATH));
        message = timeStamper.addTimeStamp(message);
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

        // List containing latencies for each message
        List<Long> latencies = new ArrayList<>();

        // Consume messages
        while (receivedMessages < NUMBER_OF_MESSAGES) {
            long beforeReceive = System.currentTimeMillis();
            Message message = consumer.receive();
            long afterReceive = System.currentTimeMillis();

            totalResponseTime += afterReceive - beforeReceive;

            int messageLength = (int) ((BytesMessage) message).getBodyLength();
            byte[] messageBytes = new byte[messageLength];
            ((BytesMessage) message).readBytes(messageBytes);

            latencies.add(afterReceive - timeStamper.extractTimeStamp(messageBytes));

            ++receivedMessages;
        }

        Collections.sort(latencies);

        System.out.println("Average response time for consumer: " + (double) totalResponseTime / NUMBER_OF_MESSAGES + " ms");
        System.out.println("Median latency: " + latencies.get(latencies.size() / 2) + " ms");

        consumer.close();
    }

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
}
