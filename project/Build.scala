import java.nio.file.Path

import sbt.IO

object Build {

  def docker(
              deployResourceDir: Path,
              mqttAssembly: Path,
              targetDir: Path,
              logger: sbt.util.Logger) = {

    import scala.sys.process._

    val whichDocker = "which docker".!
    logger.warn(s"whichDocker is ${whichDocker}")
    logger.warn(s"dockerDir=$targetDir, mqttAssembly is ${mqttAssembly} class ${mqttAssembly.getClass}")

    IO.copyDirectory(deployResourceDir.toFile, targetDir.toFile)
    IO.copy(List(mqttAssembly.toFile -> (targetDir.resolve("app.jar").toFile)))

    execIn(targetDir, "docker", "build", "--tag=mqtt4kafka", ".")
  }

  def execIn(inDir: Path, cmd: String*): Unit = {
    import scala.sys.process._
    val p: ProcessBuilder = Process(cmd.toSeq, inDir.toFile)
    val retVal = p.!
    require(retVal == 0, cmd.mkString("", " ", s" in dir ${inDir} returned $retVal"))
  }

}
