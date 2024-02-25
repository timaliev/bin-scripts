#!/usr/bin/env -S scala-cli shebang
// (SPDX-License-Identifier: MIT)
//> using scala 3.3
//> using resourceDir ${.}/conf
//> using dep "com.lihaoyi::os-lib:0.9.3"
//> using dep "io.circe::circe-yaml:1.15.0"
// v1.3.5 supports Java 8, latest version only Java 11+
//> using dep "ch.qos.logback:logback-classic:1.3.5"
//> using dep "com.typesafe.scala-logging::scala-logging:3.9.4"

// import scala.language.postfixOps
// import java.lang.System // Read more at: https://studentprojectcode.com/blog/how-to-get-and-set-environment-variables-in-scala

// import java.io.FileReader
// import java.io.FileWriter
// import java.io.File
// import java.nio.file.{Path, Paths, Files, FileAlreadyExistsException}
// import java.nio.file.attribute.{FileAttribute, PosixFilePermission, PosixFilePermissions}
// import scala.io.Source
// import scala.io.StdIn
// import java.util.Calendar
// import java.net.URL
// import org.apache.commons.codec.binary.Base64

// Yaml librarie
// import org.yaml.snakeyaml.Yaml
import io.circe._, io.circe.yaml, io.circe.yaml._
// import io.circe._, io.circe.syntax._, io.circe.Parser
// import cats.syntax.eq._, cats.instances.string._

import scala.util.{Try, Success, Failure}
// import scala.concurrent.{Await, Future}
// import scala.concurrent.duration._
// import scala.collection._

import com.typesafe.scalalogging._

// Defaults
final val defaultKubeConfFile: String = s"${os.home}/.kube/config"
val program = new Exception().getStackTrace.head.getFileName
/* Usage helper */
final val usage: String = s"""
Usage: ${program} [kubeconfig file]
  expects at most one argument: name of kubeconfig file
  if no argument is given tries to use configuration file,
  environment variable KUBECONFIG, or default file location: ${os.home}/.kube/config
"""
/* Logger to be used by programm */
val logger = Logger(getClass.getName)
/* Programm configuration class */
case class Conf(
  kubeConfigFile: String
)
/** Exit with error code and error message to logger
  * 
  * @param msg Message to log
  * @param err error code. if error == 0 log with level INFO
  */
def die(msg: String, err: Int = 127): Unit = {
  err match
    case 0 => logger.info(msg)
    case _ => logger.error(msg)
  sys.exit(err)
}
/* Same as die() only also prints the same message on Console */
def dieConsole(msg: String, err: Int = 127): Unit = {
  println(s"ERROR: ${msg}")
  die(msg, err)
}
/* Filesystem existence check, wraper around OS-Lib */
def pathExists(f: String): Boolean = os.exists(getPath(f))
/* OS-Lib file path wraper around String */
def getPath(f: String)= os.Path(f, base = os.pwd)
/* Try to read file content as a String */
def fileReader(f: String) = Try{os.read(getPath(f))}.toEither
/** Get a Map from any case class instance
  *
  * From here [[https://stackoverflow.com/questions/1226555/case-class-to-map-in-scala]]
  * 
  * @param cc case class instance
  * @return Map() where keys are case class fields names and values are
  * corresponding values
  */
def getCCParams(cc: AnyRef) =
  cc.getClass.getDeclaredFields.filterNot(_.isSynthetic).foldLeft(Map.empty[String, Any]) { (a, f) =>
    f.setAccessible(true)
    a + (f.getName -> f.get(cc))
  }

/** Parse program arguments and return a Map with configuration
  * 
  * @param args array of strings representing command line args
  * @return Map() with named configuration parameters either from command line
  * or from environment variables or from configuration file or defaults
  * 
  * Processing order and prority:
  * - Try command line arguments
  * - if none, try environment variables
  * - If none, try to use configuration file PureConfig (TODO:)
  * - If none or invalid, use defaults
  */
def parseArguments(args: Array[String]): Map[String, Any] =
  args.length match
    case 0 => {
      // Take only the first path from KUBECONFIG as kubectl config dir
      // Take default if it's empty
      val kubeConfigEnv = Try{ sys.env("KUBECONFIG") }.toEither
      logger.debug(s"Got environment variable KUBECONFIG: ${kubeConfigEnv.toOption.getOrElse("''")}")
      // Take only the first path from KUBECONFIG as kubectl config dir
      // Take default if it's empty
      kubeConfigEnv match
        case Right(s) if !s.isEmpty && s != ":" => {
          logger.debug(s"KUBECONFIG environment variable is set to: '${s}'")
          val c = s.split(':')(0)
          if pathExists(c)
          then getCCParams(Conf(c))
          else {
            die(s"Kubeconfig file ${c} does not exist, exiting.")
            Map()
          }
        }
        case _ => {
          logger.debug(s"KUBECONFIG environment variable set but not valid, will try to use defaults: '${defaultKubeConfFile}'")
          if pathExists(defaultKubeConfFile)
          then getCCParams(Conf(defaultKubeConfFile))
          else {
            die(s"Kubeconfig file ${defaultKubeConfFile} does not exist, exiting.")
            Map()
          }
        }
      }
    case 1 => {
      if pathExists(args(0))
      then getCCParams(Conf(args(0)))
      else {
        die(s"Kubeconfig file ${args(0)} does not exist")
        Map()
      }
    }
    case _ => {
      println(usage)
      die(s"Too many command line arguments given")
      Map()
    }
/* Read kubeconfig file and return it as Json object */
def getFileJson(f: String)= fileReader(f) match
  case Right(value) => {
    yaml.parser.parse(value) match
      case Right(json) => json
      case Left(fail) => {
        dieConsole(s"Kubeconfig file parsing failure ${fail}")
        Json.Null
      }
  }
  case Left(err) => {
    dieConsole(s"Cannot read kubeconfig file: ${err}")
    Json.Null
  }

logger.debug(s"======================")
logger.debug(s"Started $program")
logger.info("This utility will convert 'kubectl' configuration file to use auth data in files rather then inline")
logger.debug(s"======================")
logger.debug(s"Current working directory: ${os.pwd}")
logger.debug(s"${args.length} arguments: ${args.headOption}")

val conf = parseArguments(args)
logger.debug(s"Using configuration file: ${conf("kubeConfigFile").toString}")
val kubeConfFileJson = getFileJson(conf("kubeConfigFile").toString)
logger.debug(s"Configuration file ${conf("kubeConfigFile").toString} opened and is valid YAML: ${kubeConfFileJson.toString.slice(0,50) + "..."}")

val cursor: HCursor = kubeConfFileJson.hcursor

def authDataMap(cursor: HCursor, obj: String, authDataPath: String): Map[String, String] = {
  val objArray: Vector[Json] = cursor
    .root
    .downField(obj)
    .focus
    .flatMap(_.asArray)
    .getOrElse(Vector.empty)
  objArray.isEmpty match
    case false => logger.debug(s"Got this '${obj}' array: ${objArray.toString.slice(0,50) + "..."}")
    case true => {
      logger.warn(s"Empty '${obj}' array in ${conf("kubeConfigFile").toString}")
      return Map()
    }
  val objNames: Vector[String] = objArray
    .map(n => n.hcursor.get[String]("name"))
    .map(e => e.toOption.getOrElse(""))
  val authDataList: List[String] = authDataPath.split("/").toList
  val objAuthData = objArray
    .map(n => n
      .hcursor
      .downField("cluster")
      .get[String]("certificate-authority-data"))
    .map(e => e.toOption.getOrElse(""))
  Map()
}

val clustersArray = cursor
  .root
  .downField("clusters")
  .focus
  .flatMap(_.asArray)
  .getOrElse(Vector.empty)
clustersArray.isEmpty match
  case false => logger.debug(s"Got this 'clusters' array: ${clustersArray.toString.slice(0,50) + "..."}")
  case true => dieConsole(s"Empty 'clusters' array in ${conf("kubeConfigFile").toString}")
val clustersNames = clustersArray
  .map(n => n.hcursor.get[String]("name"))
  .map(e => e.toOption.getOrElse(""))
val clustersAuthData = clustersArray
  .map(n => n.hcursor.downField("cluster")
  .get[String]("certificate-authority-data"))
  .map(e => e.toOption.getOrElse(""))
val clustersAuthDataMap = clustersNames
  .iterator
  .zip(clustersAuthData)
  .toMap
logger.debug(s"Clusters certificate-authority-data => ${clustersAuthDataMap.toString.slice(0,50) + "..."}")

die(s"End of the story. ${program} exited with error code=1", 1)
/*
object replaceKeysToFiles extends App with Logging {
  // System.setProperty("log4j.configurationFile", "lib/log4j2.properties")
  // Debug error message & exit
  def dieLoud(msg: String): Unit = {
    logger.error(msg)
    sys.exit()
  }
  // Get file reader for path
  def fileReader(path: String): Either[Throwable, FileReader] =
    Try {
      new FileReader(new File(path))
    }.toEither
  // Get file writer for path
  def fileWriter(path: String): Either[Throwable, FileWriter] =
    Try {
      new FileWriter(new File(path))
    }.toEither
  // Creat directory on filesystem
  def createOsDir(path: String, fileAttributes: Option[java.nio.file.attribute.FileAttribute[java.util.Set[java.nio.file.attribute.PosixFilePermission]]]): Either[Throwable, java.nio.file.Path] =
    Try {
      Files.createDirectory(Paths.get(path), fileAttributes.getOrElse(null))
    }.toEither
  def createOsDir(path: String, fileAttributes: java.nio.file.attribute.FileAttribute[java.util.Set[java.nio.file.attribute.PosixFilePermission]]): Either[Throwable, java.nio.file.Path] =
      createOsDir(path, Some(fileAttributes))
  def createOsDir(path: String): Either[Throwable, java.nio.file.Path] =
      createOsDir(path, None)
  // Creat directory on filesystem writable only to owner
  def createOsDirSec(path: String): Either[Throwable, java.nio.file.Path] = {
    val permissions = PosixFilePermissions.fromString("rwx------")
    val fileAttributes = PosixFilePermissions.asFileAttribute(permissions)
    createOsDir(path, fileAttributes)
  }

  // Write clusters auth data to files with names of clusters
  def writeKeyFiles(map: Map[String, String], path: String): Unit =
    map.foreach( (name, authData) => Try {
      fileWriter(path + "/" + name + ".key") match {
        case Right(fw) => {
          fw.write(authData)
          fw.close()
        }
        case Left(e) => dieLoud(s"Cannot write key file ${name + ".key"} to ${path}: ${e}")
      }
    }.fold(
      e => logger.error(e.getMessage()),
      _ => logger.info(s"${path + "/" + name + ".key"} has been written")
    ))

  logger.info("This utility will convert 'kubectl' configuration file to use auth data in files rather then inline")
  final val kubeDefaultConfigDir = sys.env("HOME") + "/.kube"
  val kubeConfigEnv = Try{ sys.env("KUBECONFIG") }.toEither
  // Take only the first path from KUBECONFIG as kubectl config dir
  // Take default if it's empty
  logger.debug(s"Got environment variable KUBECONFIG: ${kubeConfigEnv.toOption.getOrElse("''")}")
  final val kubeConfigDir = kubeConfigEnv match
    case Right(s) if !s.isEmpty && s != ":" => s.split(':')(0).split('/').toList.dropRight(1).mkString("/")
    case _ => {
      logger.warn(s"KUBECONFIG environment variable set but not valid, will use defaults")
      kubeDefaultConfigDir
    }
  // Create secure directory for keys (0700) at default config dir or die
  // If directory exists, just don't bother
  final val kubeConfigFileName = kubeConfigDir + "/config"
  final val kubeConfigKeysDir = kubeDefaultConfigDir + "/keys"
  val keysDir = createOsDirSec(kubeConfigKeysDir) match {
    case Right(pathsDir) => {
      logger.info(s"Created secure directory for keys: ${pathsDir}")
      pathsDir
    }
    case Left(e) => e match {
      case e: java.nio.file.FileAlreadyExistsException =>
      {
        logger.debug(s"Directory for keys ${kubeConfigKeysDir} already exists")
        Paths.get(kubeConfigKeysDir)
      }
      case e: Exception => dieLoud(s"Error creating directory for keys: ${e}")
    }
  }
  logger.debug(s"Keys dir is set to: ${keysDir}")
  logger.info(s"Using 'kubectl' configuration file: ${kubeConfigFileName}")

  // Reading YAML config file
  val kubeConfigFileReader = fileReader(kubeConfigFileName)
  kubeConfigFileReader match
    case Right(file) =>logger.debug("configuration file read successfull")
    case Left(e) => dieLoud(s"cannot read configuration file ${kubeConfigFileName}: ${e}")
  val kubeConfigJson = kubeConfigFileReader
    .map(fileReader => yaml.parser.parse(fileReader))
    .flatten
    .getOrElse(Json.Null)

  logger.debug(s"Got kubectl configuration file: ${kubeConfigJson.toString.slice(0,50) + "..."}")

  //
  val cursor: HCursor = kubeConfigJson.hcursor
  val clustersArray = cursor.root.downField("clusters").focus.flatMap(_.asArray).getOrElse(Vector.empty)
  clustersArray.isEmpty match
    case false => logger.debug(s"Got this 'clusters' array: ${clustersArray.toString.slice(0,50) + "..."}")
    case true => dieLoud(s"Empty 'clusters' array in ${kubeConfigFileName}")

  val clustersNames = clustersArray.map(n => n.hcursor.get[String]("name"))
    .map(e => e.toOption.getOrElse(""))
  val clustersAuthData = clustersArray.map(n => n.hcursor.downField("cluster")
    .get[String]("certificate-authority-data"))
    .map(e => e.toOption.getOrElse(""))
  val clustersAuthDataMap = clustersNames.iterator.zip(clustersAuthData).toMap
  logger.debug(s"Clusters certificate-authority-data => ${clustersAuthDataMap.toString.slice(0,50) + "..."}")
  // Write keys to files kubeConfigKeysDir
  clustersAuthDataMap.filter((_, v) => v != "") match
    case m: Map[String, String] if m.isEmpty => logger.info(s"No certificate-authority-data for any cluster in kubeconfig file")
    case m: Map[String, String] if ! m.isEmpty => writeKeyFiles(m, kubeConfigKeysDir)

  // Replace certificate-authority-data in kubeconfig with
  //certificate-authority fields with keys' file names as values

}
*/