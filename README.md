# Messaging Performance Benchmark
This repository benchmarks message latency using ActiveMQ (JMS) and Kafka. It sends 10,000 messages via a producer/consumer running on different threads and calculates median latency.


## 📦 Packages
- jms - Implements messaging with ActiveMQ.
- kafka - Implements messaging with Kafka.


## 🚀 How It Works
1. A producer sends 10,000 messages.
2. A consumer receives messages on a separate thread.
3. Latency is measured for all messages.
