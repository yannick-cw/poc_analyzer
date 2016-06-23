package utils

import akka.testkit.TestKit
import org.scalatest.{BeforeAndAfterAll, Suite}

/**
  * Created by yannick on 09.02.16.
  */
trait StopSystemAfterAll extends BeforeAndAfterAll{
  this: TestKit with Suite =>
  override protected def afterAll(): Unit = {
    super.afterAll()

    system.terminate()
  }
}
