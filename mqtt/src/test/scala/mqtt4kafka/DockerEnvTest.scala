package mqtt4kafka

class DockerEnvTest extends BaseDockerSpec("scripts/mqtt") {
  "DockerEnv" should {
    "start/stop docker" in {
      if (isDockerRunning()) {
        stopDocker() shouldBe true
      }

      isDockerRunning() shouldBe false
      startDocker() shouldBe true
      startDocker() shouldBe true
      stopDocker() shouldBe true
      isDockerRunning() shouldBe false
    }
  }
}
