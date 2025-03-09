# Messaging Performance Benchmark
This repository benchmarks message latency using ActiveMQ (JMS) and Kafka. It sends 10,000 messages via a producer/consumer running on different threads and calculates latency for each.


## 📦 Packages
jms - Implements messaging with ActiveMQ.
kafka - Implements messaging with Kafka.


## 🚀 How It Works
A producer sends 10,000 messages.
A consumer receives messages on a separate thread.
Latency is measured for each message.
