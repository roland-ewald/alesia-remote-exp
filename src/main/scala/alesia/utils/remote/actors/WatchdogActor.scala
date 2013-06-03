package alesia.utils.remote.actors

import java.io.File

import scala.concurrent.Future
import scala.sys.process.Process

import akka.actor.Actor
import akka.actor.Props
import akka.actor.actorRef2Scala
import akka.event.Logging
import alesia.utils.remote.Config
import alesia.utils.remote.MsgExperimentConcluded

/**
 * Handles and (later) observes experiment execution
 */
class WatchdogActor(contextFolder: String, expDir: String, clazzName: String, execDir: String) extends AbstractActor {
  log.info("WatchdogActor at service.")
  log.info("WatchdogActor: starting execution.");

  // create the process that is the experiment:
  val pb = Process(Config.experimentCommandSeq2(clazzName, contextFolder, expDir), new File(contextFolder + Config.separator + expDir + Config.separator + execDir))
  val p = context.parent
  val s = self
  val f = Future {
    val res = pb.!! // executes the console lines. see execution context
    log.info("Watchdog: " + res)
    p ! MsgExperimentConcluded()
    context.stop(s)
  }
  f.onFailure {
    case e => log.info("Error: " + e)
  }

  override def receive = {
    case _ => {} //TODO: add logic for failure handling
  }
}

object WatchdogActor {
  def apply(contextFolder: String, expDir: String, execDir: String, clazzName: String): Props = { Props(new WatchdogActor(contextFolder, expDir, clazzName, execDir)) }
}