package mqtt4kafka

import com.typesafe.config.Config
import com.typesafe.scalalogging.StrictLogging
import org.eclipse.paho.client.mqttv3._
import org.eclipse.paho.client.mqttv3.persist.{MemoryPersistence, MqttDefaultFilePersistence}
import args4c.implicits._

import scala.util.control.NonFatal

object MQTT extends StrictLogging {

  def run(config: Config, listener: IMqttMessageListener): MqttClient = run(Settings(config), listener)

  def run(settings: Settings, listener: IMqttMessageListener): MqttClient = {
    val client = settings.client
    client.setCallback(LoggingCallback)

    logger.info(s"Subscribing to MQTT broker at '${settings.uri}' for ${settings.topics.size} topics ${settings.topics.mkString("[", ",", "]")}")

    settings.topics.foreach { topic =>
      client.subscribe(topic, settings.qos, listener)
    }
    client
  }

  case class Settings(uri: String, client: MqttClient, topics: List[String], qos: Int) extends AutoCloseable with StrictLogging {
    override def close(): Unit = {
      try {
        client.close()
      } catch {
        case NonFatal(e) =>
          logger.error(s"Error when closing the MQTT client: $e", e)
      }
    }
  }

  object Settings {
    def apply(rootConfig: Config): Settings = {
      val mqttConfig = rootConfig.getConfig("mqtt4kafka.mqtt")
      val (uri, client) = clientForMqttConfig(mqttConfig)
      new Settings(
        uri = uri,
        client = client,
        topics = mqttConfig.asList("topics").distinct,
        qos = mqttConfig.getInt("qos")
      )
    }

    private def clientForMqttConfig(config: Config) = {
      val persistence: MqttClientPersistence = config.getString("persistenceDir") match {
        case "" => new MemoryPersistence
        case dir => new MqttDefaultFilePersistence(dir)
      }
      val uri = config.getString("serverURI")
      val client = new MqttClient(
        uri,
        config.getString("clientId"),
        persistence)
      client.connect(connectOptionsForConfig(config))
      uri -> client
    }

    private def connectOptionsForConfig(config: Config) = {
      def opt(key: String): Option[String] = Option(config.getString(key)).map(_.trim).filterNot(_.isEmpty)

      val opts = new MqttConnectOptions()
      opts.setCleanSession(config.getBoolean("cleanSession"))
      opts.setAutomaticReconnect(config.getBoolean("automaticReconnect"))

      opt("userName").foreach(opts.setUserName)
      opt("password").map(_.toCharArray).foreach(opts.setPassword)
      opts
    }
  }

  object LoggingCallback extends MqttCallback with StrictLogging {
    override def messageArrived(topic: String, message: MqttMessage): Unit = {
      logger.debug(s"Receiving Data, Topic : $topic, message=$message")
    }

    override def connectionLost(cause: Throwable): Unit = {
      logger.warn(s"Connection lost: $cause")
    }

    override def deliveryComplete(token: IMqttDeliveryToken): Unit = {
    }
  }

}
