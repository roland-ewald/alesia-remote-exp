package alesia.utils.remote.actors

import akka.actor.actorRef2Scala
import alesia.utils.remote.Config
import alesia.utils.remote.ExpID
import alesia.utils.remote.MsgExperimentInit
import alesia.utils.remote.MsgFilePackage
import alesia.utils.remote.MsgHandshake
import alesia.utils.remote.MsgReadFile
import alesia.utils.remote.MsgStartExperiment

/**
 * Central entry point to the ActorSystem.
 */
class EntryActor extends AbstractActor {
  log.info("EntryActor at service.")

  val experimentID = new ExpID

  val fileActor = context.actorOf(FileActor(Config.entrypointDir, experimentID))

  val remoteWorker = context.actorFor(Config.actorAdress(Config.workerActorName, Config.workerASName, Config.workerIP, Config.workerPort))

  remoteWorker ! MsgExperimentInit(experimentID, Config.expMainClass)

  /** Wait for handshake. @see MsgHandshake */
  override def receive = {
    case MsgHandshake(aRef) => {
      fileActor ! MsgReadFile(Config.entrypointDir + Config.separator + Config.classFileName, aRef) // send class file
      aRef ! MsgStartExperiment(experimentID) // signals, all files have been sent, start execcution
    }
    case resultFile: MsgFilePackage => fileActor ! resultFile
  }
}
