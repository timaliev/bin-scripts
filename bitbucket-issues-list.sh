#!/bin/bash
exec scala -deprecation -savecompiled -classpath "lib/lift-json_2.11-3.0-SNAPSHOT.jar:lib/paranamer-2.5.6.jar:lib/commons-codec-1.9.jar" "$0" "$0" "$@"
!#

import scala.io.Source
import scala.io.StdIn

import java.net.URL
import org.apache.commons.codec.binary.Base64

import net.liftweb.json.DefaultFormats
import net.liftweb.json._

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
		var API_VERSION = 2

		val fileContent : String = args.length match {
			case 1 => { API_VERSION = 1
						val bitbucketRepositorySPEditor = "semanticplatform/ontologyeditor/"
						val bitbucketApiUrlv1 = "https://api.bitbucket.org/1.0/"
						val bitbucketRepositoriesUrlv1 = bitbucketApiUrlv1 + "repositories/"
						val bitbucketIssuesOptions = "limit=50&sort=local_id"
						val bitbucketIssuesUrl = bitbucketRepositoriesUrlv1 + bitbucketRepositorySPEditor + "issues" + "?" + bitbucketIssuesOptions

						println ("Reading JSON from URL: " + bitbucketIssuesUrl + " ...\n\n")
						val username = StdIn.readLine("Please, enter your BitBucket user name: ")
						print("Please, enter your BitBucket password: ")
						print(Console.INVISIBLE)
						val password = StdIn.readLine
						print(Console.RESET)

						val connection = new URL(bitbucketIssuesUrl).openConnection
						connection.setRequestProperty(HttpBasicAuth.AUTHORIZATION, HttpBasicAuth.getHeader(username, password))
						Source.fromInputStream(connection.getInputStream).getLines.mkString("\n")
			}
			case 2 => { println ("Reading JSON from File: " + args(1).toString + " ...\n\n")
						Source.fromFile(args(1)).getLines.mkString("\n")
			}
			case _ => throw new IllegalArgumentException("Too many arguments.\n\n\tUse:\n\n\t\t" + args(0).toString + " [JSON file name] to read issues from file, or\n\n\t\t" + args(0).toString + " without arguments to read issues from BitBucket's RESTfull API\n")
		}

		API_VERSION match {
			case 1 => println (parse(fileContent).children.mkString("\n\n"))
			case 2 => {
				    	val issues = (parse(fileContent) \ "issues").children

				    	// println (issues.mkString("\n\n"))

					    val headers = "id,kind,status,priority,title,content,assignee,reporter,created_on,milestone,version,updated_on"
					    println(headers)

				    	for (i <- issues) { // read JSON structure for each issue and print values out as CSV string
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
				    	}
			}
			case _ => throw new Exception("Unknown error")
		} 

  	}

}

PrintBibucketIssues.main(args)