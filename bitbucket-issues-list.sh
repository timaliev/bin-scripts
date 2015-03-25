#!/bin/bash
exec scala -deprecation -savecompiled -classpath "lib/lift-json_2.11-3.0-SNAPSHOT.jar:lib/paranamer-2.5.6.jar:lib/commons-codec-1.9.jar:lib/grizzled-slf4j_2.11-1.0.2.jar:lib/slf4j-api-1.7.10.jar:lib/slf4j-simple-1.7.10.jar" "$0" "$0" "$@"
!#

import scala.io.Source
import scala.io.StdIn
import java.util.Calendar
import java.net.URL
import org.apache.commons.codec.binary.Base64

import net.liftweb.json.DefaultFormats
import net.liftweb.json._

import grizzled.slf4j.Logging

import scala.util.{Success, Failure}
import scala.concurrent._
import ExecutionContext.Implicits._

object HttpBasicAuth {
   val BASIC = "Basic"
   val AUTHORIZATION = "Authorization"

   def encodeCredentials(username: String, password: String): String = {
      new String(Base64.encodeBase64String((username + ":" + password).getBytes))
   }

   def getHeader(username: String, password: String): String = 
      BASIC + " " + encodeCredentials(username, password)
}

object PrintBibucketIssues extends Logging {

	def printV1Issues(fileContent : String) {

		val issues = parse(fileContent).children

		// println(s"\nGot content:\n\n ${fileContent}")

	    val headers = "id,kind,status,priority,title,content,assignee,reporter,created_on,milestone,version,updated_on"
	    println(headers)

		implicit val formats = DefaultFormats

		for (i <- issues) { // read V1 JSON structure for each issue and print values out as CSV string
	    	val assignee = (i \ "responsible" \ "display_name") match {
	    		case JNothing => ""
	    		case JNull => ""
	    		case e => e.extract[String]
	    	}
	    	val component = (i \ "metadata" \ "component") match {
	    		case JNothing => ""
	    		case JNull => ""
	    		case e => e.extract[String]
	    	}
	    	val content = (i \ "content") match {
	    		case JNothing => ""
	    		case JNull => ""
	    		case e => e.extract[String].replace('"', '`')
	    	}
	    	val content_updated_on = (i \ "utc_last_updated") match {
	    		case JNothing => ""
	    		case JNull => ""
	    		case e => e.extract[String].split("T")(0).toString
	    	}
	    	val created_on = (i \ "created_on") match {
	    		case JNothing => ""
	    		case JNull => ""
	    		case e => e.extract[String].split("T")(0).toString
	    	}
	    	val edited_on = (i \ "edited_on") match {
	    		case JNothing => ""
	    		case JNull => ""
	    		case e => e.extract[String].split("T")(0).toString
	    	}
	    	val id = (i \ "local_id") match {
	    		case JNothing => ""
	    		case JNull => ""
	    		case e => e.extract[Int]
	    	}
	    	val kind = (i \ "metadata" \ "kind") match {
	    		case JNothing => ""
	    		case JNull => ""
	    		case e => e.extract[String]
	    	}
	    	val milestone = (i \ "metadata" \ "milestone") match {
	    		case JNothing => ""
	    		case JNull => ""
	    		case e => e.extract[String]
	    	}
	    	val priority = (i \ "priority") match {
	    		case JNothing => ""
	    		case JNull => ""
	    		case e => e.extract[String]
	    	}
	    	val reporter = (i \ "reported_by" \ "display_name") match {
	    		case JNothing => ""
	    		case JNull => ""
	    		case e => e.extract[String]
	    	}
	    	val status = (i \ "status") match {
	    		case JNothing => ""
	    		case JNull => ""
	    		case e => e.extract[String]
	    	}
	    	val title = (i \ "title") match {
	    		case JNothing => ""
	    		case JNull => ""
	    		case e => e.extract[String].replace('"', '`')
	    	}
	    	val updated_on = (i \ "utc_last_updated") match {
	    		case JNothing => ""
	    		case JNull => ""
	    		case e => e.extract[String].split("T")(0).toString
	    	}
	    	val version = (i \ "metadata" \ "version") match {
	    		case JNothing => ""
	    		case JNull => ""
	    		case e => e.extract[String]
	    	}
	       	// val watchers = (i \ "watchers").extract[List[String]]
	    	// val voters = (i \ "voters").extract[List[String]]

	    	val outStr = id + "," + kind + "," + status + "," + priority + "," + '"' + title + '"' + "," + '"' + content + '"' + "," + assignee + "," +
	    		reporter + "," + created_on + "," + milestone + "," + version + "," + updated_on

			println(outStr)
		}

	}

	def printV2Issues(fileContent : String) {

		val issues = (parse(fileContent) \ "issues").children

		// println (issues.mkString("\n\n"))

	    val headers = "id,kind,status,priority,title,content,assignee,reporter,created_on,milestone,version,updated_on"
	    println(headers)

		implicit val formats = DefaultFormats

		for (i <- issues) { // read V2 JSON structure for each issue and print values out as CSV string
	    	val assignee = (i \ "assignee").extract[String] match {
	    		case null => ""
	    		case e => e
	    	}
	    	val component = (i \ "component").extract[String] match {
	    		case null => ""
	    		case e => e
	    	}
	    	val content = (i \ "content").extract[String] match {
	    		case null => ""
	    		case e => e.replace('"', '`')
	    	}
	    	val content_updated_on = (i \ "content_updated_on").extract[String] match {
	    		case null => ""
	    		case e => e.split("T")(0).toString
	    	}
	    	val created_on = (i \ "created_on").extract[String].split("T")(0).toString
	    	val edited_on = (i \ "edited_on").extract[String] match {
	    		case null => ""
	    		case e => e.split("T")(0).toString
	    	}
	    	val id = (i \ "id").extract[Int]
	    	val kind = (i \ "kind").extract[String]
	    	val milestone = (i \ "milestone").extract[String] match {
	    		case null => ""
	    		case e => e
	    	}
	    	val priority = (i \ "priority").extract[String]
	    	val reporter = (i \ "reporter").extract[String] match {
	    		case null => ""
	    		case e => e
	    	}
	    	val status = (i \ "status").extract[String]
	    	val title = (i \ "title").extract[String].replace('"', '`')
	    	val updated_on = (i \ "updated_on").extract[String].split("T")(0).toString
	    	val version = (i \ "version").extract[String] match {
	    		case null => ""
	    		case e => e
	    	}
	       	// val watchers = (i \ "watchers").extract[List[String]]
	    	// val voters = (i \ "voters").extract[List[String]]

	    	val outStr = id + "," + kind + "," + status + "," + priority + "," + '"' + title + '"' + "," + '"' + content + '"' + "," + assignee + "," +
	    		reporter + "," + created_on + "," + milestone + "," + version + "," + updated_on

			println(outStr)
		}
	}

  	def main(args: Array[String])  {

		implicit val formats = DefaultFormats
		val PAGING : Int = 50
		val REPO = "semanticplatform/ontologyeditor/"
		var API_VERSION: Int = 2

		logger.info(Calendar.getInstance().getTime + ": Hello, world")

		def fetchNextPage(bitbucketRepository: String, user: String, pass: String, start: Int = 0, limit: Int = PAGING, api: Int = 1): List[String] = {

			val bitbucketApiUrl = api match {
				case 1 => "https://api.bitbucket.org/1.0/"
				case 2 => "https://api.bitbucket.org/2.0/"
				case n => throw new Exception(s"Unknown API version: ${n}")
			}

			val bitbucketRepositoriesUrl = bitbucketApiUrl + "repositories/"
			val bitbucketIssuesOptions = "limit=" + limit.toString + "&sort=local_id&start=" + start.toString
			val bitbucketIssuesUrl = bitbucketRepositoriesUrl + bitbucketRepository + "issues"
			val bitbucketIssuesUrlOptions = bitbucketIssuesUrl + "?" + bitbucketIssuesOptions

			logger.info(Calendar.getInstance().getTime + s": Fetching JSON from URL: ${bitbucketIssuesUrlOptions} ...")
			val connection = new URL(bitbucketIssuesUrlOptions).openConnection
			connection.setRequestProperty(HttpBasicAuth.AUTHORIZATION, HttpBasicAuth.getHeader(user, pass))
			Source.fromInputStream(connection.getInputStream).getLines.toList
		}

		def fetchBitbacketIssues(rep: String, user: String, pass: String, page: Int = 0, results: List[JValue] = Nil): List[JValue] =
			fetchNextPage(rep, user, pass, page * PAGING) flatMap { result =>
				val issuesNumber = (parse(result) \ "count").extract[Int]
				val issues = parse(result) \ "issues"
				if (issuesNumber > (page + 1) * PAGING)
					fetchBitbacketIssues(rep, user, pass, page + 1, issues :: results)
				else
					issues :: results
			}


		val fileContent : String = args.length match {
			case 1 => { API_VERSION = 1
						
						logger.info(Calendar.getInstance().getTime + ": Getting JSON from BitBucket website.")
						val username = StdIn.readLine(s"Please, enter your BitBucket account user name for [${REPO}] repository: ")
						print("Please, enter your BitBucket password: ")
						print(Console.INVISIBLE)
						val password = StdIn.readLine
						print(Console.RESET)
						logger.info(Calendar.getInstance().getTime + ": Fetching, please wait...")

						val r : List[JValue] = fetchBitbacketIssues(REPO, username, password).reverse
						pretty(render((r.head /: r.tail)(_ merge _)))
			}
			case 2 => { logger.info(Calendar.getInstance().getTime + ": Reading JSON from File: " + args(1).toString + ".")
						Source.fromFile(args(1)).getLines.mkString
			}
			case _ => throw new IllegalArgumentException("Too many arguments.\n\n\tUse:\n\n\t\t" + args(0).toString + " [JSON file name] to read issues from file, or\n\n\t\t" + args(0).toString + " without arguments to read issues from BitBucket's RESTfull API\n")
		}

		API_VERSION match {
			case 1 => printV1Issues(fileContent)
			case 2 => printV2Issues(fileContent)
			case _ => throw new Exception("Unknown error")
		} 

  	}

}

PrintBibucketIssues.main(args)