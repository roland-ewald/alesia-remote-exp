package alesia.utils.remote

import java.lang.reflect.Array
import java.io.File

object Config {

	val workerIP = "127.0.0.1"
	val workerPort = "2500"
	val workerASName = "WorkerActorSystem"
	val workerActorName = "WorkerActor"

	val entryIP = "127.0.0.1"
	val entryPort = "2501"
	val entryASName = "EntryActorSystem"
	val entryActorName = "EntryActor"

	def separator: String = System.getProperty("file.separator");

	// On Worker only:
	def contextFolder = (new File(".")).getCanonicalPath() // The folder the actorSystem is executed in	
	def experimentCommandSeq(n: Int): Seq[String] = Seq("java", "-cp", "\"" + contextFolder + separator + experimentDirectory(n) + "\"", "HelloWoerld")
	def resultFileName = "results.txt"
	def experimentDirectory = (number: Int) => "ExperimentDir" + number

	// On Entrypoint only:
	def experimentFileOriginalFile = List("src", "main", "resources", "HelloWoerld.class").fold(".")((a, b) => a + separator + b)

	// On Worker and Entrypoint:
	def experimentFileName = "HelloWoerld.class"

	def configString(ip: String, port: String): String = {
		"""
			  akka {
			    actor {
			      provider = "akka.remote.RemoteActorRefProvider"
			    }
			    remote {
			      transport = "akka.remote.netty.NettyRemoteTransport"
			      netty {
			        hostname = """" + ip + """"
			        port = """ + port + """
			      }
			    }
			  }
		"""
	} // NOTE: IP IS SURROUNDED BY "'s
	def actorAdress(name: String, asName: String, ip: String, port: String): String = "akka://" + asName + "@" + ip + ":" + port + "/user/" + name

}