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

		val request = par.method match {
			case "GET" => {
				val r = url(par.url)
				debug(s"Set method to GET, URL to ${par.url}, request to ${r}")
				r
			}
			case "POST" => {
				val q = """ "query" """
				val rBody = "------WebKitFormBoundary7GObQBQ4qZnmbeNg\nContent-Disposition: form-data; name=" +
				q +
				"\n\n6452938812\n------WebKitFormBoundary7GObQBQ4qZnmbeNg--\n"
				val r = url(par.url).
					POST.
					// addHeader("Accept-Language", "ru-RU,ru;q=0.8,en-US;q=0.6,en;q=0.4").
					addHeader("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_10_2) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/41.0.2272.118 Safari/537.36").
					// addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8").
					// addHeader("Cache-Control", "max-age=0").
					// addHeader("Referer", "http://www.vestnik-gosreg.ru/publ/fz83/").
					// addHeader("Origin", "http://www.vestnik-gosreg.ru").
					addHeader("Content-Type", "multipart/form-data; boundary=----WebKitFormBoundary7GObQBQ4qZnmbeNg").
					// addCookie("VPSESSID=gvb277n2cgada5pcb7epu8g4f6").
					setBody(rBody)
				debug(s"Set method to POST, URL to ${par.url}, request to ${r}, request body to\n ${rBody}")
				r
			}
			case _ =>{
				val r = url(par.url).setMethod(par.method)
				debug(s"Set method to ${par.method}, URL to ${par.url}, request to ${r}")
				r
			}
		}
		// Http(request OK as.String.utf8)
		Http(request OK as.String.utf8)
	}

	def main(args: Array[String])  {
		val parameters: HttpParameters = args.length match {
			case 1 => new HttpParameters
			case 4 => {
				val urlRegExp = "^https?://(.*)$".r
				urlRegExp.findFirstIn(args(1)) match {
					case Some(_) => "OK"
					case None => throw new IllegalArgumentException(s"Illegal URL '${args(1)}'. Should be:\n\n\thttp(s)://someDomain\n\n")
				}
				args(2) match {
					case "GET" | "POST" | "PUT" | "DELETE" | "HEAD" | "TRACE" => "OK"
					case _ => throw new IllegalArgumentException(s"Illegal method '${args(2)}'. Should be one of:\n\n\tGET|POST|PUT|DELETE|HEAD|TRACE\n\n")
				}
				// args(3) match {

				// }
				HttpParameters(args(1), args(2), args(3)) 
			}
			case _ => throw new IllegalArgumentException("Illegal arguments.\n\n\tUse:\n\n\t\t" + args(0).toString + " [Valid URL] [METHOD (GET|POST etc.)] [ENCODING], or\n\n\t\t" + args(0).toString + " without arguments to GET from example.com\n")
		}
		debug(s"got HTTP parameters ${parameters}")
		val result = httpRequest(parameters)
		result.map { r =>
			println(s"\n\n\t-------- Request Results --------\n\n${r}")
			println("\n\n\t-------- Request Results End--------\n\n")
		}
		result onComplete { case _ => Http.shutdown() }
	}
}

DispatchAHTTP.main(args)