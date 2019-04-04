package mqtt4kafka

import org.scalatest.concurrent.Eventually
import org.scalatest.{BeforeAndAfterAll, GivenWhenThen, Matchers, WordSpec}

/**
  * Test we can, after spinning up:
  *
  * 1) a kafka container
  * 2) our mqtt/kafka publisher container
  *
  * send an MQTT message to localhost:1883 and consumer a kafka message
  */
//class PingTest extends BaseKafkaSpec {
class PingTest extends WordSpec with Matchers with Eventually with BeforeAndAfterAll with GivenWhenThen with eie.io.LowPriorityIOImplicits {

  "MQTT to Kafka" ignore {

    "work" in {

    }
  }

}
