// Preload for Scala REPL
// Use with -i option, like this:
// 
// scala -classpath (cat ./lib/kubectl_config_sec_keys.sc-classpath.cache):(pwd)/lib -i ./conf/replPreload.sc
// 
import scala.language.postfixOps
import scala.util.{Try, Success, Failure}
import java.lang.System
import java.io.FileReader
import java.io.FileWriter
import java.io.File
import java.nio.file.{Path, Paths, Files}
import java.nio.file.attribute.{FileAttribute, PosixFilePermission, PosixFilePermissions}
import io.circe._
import io.circe.yaml
import io.circe.yaml._
// import org.apache.logging.log4j.scala.Logging
// import org.apache.logging.log4j.scala.Logger
// import org.apache.logging.log4j.Level
import org.apache.logging.log4j.{LogManager, Logger}

val logger = LogManager.getLogger("REPL")
logger.info("Loggin in REPL enabled")

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
  val kubeDefaultConfigDir = sys.env("HOME") + "/.kube"
  val kubeConfigEnv = Try{ sys.env("KUBECONFIG") }.toEither
  // Take only the first path from KUBECONFIG as kubectl config dir
  // Take default if it's empty
  logger.debug(s"Got environment variable KUBECONFIG: ${kubeConfigEnv.toOption.getOrElse("''")}")
  val kubeConfigDir = kubeConfigEnv match
    case Right(s) if !s.isEmpty && s != ":" => s.split(':')(0).split('/').toList.dropRight(1).mkString("/")
    case _ => {
      logger.warn(s"KUBECONFIG environment variable set but not valid, will use defaults")
      kubeDefaultConfigDir
    }
  // Create secure directory for keys (0700) at default config dir or die
  // If directory exist, just don't bother
  val kubeConfigFileName = kubeConfigDir + "/config"
  val kubeConfigKeysDir = kubeDefaultConfigDir + "/keys"
  val keysDir = createOsDirSec(kubeConfigKeysDir) match {
    case Right(pathsDir) => {
      logger.info(s"Created secure directory for keys: ${d}")
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
  val clustersArray = cursor.root.downField("clusters")
    .focus
    .flatMap(_.asArray)
    .getOrElse(Vector.empty)
  clustersArray.isEmpty match
    case false => logger.debug(s"Got this 'clusters' array: ${clustersArray.toString.slice(0,50) + "..."}")
    case true => dieLoud(s"Empty 'clusters' array in ${kubeConfigFileName}")

  val clustersNames = clustersArray.map(n => n.hcursor.get[String]("name"))
    .map(e => e.toOption.getOrElse(""))
  val clustersAuthData = clustersArray.map(n => n.hcursor.downField("cluster")
    .get[String]("certificate-authority-data"))
    .map(e => e.toOption.getOrElse(""))
    .filter(_ != "")
  // if clustersAuthData.

  val clustersAuthDataMap = clustersNames.iterator.zip(clustersAuthData).toMap
  logger.debug(s"Clusters certificate-authority-data => ${clustersAuthDataMap.toString.slice(0,50) + "..."}")