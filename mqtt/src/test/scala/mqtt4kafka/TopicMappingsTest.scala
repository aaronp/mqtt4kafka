package mqtt4kafka

import com.typesafe.config.ConfigFactory
import org.scalatest.{Matchers, WordSpec}

class TopicMappingsTest extends WordSpec with Matchers {

  "TopicMappings.forConfig" should {
    "map topics" in {
      val config = ConfigFactory.parseString(
        """foo : bar
          |fizz : buzz
          |"x/y/z" : "a-b/c"
        """.stripMargin)
      val mapping = TopicMappings.forConfig(config)
      mapping("foo") shouldBe "bar"
      mapping("fizz") shouldBe "buzz"
      mapping("x/y/z") shouldBe "a-b/c"
      mapping("unspecified") shouldBe "unspecified"
      mapping("") shouldBe ""
      mapping("pass-through") shouldBe "pass-through"
    }
  }
}
