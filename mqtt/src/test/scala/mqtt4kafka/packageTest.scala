package mqtt4kafka

import org.scalatest.{Matchers, WordSpec}

class packageTest extends WordSpec with Matchers {

  "unquote" should {
    List(
      "foo" -> "foo",
      "foo\" " -> "foo\" ",
      "\" foo \" " -> " foo ",
      "some \" foo \" " -> "some \" foo \" ",
      "\" foo \" suffix" -> "\" foo \" suffix"
    ).foreach {
      case (input, expected) =>
        s"Unquote >$input< as $expected" in {
          unquote(input) shouldBe expected
        }
    }
  }
}
