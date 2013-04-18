package alesia.utils.remote.actors

import java.io.File
import java.io.PrintWriter
import scala.io.Source
import scala.sys.process.Process
import akka.actor.Actor
import akka.actor.Props
import akka.actor.actorRef2Scala
import akka.dispatch.Future
import akka.event.Logging
import alesia.utils.remote.Config
import scala.collection.immutable.HashMap
import alesia.utils.remote.ID
import akka.actor.ActorRef
import alesia.utils.remote.MsgCreateExperiment
import alesia.utils.remote.MsgExperimentResults
import alesia.utils.remote.MsgIsExperimentCreated
import alesia.utils.remote.MsgIsExperimentReady
import alesia.utils.remote.MsgCreateExperiment2
import akka.actor.ActorPath
import java.io.FileOutputStream

class WorkerActor extends Actor {
	val log = Logging(context.system, this)
	log.info("WorkerActor at service.")
	var experimentActorMap = new HashMap[ID, ActorRef]
	var entryActorMap = new HashMap[ID, ActorRef]
	var experimentNumber = 1 // arbitrary number, just for experiment folders

	override def receive = {
		case MsgCreateExperiment(classfileContent: Array[Byte]) => {
			val experiment = context.actorOf(Props[WorkerExperimentActor])
			val id = new ID
			experiment ! MsgCreateExperiment2(classfileContent, experimentNumber, id: ID)
			experimentActorMap += id -> experiment
			val sendr = sender
			entryActorMap += id -> sendr
			experimentNumber = experimentNumber + 1
		}
		case MsgExperimentResults(id: ID, content: String) => {
			log.info("Experiment results at parent.")
			entryActorMap(id) ! MsgExperimentResults(id, content)
			experimentActorMap.-(id)
			entryActorMap.-(id)
		}
	}
}

class WorkerExperimentActor extends Actor {
	import context.dispatcher // execturion context for Futures
	val log = Logging(context.system, this)
	log.info("WorkerExperimentActor at service.")
	var pb: scala.sys.process.ProcessBuilder = null // keep this, so wathcdog can cancell it TODO: Watchdog
	var experimentNumber: Int = 0
	var id: ID = new ID

	override def receive = {
		case MsgCreateExperiment2(classfileContent: Array[Byte], number: Int, id: ID) => {
			log.info("Msg received");
			experimentNumber = number
			this.id = id

			// create experiment directory: TODO: make concurrent
			val dir = new File(Config.experimentDirectory(experimentNumber))
			if (!dir.exists() && !dir.mkdir()) log.info("Could not create experiment directory"); // TODO: error handling
			dir.deleteOnExit()

			// crete the .class file, that is the experiment:
			val file = new File(Config.experimentDirectory(experimentNumber) + Config.separator + Config.experimentFileName)
			file.deleteOnExit() // file will be deleted when jvm exits
			val pw = new FileOutputStream(file)

			val f = Future {
				pw.write(classfileContent)
				pw.close // includes flush
			} onSuccess {
				case _ => self ! MsgIsExperimentCreated //
			} onFailure {
				case exception => ; // TODO: File was not created
			}
		}

		case MsgIsExperimentCreated => {
			// here: assured the Experiment *.class files are safely created
			log.info("Experiment File created");

			// create the process that is the experiment:
			pb = Process(Config.experimentCommandSeq(experimentNumber), new File(Config.contextFolder + Config.separator + Config.experimentDirectory(experimentNumber)))

			val f = Future {
				pb.!! // executes the console lines. see execution context
			} onSuccess {
				case res: String => self ! MsgIsExperimentReady
			} onFailure {
				case _ => ; // note: cannot happen. use watchdog instead
			}
		}
		case MsgIsExperimentReady => {
			// here: assured Experiment is finished
			log.info("Experiment has exited")
			val x = context.parent

			// retrieve the experiment result file and send it to entrypoint:
			val file = new File(Config.experimentDirectory(experimentNumber) + Config.separator + Config.resultFileName)
			if (file.exists()) { // send file
				val source = Source.fromFile(file)
				val f = Future {
					val lines = source.mkString // blocks till finished
					source.close
					file.delete()
					lines
				} onSuccess {
					case lines: String => x ! MsgExperimentResults(id, lines)
				} onFailure {
					case exception => ; // TODO: file could not be read
				}
			} else {
				log.error("no result file found")
				// TODO: no File
			}
			pb = null
			experimentNumber = 0
			// at this point, the actor has received the last message and can be closed
			context.stop(self)
		}
	}
}
