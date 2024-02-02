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