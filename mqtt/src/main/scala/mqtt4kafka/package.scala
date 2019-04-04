import java.util.Properties

import com.typesafe.config.Config

import scala.util.Try

package object mqtt4kafka {

  type Bytes = Array[Byte]

  def utf8(bytes: Bytes) = new String(bytes, "UTF-8")

  private val UnquoteR = "\\s*\"(.*)\"\\s*".r

  def unquote(str: String) = str match {
    case UnquoteR(middle) => middle
    case _ => str
  }


  def propertiesForConfig(config: Config): Properties = {
    object AsInteger {
      def unapply(str: String): Option[Integer] = {
        Try(Integer.valueOf(str.trim)).toOption
      }
    }
    import args4c.implicits._
    config.collectAsStrings().map {
      case (key, value) => (key, unquote(value))
    }.foldLeft(new java.util.Properties) {
      case (props, (key, AsInteger(value))) =>
        props.put(key, value)
        props
      case (props, (key, value)) =>
        props.put(key, value)
        props
    }
  }
}
