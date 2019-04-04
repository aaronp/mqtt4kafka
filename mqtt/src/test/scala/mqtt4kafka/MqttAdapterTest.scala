package mqtt4kafka

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import args4c.implicits._
import com.typesafe.config.{Config, ConfigFactory}
import org.apache.kafka.clients.consumer.{ConsumerRecord, KafkaConsumer}
import org.eclipse.paho.client.mqttv3.MqttMessage

import scala.collection.JavaConverters._
import scala.util.Try

/**
  * The 'main' test -- mixing in other test traits gives us the ability to have a 'before/after all' step which can
  * apply to ALL our tests, and so we don't stop/start e.g. kafka container for each test suite
  */
class MqttAdapterTest extends BaseDockerSpec("scripts/kafka") {

  import MqttAdapterTest._

  "Main.run" should {
    "consume kafka messages received by the MQTT broker" in {
      Given("An MQTT republishing broker")

      DockerEnv("scripts/mqtt").bracket {


        val topic = randomString()
        val testConfig = Array(s"mqtt4kafka.mqtt.topics=$topic", s"mqtt4kafka.mqtt.clientId=${randomString}").asConfig(Main.defaultConfig())
        testConfig.asList("mqtt4kafka.mqtt.topics") should contain only (topic)

        // verify MQTT broker is up/connectable
        And("A connected client")
        Using(Main.run(testConfig)) { _ =>

          When("We're listening to kafka")
          val kafkaClient = newKafkaConsumer(testConfig, Set(topic))

          And("An MQTT client publishes some MQTT messages")

          val mqttPublisherConfig = Array(s"mqtt4kafka.mqtt.clientId=${randomString}").asConfig(testConfig)
          Using(MQTT.Settings(mqttPublisherConfig)) { settings =>
            val mqttClient = settings.client
            val expected: List[String] = List(
              "this is an MQTT message",
              "this is another MQTT message",
              "done"
            )
            expected.foreach { data =>
              logger.info(s"Publishing $data to $topic")
              mqttClient.publish(topic, data.getBytes("UTF-8"), 1, true)
            }

            Then("The kafka client should observe the message")

            import RichKafkaConsumer.implicits._
            val received: List[ConsumerRecord[String, Bytes]] = kafkaClient.pull(testTimeout / 2).take(expected.size).toList
            received.size shouldBe expected.size
            received.foreach(_.topic shouldBe topic)
            val actual: List[String] = received.map(d => utf8(d.value))

            actual should contain theSameElementsAs (expected)
          }
        }
      }
    }
  }
  "MqttAdapter.apply" should {
    "published received messages to kafka" in withMat { implicit mat =>

      Given("A Kafka MQTT listener")
      implicit val scheduler = Main.newSchedulerService()

      val mqttTopic1 = randomString
      val kafkaTopic1 = randomString
      val mqttTopic2 = randomString
      val kafkaTopic2 = randomString
      val passthroughTopic = randomString
      Using {
        val config = ConfigFactory.parseString(
          s"""mqtt4kafka.topics.mapping {
             |  $mqttTopic1 : $kafkaTopic1

             |  $mqttTopic2 : $kafkaTopic2

             |}""".stripMargin).withFallback(Main.defaultConfig)
        MqttAdapter(config)
      } { listenerUnderTest =>
        val messages@List((_, first), (_, second), (_, third)) = List(
          mqttTopic1 -> randomString(),
          mqttTopic2 -> randomString(),
          passthroughTopic -> randomString()
        )

        And("A kafka consumer to observe published messages")
        val kafkaClient = newKafkaConsumer(Main.defaultConfig, Set(kafkaTopic1, kafkaTopic2, passthroughTopic, mqttTopic1, mqttTopic2))

        When("The kafka consumer receives the MQTT messages")
        try {
          messages.zipWithIndex.foreach {
            case ((topic, payload), idx) =>
              val mqttMsg = new MqttMessage(payload.getBytes("UTF-8"))
              mqttMsg.setId(idx)
              listenerUnderTest.messageArrived(topic, mqttMsg)
          }

          Then("our kafka consumer should observe the published messages")
          import RichKafkaConsumer.implicits._
          val received: List[ConsumerRecord[String, Bytes]] = kafkaClient.pull(testTimeout / 2).take(messages.size).toList
          received.size shouldBe messages.size

          val actual = received.map { record =>
            record.topic -> utf8(record.value())
          }

          actual should contain only(
            kafkaTopic1 -> first,
            kafkaTopic2 -> second,
            passthroughTopic -> third)
        } finally {
          Try(scheduler.shutdown())
          kafkaClient.close()


        }
      }
    }
  }

  def withMat[A](implicit thunk: ActorMaterializer => A): A = {

    implicit val system = ActorSystem(getClass.getSimpleName.filter(_.isLetter))
    implicit val mat = ActorMaterializer()
    try {
      thunk(mat)
    } finally {
      mat.shutdown()
      system.terminate()
      system.whenTerminated.futureValue
    }
  }

}

object MqttAdapterTest {

  def newKafkaConsumer(rootConfig: Config, topics: Set[String]): KafkaConsumer[String, Bytes] = {
    val config = {
      ConfigFactory.parseString(
        s"""bootstrap.servers : "localhost:9092"
           |group.id : "${BaseDockerSpec.randomString()}"
           |client.id: "${BaseDockerSpec.randomString()}"
           |auto.offset.reset : "earliest"
        """.stripMargin).withFallback(rootConfig.getConfig("akka.kafka.consumer"))
    }
    val properties = propertiesForConfig(config)
    val consumer = new KafkaConsumer[String, Bytes](properties)
    consumer.subscribe(topics.asJava)
    consumer
  }


}
