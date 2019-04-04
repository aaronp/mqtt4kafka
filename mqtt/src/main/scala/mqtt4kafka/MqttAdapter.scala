package mqtt4kafka

import java.lang.management.ManagementFactory
import java.util.Properties
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.{ScheduledExecutorService, ScheduledFuture, TimeUnit}

import args4c.implicits._
import com.typesafe.config.Config
import com.typesafe.scalalogging.StrictLogging
import javax.management.ObjectName
import mqtt4kafka.MqttAdapter.KafkaPublisher
import org.apache.kafka.clients.producer.{KafkaProducer, ProducerRecord}
import org.eclipse.paho.client.mqttv3.{IMqttMessageListener, MqttMessage}

import scala.concurrent.duration.FiniteDuration
import scala.sys.ShutdownHookThread
import scala.util.Try


/**
  * An MQTT listener and Kafka publisher
  *
  * @param rootConfig
  * @param name
  * @param scheduler
  */
class MqttAdapter private(rootConfig: Config, name: ObjectName)(implicit scheduler: ScheduledExecutorService) extends IMqttMessageListener with AutoCloseable with MqttAdapterMBean with StrictLogging {

  private val kafkaConfig = rootConfig.getConfig("mqtt4kafka.kafka")
  private var publisher: Option[KafkaPublisher] = None

  private var flushFuture: ScheduledFuture[_] = scheduleFlushes()

  private def defaultFrequency = kafkaConfig.asFiniteDuration("flushEvery")

  private def scheduleFlushes(frequency: FiniteDuration = defaultFrequency) = {
    scheduler.scheduleAtFixedRate(() => flush(), frequency.toMillis, frequency.toMillis, TimeUnit.MILLISECONDS)
  }


  private object Lock

  def currentPublisher(): KafkaPublisher = {
    Lock.synchronized {
      publisher.getOrElse {
        publisher = Option(createPublisher())
        currentPublisher()
      }
    }
  }

  private val topicMapping = TopicMappings(rootConfig)

  private def kafkaProperties(): Properties = {
    val props: Properties = propertiesForConfig(kafkaConfig.withoutPath("flushEvery"))
    propertyOverrides.foldLeft(props) {
      case (p, (k, v)) =>
        p.setProperty(k, v)
        p
    }
  }

  private def createPublisher(): KafkaPublisher = {
    val props = kafkaProperties()
    logger.info(s"Connecting to Kafka using:\n${format(props)}\n")

    val producer: KafkaProducer[String, Bytes] = MqttAdapter.newProducer[String, Bytes](props)
    new KafkaPublisher(producer)
  }

  def send(kafkaTopic: String, key: String, value: Array[Byte]): Unit = {
    currentPublisher().push(kafkaTopic, key, value)
  }

  override def messageArrived(topic: String, message: MqttMessage): Unit = {
    val key = message.getId.toString
    val value = message.getPayload
    val kafkaTopic = topicMapping(topic)
    logger.debug(s"sending key=$key, ${value.length} bytes from mqtt $topic to kafka topic $kafkaTopic")
    send(kafkaTopic, key, value)
  }

  override def close(): Unit = {
    logger.info("Closing")
    Try(ManagementFactory.getPlatformMBeanServer().unregisterMBean(name))

    disconnectFromKafka()
  }

  private var propertyOverrides = Map[String, String]()

  override def setProperty(key: String, value: String): String = {
    propertyOverrides = propertyOverrides.updated(key, value)
    getProperties()
  }

  override def getProperties(): String = format(kafkaProperties())

  def format(all: Properties): String = {
    import scala.collection.JavaConverters._
    all.keySet().asScala.map(_.toString).map { key =>
      val value = all.getProperty(key)
      s"${key} : ${value}"
    }.mkString(";\n")
  }

  override def flush() = {

    Lock.synchronized {
      publisher.fold(false) { p =>
        p.flush()
        true
      }
    }
  }

  override def disconnectFromKafka() = {
    Lock.synchronized {
      publisher.fold(false) { p =>
        Try(p.flush())
        p.close()
        publisher = None
        true
      }
    }
  }

  override def reconnect(): Unit = {
    disconnectFromKafka()
    currentPublisher()
    ()
  }

  override def cleanPropertyOverrides(): String = {
    propertyOverrides = Map.empty
    getProperties()
  }

  override def getConfig(): String = {
    rootConfig.getConfig("mqtt4kafka").summaryEntries().mkString(";\n")
  }

  override def cancelAutoFlush(): Boolean = {
    flushFuture.isCancelled() || flushFuture.cancel(false)
  }

  override def sendTestMessage(topic: String, key: String, message: String) = {
    send(topic, key, message.getBytes("UTF-8"))
  }

  override def setAutoFlush(frequencyInMillis: Int): Boolean = {
    val cancelled = cancelAutoFlush()
    import concurrent.duration._
    val freq = if (frequencyInMillis == 0) frequencyInMillis.millis else defaultFrequency
    flushFuture = scheduleFlushes(freq)
    cancelled
  }
}

object MqttAdapter {

  class KafkaPublisher(producer: KafkaProducer[String, Bytes]) extends StrictLogging with AutoCloseable {
    def push(kafkaTopic: String, key: String, value: Array[Byte]): Unit = {
      dirty = true
      logger.debug(s"Sending ${value.length} bytes to Kafka '$kafkaTopic' w/ key $key")
      producer.send(new ProducerRecord[String, Bytes](kafkaTopic, key, value))
    }

    @volatile private var dirty = false

    def flush(): Unit = {
      if (dirty) {
        logger.trace("flushing")
        dirty = false
        producer.flush()
      } else {
        logger.trace("skipping flush")
      }
    }

    override def close(): Unit = producer.close()
  }

  private val globalId = new AtomicInteger(0)

  private def objectName(): ObjectName = {
    new ObjectName(s"MQTT4Kafka:name=MqttAdapter-${globalId.incrementAndGet}")
  }

  def apply(rootConfig: Config)(implicit scheduler: ScheduledExecutorService): IMqttMessageListener with AutoCloseable = {
    val name = objectName()
    val listener = new MqttAdapter(rootConfig, name)

    ManagementFactory.getPlatformMBeanServer().registerMBean(listener, name)

    ShutdownHookThread {
      listener.close()
    }

    listener
  }

  def newProducer[K, V](properties: Properties): KafkaProducer[K, V] = new KafkaProducer[K, V](properties)


}
