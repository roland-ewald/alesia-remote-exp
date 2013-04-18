package alesia.utils.misc

import com.jcabi.aether.Aether
import java.io.File
import java.util.Arrays
import org.apache.maven.project.MavenProject
import org.sonatype.aether.artifact.Artifact
import org.sonatype.aether.util.artifact.DefaultArtifact
import org.sonatype.aether.repository.RemoteRepository
import org.apache.maven.artifact.repository.MavenArtifactRepository
import org.apache.maven.artifact.repository.ArtifactRepositoryPolicy
import org.apache.maven.artifact.repository.layout.DefaultRepositoryLayout

/**
 * Testing jcabi-aether (http://www.jcabi.com/jcabi-aether) for retrieval of JAR files...
 *
 * @author Roland Ewald
 */
object TestMavenJARRetrieval extends App {

  val myLocalMavenRepo = "C:/mavenrepo"

  val myRemoteMavenRepo = "http://repo1.maven.org/maven2/"

  val rep = new MavenArtifactRepository(
    "remote", myRemoteMavenRepo,
    new DefaultRepositoryLayout(), new ArtifactRepositoryPolicy(), new ArtifactRepositoryPolicy())

  val project = new MavenProject()
  project.setRemoteArtifactRepositories(
    Arrays.asList(rep))

  val deps = new Aether(project, new File(myLocalMavenRepo)).resolve(
    new DefaultArtifact("com.jcabi", "jcabi-aether", "", "jar", "0.7.17"),
    "runtime")

  val it = deps.iterator()
  while (it.hasNext()) {
    val d = it.next()
    println(d.getFile())
  }

}