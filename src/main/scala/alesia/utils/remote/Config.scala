package alesia.utils.remote

import java.lang.reflect.Array
import java.io.File

object Config {

	val maxPackageSize = 90000 // in Bytes of content, max should be about 900000 = ~900kb

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
	def experimentCommandSeq2(clazzName: String, contextFolder: String, expDir: String): Seq[String] = Seq("java", "-cp", "\"" + contextFolder + separator + expDir + "\"", clazzName)
	def experimentDirectory(number: Long) = "ExperimentDir" + number
	def libsFolder(dir: String) = dir + separator + "libs"
	def resultsFolder(dir: String) = dir + separator + resultsFolderName
	def resultsFolderName = "results"
	def resultFileName = "results.txt"

	// On Entrypoint only:
	def entrypointDir = Config.contextFolder + Config.separator + "Entrypoint"
	def classFileName = "HelloWoerld.class"

	// On Worker and Entrypoint:
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