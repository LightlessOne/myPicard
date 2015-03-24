import com.typesafe.sbt.SbtGit._
import de.johoop.testngplugin.TestNGPlugin._
import sbt.Package.ManifestAttributes

name := "picard"

version := "1.130"

organization := "com.github.broadinstitute"

javaSource in Compile := baseDirectory.value / "src/java"

javaSource in Test := baseDirectory.value / "src/tests"

unmanagedResourceDirectories in Test := Seq(baseDirectory.value / "src/scripts", baseDirectory.value / "testdata", baseDirectory.value / "src/tests/scripts")

libraryDependencies += "org.testng" % "testng" % "6.8.8"

testNGSettings

testNGSuites := Seq("src/tests/resources/testng.xml")

autoScalaLibrary := false

publishMavenStyle := true

publishArtifact in Test := false

pomIncludeRepository := { _ => false}

crossPaths := false

javacOptions in Compile ++= Seq("-source", "1.6")

javacOptions in(Compile, compile) ++= Seq("-target", "1.6")

versionWithGit

assemblyJarName := s"${name.value}-${version.value}.jar"

val PicardOpt = config("picardopt") extend Compile

lazy val htsjdk = uri("git://github.com/samtools/htsjdk")

val gitVersion = settingKey[String]("The picard head commit git hash.")

gitVersion := git.gitHeadCommit.value.get

unmanagedJars in Compile ~= { uj =>
  Seq(Attributed.blank(file(System.getProperty("java.home").dropRight(3) + "lib/tools.jar"))) ++ uj
}

test in assembly := {}

packageOptions := Seq(ManifestAttributes(
  ("Implementation-Version", s"${version.value}(${gitVersion.value})"),
  ("Implementation-Vendor", "Broad Institute"),
  ("Main-Class", "picard.cmdline.PicardCommandLine"),
  ("Implementation-Title", "PICARD Tools")
))

publishTo := {
  val nexus = "https://oss.sonatype.org/"
  if (isSnapshot.value)
    Some("snapshots" at nexus + "content/repositories/snapshots")
  else
    Some("releases" at nexus + "service/local/staging/deploy/maven2")
}

pomExtra := <url>http://samtools.github.io/htsjdk/</url>
  <licenses>
    <license>
      <name>MIT License</name>
      <url>http://opensource.org/licenses/MIT</url>
      <distribution>repo</distribution>
    </license>
  </licenses>
  <scm>
    <url>git@github.com:samtools/htsjdk.git</url>
    <connection>scm:git:git@github.com:samtools/htsjdk.git</connection>
  </scm>
  <developers>
    <developer>
      <id>picard</id>
      <name>Picard Team</name>
      <url>http://broadinstitute.github.io/picard/</url>
    </developer>
  </developers>

assemblyMergeStrategy in assembly := {
  case x if Assembly.isConfigFile(x) =>
    MergeStrategy.concat
  case PathList(ps@_*) if (Assembly.isReadme(ps.last) || Assembly.isLicenseFile(ps.last)) =>
    MergeStrategy.rename
  case PathList("META-INF", xs@_*) =>
    xs map {
      _.toLowerCase
    } match {
      case ("manifest.mf" :: Nil) | ("index.list" :: Nil) | ("dependencies" :: Nil) =>
        MergeStrategy.discard
      case ps@(x :: xs) if ps.last.endsWith(".sf") || ps.last.endsWith(".dsa") =>
        MergeStrategy.discard
      case "plexus" :: xs =>
        MergeStrategy.discard
      case "spring.tooling" :: xs =>
        MergeStrategy.discard
      case "services" :: xs =>
        MergeStrategy.filterDistinctLines
      case ("spring.schemas" :: Nil) | ("spring.handlers" :: Nil) =>
        MergeStrategy.filterDistinctLines
      case _ => MergeStrategy.deduplicate
    }
  case "asm-license.txt" | "overview.html" =>
    MergeStrategy.discard
  case _ => MergeStrategy.deduplicate
}

val root = project.in(file(".")).
  configs(PicardOpt).
  settings(inConfig(PicardOpt)(
  Classpaths.configSettings ++ Defaults.configTasks ++ baseAssemblySettings ++ Seq(
    assemblyJarName := s"${name.value}-opt-${version.value}.jar",
    //this is where we would put in the maven dependency for gatk-tools-java-picard
    //currently it expects it to be in opt and you can't build picard-opt jar without it there
    unmanagedJars in Compile += baseDirectory.value / "opt/gatk-tools-java-picard-1.0.jar",
    assemblyExcludedJars in assembly := {
      val cp = (fullClasspath in assembly).value
      cp filter { jar =>
        jar.data.getName == "tools.jar"
      }
    }
  )): _*) dependsOn htsjdk


assemblyExcludedJars in assembly := {
  val cp = (fullClasspath in assembly).value
  cp filter { jar =>
    jar.data.getName == "gatk-tools-java-picard-1.0.jar" || jar.data.getName == "tools.jar"
  }
}