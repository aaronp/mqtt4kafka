package mqtt4kafka

import org.apache.kafka.clients.consumer.{ConsumerRecord, ConsumerRecords, KafkaConsumer}

import scala.concurrent.duration.FiniteDuration

class RichKafkaConsumer[K, V](client: KafkaConsumer[K, V]) {

  def pull(timeout: FiniteDuration): Iterator[ConsumerRecord[K, V]] = {
    val all: Iterator[Iterable[ConsumerRecord[K, V]]] = Iterator.continually(pullOrEmpty(timeout))
    all.flatten
  }

  def pullOrEmpty(timeout: FiniteDuration): Iterable[ConsumerRecord[K, V]] = {
    import scala.collection.JavaConverters._
    val records: ConsumerRecords[K, V] = client.poll(java.time.Duration.ofMillis(timeout.toMillis))
    // we need to seek to the beginning -- which we can only do after the first poll
    if (records.count() > 0) {
      records.asScala
    } else {
      Nil
    }
  }

}

object RichKafkaConsumer {

  object implicits {
    implicit def asRichConsumer[K, V](client: KafkaConsumer[K, V]) = new RichKafkaConsumer[K, V](client)
  }

}