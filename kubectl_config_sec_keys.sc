#!/usr/bin/env bash
# (SPDX-License-Identifier: MIT)
# Based on this note: http://dev.bizo.com/2009/07/dependency-management-for-scala-scripts-using-ivy.html
#
# Use -ivy parameter to invoke ivy and download/update libraries
# Alternativly delete classpath cache file in ./lib/${scriptname}-classpath.cache
# It will be recreated on next run and libraries updated
#
set -Eeuo pipefail
L=$(stat -f $0)
IVY=$@
scriptname=$(basename $L)
tempdir=$(mktemp -d /tmp/${scriptname}.XXXXXXXXXX)
tempfile=$(mktemp ${tempdir}/${scriptname}.XXXXXXXXXX)
trap cleanup SIGINT SIGTERM ERR EXIT

cleanup() {
  trap - SIGINT SIGTERM ERR EXIT
  set +Eeu
  # script cleanup here
  #echo -e "${FAINT}Exiting, script cleanup.${NOFORMAT}"
  rm -rf "${tempdir}" "${tempfile}"
}

die() {
  local msg=$1
  local code=${2-127} # default exit status 127
  echo -e "$msg"
  exit "$code"
}

L=$(dirname $L)/lib
cp="$(echo ${L}/*.jar | sed 's/ /:/g')"

if [ "${IVY}" = "-ivy" ] || [ ! -f ${L}/${scriptname}-classpath.cache ]; then
  ver="2.5.2"
  ivy_version="apache-ivy-"${ver}

  if [ ! -f ${L}/ivy.jar ]; then
    wget -nv -c -O - https://dlcdn.apache.org/ant/ivy/${ver}/${ivy_version}-bin.tar.gz | tar -C ${tempdir} -zxf - \
      && cp ${tempdir}/${ivy_version}/ivy-${ver}.jar ${L}/ivy.jar \
      || die "${BOLD}${RED}ERROR: cannot get Ivy from apache web-site${NOFORMAT}"
  fi

  [ -n "$(command -v java)" ] && \
    java -jar ${L}/ivy.jar -ivy $(dirname ${L})/conf/ivy-${scriptname}.xml \
      -cachepath ${tempfile} || \
      die "${BOLD}${RED}ERROR: No java interpreter or execution error${NOFORMAT}"

  classpath=$(cat ${tempfile} | tr -d "\n\r")
  echo ${classpath} > ${L}/${scriptname}-classpath.cache
fi

if [ -n "$(command -v scala)" ]; then
  classpath=$(cat ${L}/${scriptname}-classpath.cache)
  cleanup
  exec scala -deprecation -feature -savecompiled \
    -classpath ${classpath}:${cp}:${L} $0 $0 "$@" || \
    die "${BOLD}${RED}ERROR: No scala interpreter or execution error${NOFORMAT}"
fi

exit
!#

import scala.language.postfixOps
import java.lang.System // Read more at: https://studentprojectcode.com/blog/how-to-get-and-set-environment-variables-in-scala

// File reader
import java.io.FileReader
import java.io.FileWriter
import java.io.File
import java.nio.file.{Path, Paths, Files, FileAlreadyExistsException}
import java.nio.file.attribute.{FileAttribute, PosixFilePermission, PosixFilePermissions}
// import scala.io.Source
// import scala.io.StdIn
// import java.util.Calendar
// import java.net.URL
// import org.apache.commons.codec.binary.Base64

// Yaml librarie
// import org.yaml.snakeyaml.Yaml
import io.circe._
import io.circe.yaml
import io.circe.yaml._
// import io.circe._, io.circe.syntax._, io.circe.Parser
// import cats.syntax.eq._, cats.instances.string._

// import net.liftweb.json.DefaultFormats
// import net.liftweb.json._

// import grizzled.slf4j.Logging
import org.apache.logging.log4j.scala.Logging
// import org.apache.logging.log4j.scala.Logger

import scala.util.{Try, Success, Failure}
// import scala.concurrent.{Await, Future}
// import scala.concurrent.duration._
// import scala.collection._

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
  // If directory exist, just don't bother
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
  val changedJson = cursor.root.downField("clusters").

}