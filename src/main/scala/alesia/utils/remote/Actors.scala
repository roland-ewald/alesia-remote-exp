package alesia.utils.remote

import java.io.File
import java.io.PrintWriter
import scala.io.Source
import scala.sys.process.Process
import akka.actor.Actor
import akka.actor.actorRef2Scala
import akka.event.Logging
import akka.util.duration.intToDurationInt
import akka.dispatch.Future
import akka.actor.Status.Success
import akka.actor.Props
import akka.dispatch.OnSuccess

object Actors {
	class WorkerActor extends Actor {
		val log = Logging(context.system, this)
		log.info("WorkerActor at service.")

		override def receive = {
			case MsgCreateExperiment(classfileContent: String, number: Int) => {
				val experiment = context.actorOf(Props[WorkerExperimentActor]) // TODO: differentiate for each experiment folder on file system!
				experiment ! MsgCreateExperiment(classfileContent: String, number)
			}
		}
	}

	class WorkerExperimentActor extends Actor {
		import context.dispatcher // execturion context for Futures
		val log = Logging(context.system, this)
		log.info("WorkerExperimentActor at service.")
		var pb: scala.sys.process.ProcessBuilder = null // keep this, so wathcdog can cancell it TODO: Watchdog
		var experimentNumber: Int = 0

		override def receive = {
			case MsgCreateExperiment(classfileContent: String, number: Int) => {
				log.info("Msg received");
				experimentNumber = number

				// create experiment directory: TODO: make concurrent
				val dir = new File(Config.experimentDirectory(experimentNumber))
				if (!dir.exists() && !dir.mkdir()) log.info("Could not create experiment directory"); // TODO: error handling
				dir.deleteOnExit()

				// crete the .class file, that is the experiment:
				val file = new File(Config.experimentDirectory(experimentNumber) + Config.separator + Config.experimentFileName)
				file.deleteOnExit() // file will be deleted when jvm exits
				val pw = new PrintWriter(file)

				val f = Future {
					pw.print(classfileContent)
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
						case lines: String => context.actorFor(Config.actorAdress(Config.entryActorName, Config.entryASName, Config.entryIP, Config.entryPort)) ! MsgExperimentResults(Config.resultFileName, lines)
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
	class EntryActor extends Actor {
		val log = Logging(context.system, this)
		log.info("EntryActor at service.")

		override def receive = {
			case MsgExperimentResults(filename: String, content: String) => {
				log.info("Results received")
				val file = new File("Entrypoint_results.txt")

				file.createNewFile()
				val pw = new PrintWriter(file)
				try { pw.println(content); pw.println("At Entrypoint at " + System.currentTimeMillis()) } finally { pw.close() }
			}
		}
	}
}

case class MsgCreateExperiment(classfileContent: String, number: Int)
case class MsgExperimentReady
case class MsgIsExperimentReady
case class MsgExperimentResults(filename: String, content: String)
case class MsgIsExperimentCreated