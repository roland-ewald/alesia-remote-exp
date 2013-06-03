package alesia.utils.remote.mainclasses

import com.typesafe.config.ConfigFactory
import akka.actor._
import akka.actor.ActorSystem
import akka.actor.Props
import alesia.utils.remote.actors.WorkerActor
import alesia.utils.remote.Config

object ExecuteWorker {
	def main(args: Array[String]) = {

		//		val a:ComponentRatingSystem = TrueSkillRatingSystem.createDefaultSetup
		//		a.submitResults(List(Set("a"), Set("b")))
		//		System.out.println(a.getPoints("a"))

		val workerAS = ActorSystem(Config.workerASName, ConfigFactory.load(ConfigFactory.parseString(Config.configString(Config.workerIP, Config.workerPort))))

		val worker = workerAS.actorOf(Props[WorkerActor], name = Config.workerActorName)
		Thread.sleep(30000)
		workerAS.shutdown // shutdown after 30 sec
	}
}