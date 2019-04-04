package mqtt4kafka

import com.typesafe.config.Config
import args4c.implicits._

object TopicMappings {
  def apply(config: Config): Map[String, String] = forConfig(config.getConfig("mqtt4kafka.topics.mapping"))

  def forConfig(config: Config): Map[String, String] = {
    config.collectAsMap().map {
      case (key, value) => unquote(key) -> unquote(value)
    }.withDefault(identity)
  }
}
