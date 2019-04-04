package mqtt4kafka

import com.typesafe.config.ConfigFactory
import org.eclipse.paho.client.mqttv3.{IMqttMessageListener, MqttClient, MqttMessage}

import scala.collection.mutable.ListBuffer
import scala.util.Try

class MQTTTest extends BaseDockerSpec("scripts/mqtt") {
  "MQTT.run" should {
    "notify a listener for all topics" in {
      Given("A publisher MQTT client")
      val persistenceDir = s"target/${getClass.getSimpleName}${System.nanoTime}/persistence".asPath.mkDirs()
      val publisherClient: MqttClient = {
        val config = {
          ConfigFactory.parseString(
            s"""mqtt4kafka.mqtt.persistenceDir=${persistenceDir.toAbsolutePath.toString}
               |mqtt4kafka.mqtt.clientId=${randomString}
             """.stripMargin).withFallback(Main.defaultConfig())
        }
        MQTT.Settings(config).client
      }


      And("An MQTT subscriber client")
      val listener = new MQTTTest.TestListener
      val client = {
        val config = {
          ConfigFactory.parseString(
            s"""mqtt4kafka.mqtt.clientId=${randomString}
               |mqtt4kafka.mqtt.topics=["first", "second"]
             """.stripMargin).withFallback(Main.defaultConfig())
        }
        MQTT.run(config, listener)
      }

      When("The publisher sends some messages to subscribed topics")
      try {
        publisherClient.publish("first", "message on first".getBytes("UTF-8"), 2, true)
        publisherClient.publish("second", "message on second".getBytes("UTF-8"), 2, true)
        publisherClient.publish("ignored", "message should be ignored".getBytes("UTF-8"), 2, true)

        Then("The subscriber should see the messages to which it has subscribed")
        eventually {
          listener.received.size should be >= 2
        }

        listener.received should contain only(
          "first" -> "message on first",
          "second" -> "message on second")
      } finally {
        Try(publisherClient.close())
        Try(client.close())
        Try(persistenceDir.delete())
      }
    }
  }
}

object MQTTTest {
  class TestListener extends IMqttMessageListener {
    val received = ListBuffer[(String, String)]()

    override def messageArrived(topic: String, message: MqttMessage): Unit = {
      val d8a = utf8(message.getPayload)
      received += (topic -> d8a)
    }
  }
}
