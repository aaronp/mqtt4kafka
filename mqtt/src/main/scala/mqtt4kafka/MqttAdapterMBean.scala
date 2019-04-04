package mqtt4kafka

/**
  * Operations for the MqttAdapter
  */
trait MqttAdapterMBean {

  def cancelAutoFlush() : Boolean

  def setAutoFlush(frequencyInMillis : Int) : Boolean

  def sendTestMessage(topic : String, key : String, message : String)

  /**
    * Forces a flush
    *
    * @return true if we're currently connected to kafka
    */
  def flush(): Boolean

  /** @return true if this operation had an effect in disconnecting from kafka
    */
  def disconnectFromKafka(): Boolean

  /** Disconnect and reconnect to kafka
    */
  def reconnect(): Unit

  /**
    * Override a kafka property
    *
    * @param key
    * @param value
    * @return the properties as a string
    */
  def setProperty(key: String, value: String): String

  /** @return the properties with all overrides removed
    */
  def cleanPropertyOverrides(): String

  /** @return the kafka properties as a string
    */
  def getProperties(): String

  /** @return the mqtt config
    */
  def getConfig(): String
}