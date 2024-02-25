// Preload for Scala REPL
// Use with -i option, like this:
// 
// scala -classpath (cat ./lib/kubectl_config_sec_keys.sc-classpath.cache):(pwd)/lib -i ./conf/replPreload.sc
// 
import scala.language.postfixOps
import scala.util.{Try, Success, Failure}

import io.circe._
import io.circe.yaml
import io.circe.yaml._
// import org.apache.logging.log4j.scala.Logging
// import org.apache.logging.log4j.scala.Logger
// import org.apache.logging.log4j.Level
// import org.apache.logging.log4j.{LogManager, Logger}
import com.typesafe.scalalogging._

val logger = Logger("REPL")
logger.info("Loggin in REPL enabled")