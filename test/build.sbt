import eie.io._
enablePlugins(CucumberPlugin)
CucumberPlugin.glue := "classpath:mqtt4kafka"
CucumberPlugin.features := List("classpath:mqtt4kafka.test")