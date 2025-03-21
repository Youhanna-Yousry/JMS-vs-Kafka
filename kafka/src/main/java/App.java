import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

public class App {

    private static final String TOPIC = "my-kafka-topic";
    private static final String BOOTSTRAP_SERVER = "localhost:9092";
    private static final String MESSAGE_PATH = "src/main/resources/message.txt";
    private static final int NUMBER_OF_MESSAGES = 10000;

    private static void produce() {
        Properties props = new Properties();
        props.put("bootstrap.servers", BOOTSTRAP_SERVER);
        props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        props.put("value.serializer", "org.apache.kafka.common.serialization.ByteArraySerializer");

        // read the message from the file as array of bytes
        byte[] message = null;
        try {
            message = Files.readAllBytes(Paths.get(MESSAGE_PATH));
        } catch (Exception e) {
            System.err.println("Error reading the message from the file with path: " + MESSAGE_PATH);
            System.exit(1);
        }

        long totalResponseTime = 0;

        // create a producer that send the message(key = message id, value = message content) to the topic
        try (Producer<String, byte[]> producer = new KafkaProducer<>(props)) {
            for (int i = 0; i < NUMBER_OF_MESSAGES; i++) {
                long beforeSend = System.nanoTime();
                producer.send(new ProducerRecord<>(TOPIC, null, System.nanoTime(), Integer.toString(i), message));
                producer.flush();
                totalResponseTime += System.nanoTime() - beforeSend;
            }
        }

        System.out.println("Average response time for producer: " + (double) totalResponseTime / NUMBER_OF_MESSAGES / 1e6 + " ms");
    }

    private static void consume() {
        Properties props = new Properties();
        props.put("bootstrap.servers", BOOTSTRAP_SERVER);
        props.put("group.id", "my-kafka-group");
        props.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        props.put("value.deserializer", "org.apache.kafka.common.serialization.ByteArrayDeserializer");
        props.setProperty("enable.auto.commit", "true");
        props.setProperty("auto.commit.interval.ms", "1000");
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 1);
        props.setProperty("auto.offset.reset", "earliest");

        List<Long> latencies = new ArrayList<>();

        int receivedMessages = 0;

        long totalResponseTime = 0;

        // create a consumer that reads the message from the topic
        try (Consumer<String, byte[]> consumer = new KafkaConsumer<>(props)) {
            consumer.subscribe(List.of(TOPIC));
            while (receivedMessages < NUMBER_OF_MESSAGES) {
                long beforePoll = System.nanoTime();
                ConsumerRecords<String, byte[]> records = consumer.poll(Duration.ofMillis(1000));
                receivedMessages += records.count();
                long afterPoll = System.nanoTime();
                for (var record : records) {
                    latencies.add(afterPoll - record.timestamp());
                    totalResponseTime += afterPoll - beforePoll;
                }
            }
        }

        Collections.sort(latencies);

        System.out.println("Average response time for consumer: " + (double) totalResponseTime / NUMBER_OF_MESSAGES / 1e6 + " ms");
        if (latencies.size() % 2 == 0) {
            System.out.println("Median latency: " + (latencies.get(latencies.size() / 2) + latencies.get(latencies.size() / 2 - 1)) / 2 / 1e6 + " ms");
        } else {
            System.out.println("Median latency: " + latencies.get(latencies.size() / 2) / 1e6 + " ms");
        }
    }

    public static void main(String[] args) throws Exception {
        Thread producerThread = new Thread(App::produce);
        Thread consumerThread = new Thread(App::consume);

        // Start producer and consumer threads
        consumerThread.start();
        producerThread.start();

        // Wait for producer and consumer threads to finish
        consumerThread.join();
        producerThread.join();
    }

}
