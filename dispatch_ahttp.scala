#!/bin/bash
#exec scala -Ylog-classpath -toolcp "lib" -deprecation -feature -savecompiled -classpath "lib/lift-json_2.11-3.0-SNAPSHOT.jar:lib/paranamer-2.5.6.jar:lib/commons-codec-1.9.jar:lib/grizzled-slf4j_2.11-1.0.2.jar:lib/slf4j-api-1.7.10.jar:lib/slf4j-simple-1.7.10.jar:lib/httpclient-4.4.jar:lib/dispatch-core_2.11-0.11.2.jar" "$0" "$0" "$@"
exec scala -toolcp "lib" -deprecation -feature -savecompiled -classpath "lib/lift-json_2.11-3.0-SNAPSHOT.jar:lib/paranamer-2.5.6.jar:lib/commons-codec-1.9.jar:lib/grizzled-slf4j_2.11-1.0.2.jar:lib/slf4j-api-1.7.10.jar:lib/slf4j-simple-1.7.10.jar:lib/async-http-client-1.8.15.jar:lib/netty-3.2.10.Final.jar:lib/dispatch-core_2.10-0.11.2.jar" "$0" "$0" "$@"
!#

import scala.language.postfixOps
import scala.io.Source
import scala.io.StdIn
import java.util.Calendar
// import java.net.URL
// import org.apache.commons.codec.binary.Base64

import net.liftweb.json.DefaultFormats
import net.liftweb.json._

import grizzled.slf4j.Logging

import scala.util.{Success, Failure}
import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import scala.collection._

// import scala.concurrent.ExecutionContext.Implicits.global

import dispatch._, Defaults._

case class HttpParameters(url: String = "http://example.com", method: String= "GET", encoding: String = "default")

object DispatchAHTTP extends Logging {

	def httpRequest(par: HttpParameters): Future[String] = {
		val svc = url(par.url)
		Http(svc OK as.String)	
	}

	def main(args: Array[String])  {
		val parameters: HttpParameters = args.length match {
			case 1 => new HttpParameters
			case 4 => {
				HttpParameters(args(1), args(2), args(3)) 
			}
			case _ => throw new IllegalArgumentException("Illegal arguments.\n\n\tUse:\n\n\t\t" + args(0).toString + " [Valid URL] [METHOD (GET|POST etc.)] [ENCODING], or\n\n\t\t" + args(0).toString + " without arguments to GET from example.com\n")
		}
		debug(s"got HTTP parameters ${parameters}")
		val result = httpRequest(parameters)
		result.map { r =>
			println(s"\n\n\t-------- Request Results --------\n\n${r}")
		}
	}
}

DispatchAHTTP.main(args)