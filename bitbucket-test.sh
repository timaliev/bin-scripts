#!/bin/bash
exec scala -savecompiled -classpath "lib/lift-json_2.11-3.0-SNAPSHOT.jar:lib/commons-codec-1.9.jar:lib/httpclient-4.4.jar:lib/commons-logging-1.2.jar:lib/logback-core-1.1.2.jar:lib/slf4j-1.7.10/slf4j-api-1.7.10.jar:lib/slf4j-1.7.10/slf4j-simple-1.7.10.jar" "$0" "$0" "$@"
!#

import scala.io.Source
import java.net.URL
import sys.process._

// For Lift JSON parser
import net.liftweb.json.DefaultFormats
import net.liftweb.json._
import org.apache.commons.codec.binary.Base64

object HttpBasicAuth {
   val BASIC = "Basic"
   val AUTHORIZATION = "Authorization"

   def encodeCredentials(username: String, password: String): String = {
      new String(Base64.encodeBase64String((username + ":" + password).getBytes))
   }

   def getHeader(username: String, password: String): String = 
      BASIC + " " + encodeCredentials(username, password)
}

object PrintBibucketIssues {

  	def main(args: Array[String]) {

		implicit val formats = DefaultFormats

		val fileContent : String = args.length match {
			case 1 => { val bitbucketRepositorySPEditor = "semanticplatform/ontologyeditor/"
						val bitbucketApiUrlv1 = "https://api.bitbucket.org/1.0/"
						val bitbucketRepositoriesUrlv1 = bitbucketApiUrlv1 + "repositories/"
						val bitbucketIssuesOptions = "limit=50&sort=local_id"
						val bitbucketIssuesUrl = bitbucketRepositoriesUrlv1 + bitbucketRepositorySPEditor + "issues" + "?" + bitbucketIssuesOptions

						println ("Reading JSON from URL: " + bitbucketIssuesUrl + " ...")
						val username = readLine("Please, enter your BitBucket user name: ")
						print("Please, enter your BitBucket password: ")
						print(Console.INVISIBLE)
						val password = readLine
						print(Console.RESET)

						val connection = new URL(bitbucketIssuesUrl).openConnection
						connection.setRequestProperty(HttpBasicAuth.AUTHORIZATION, HttpBasicAuth.getHeader(username, password))
						Source.fromInputStream(connection.getInputStream).getLines.mkString("\n")
			}
			case 2 => { println ("Reading JSON from File: " + args(1).toString + " ...")
						Source.fromFile(args(1)).getLines.mkString("\n")
			}
			case _ => throw new IllegalArgumentException("Too many arguments.\n\n\tUse:\n\n\t\t" + args(0).toString + " [JSON file name] to read issues from file, or\n\n\t\t" + args(0).toString + " without arguments to read issues from BitBucket's RESTfull API\n")
		}

		println (fileContent)
		println ("\n----------\n")

    	val issues = (parse(fileContent) \ "issues")

    	// val issuesList = issues.values
    	// val bbIssues = for(is <- issues) yield is.extract[BBIssue]
	    // val headers = "id,kind,status,priority,title,content,assignee,reporter,created_on,milestone,version,updated_on"
	    // println(headers)

	    val testJson = pretty(render(issues))
	    println(s"\n ${testJson}\n\n\n")

		val procs = ("ps auxw" #| "grep java").!!.trim

		println(procs)
/*
    	for (i <- issues) {
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
	       	val watchers = (i \ "watchers").extract[List[String]]
	    	val voters = (i \ "voters").extract[List[String]]

	    	val outStr = id + "," + kind + "," + status + "," + priority + "," + '"' + title + '"' + "," + '"' + content + '"' + "," + assignee + "," +
	    		reporter + "," + created_on + "," + milestone + "," + version + "," + updated_on

    		println(outStr)
    	} */
  	}
}

PrintBibucketIssues.main(args)

