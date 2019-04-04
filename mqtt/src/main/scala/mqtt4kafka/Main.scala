package mqtt4kafka

import java.util.concurrent.{Executors, ScheduledExecutorService, ThreadFactory}

import args4c.ConfigApp
import com.typesafe.config.{Config, ConfigFactory}
import com.typesafe.scalalogging.StrictLogging

/**
  * The main entry point of an application which connects an MQTT subscriber to an MQTT broker and republishes the messages to kafka
  */
object Main extends ConfigApp with StrictLogging {
  type Result = RunningApp

  override def defaultConfig(): Config = ConfigFactory.load()

  override def runMain(userArgs: Array[String],
                       readLine: String => String,
                       setupUserArgFlag: String = defaultSetupUserArgFlag,
                       ignoreDefaultSecretConfigArg: String = defaultIgnoreDefaultSecretConfigArg,
                       pathToSecretConfigArg: String = defaultSecretConfigArg): Option[Result] = {
    logger.info(s"${userArgs.size} User Args: ${userArgs.mkString("[", ",", "]")}")
    super.runMain(userArgs, readLine, setupUserArgFlag, ignoreDefaultSecretConfigArg, pathToSecretConfigArg)
  }

  override def run(config: Config) = {
    logger.info("Running with:\n" + config.withPaths("mqtt4kafka").summary())

    implicit val scheduler: ScheduledExecutorService = newSchedulerService()
    val listener = MqttAdapter(config)
    val client = MQTT.run(config, listener)
    RunningApp(client, listener, scheduler)
  }

  def newSchedulerService(): ScheduledExecutorService = {
    Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {
      override def newThread(r: Runnable): Thread = {
        val t = new Thread(r)
        t.setName("flush-scheduler")
        t.setDaemon(true)
        t
      }
    })
  }
}
