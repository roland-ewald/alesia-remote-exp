package alesia.utils.remote

import java.lang.reflect.Array
import java.io.File

object Config {

	val maxPackageSize = 900000 // in Bytes of content, currently about 900000 = 900kb

	val workerIP = "127.0.0.1"
	val workerPort = "2500"
	val workerASName = "WorkerActorSystem"
	val workerActorName = "WorkerActor"

	val entryIP = "127.0.0.1"
	val entryPort = "2501"
	val entryASName = "EntryActorSystem"
	val entryActorName = "EntryActor"

	// System Properties 
	def separator: String = System.getProperty("file.separator")
	def cSeperator: String = System.getProperty("path.separator")

	// On Worker only:
	def contextFolder = (new File(".")).getCanonicalPath() // The folder the actorSystem is executed in	
	def experimentCommandSeq(n: Int): Seq[String] = Seq("java", "-cp", "\"" + contextFolder + separator + experimentDirectory(n) + "\"", "HelloWoerld")
	def resultFileName = "results.txt"
	def experimentDirectory = (number: Int) => "ExperimentDir" + number
	//	def experimentCommandSeq(dir: String, clazz: String): Seq[String] = Seq("java", "-cp", "\"" + dir + cSeperator + libsFolder(dir) + separator + "*" + "\"", clazz)
	def libsFolder(dir: String) = dir + separator + "libs"
	def resultsFolder(dir: String) = dir + separator + "results"

	// On Entrypoint only:
	def experimentFileOriginalFile = "./HelloWoerld.class" // List("src", "main", "resources", "HelloWoerld.class").fold(".")((a, b) => a + separator + b)

	// On Worker and Entrypoint:
	def experimentFileName = "HelloWoerld.class"
	def expMainClass = "HelloWoerld"

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
			      log-sent-messages = on
			      log-received-messages = on
			    }
			  }
		"""
	} // NOTE: IP IS SURROUNDED BY "'s
	def actorAdress(name: String, asName: String, ip: String, port: String): String = "akka://" + asName + "@" + ip + ":" + port + "/user/" + name

}