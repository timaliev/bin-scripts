#!/bin/bash
exec scala -savecompiled -classpath "lib/lift-json_2.11-3.0-SNAPSHOT.jar:lib/paranamer-2.5.6.jar" "$0" "$0" "$@"
!#

import scala.io.Source
import net.liftweb.json.DefaultFormats
import net.liftweb.json._

object PrintBibucketIssues {

  	def main(args: Array[String]) {

		val fileContent : String = args.length match {
			case 1 => throw new IllegalArgumentException("No file name given. Use: " + args(0).toString + " [JSON file name]\n")
			case 2 => Source.fromFile(args(1)).getLines.mkString("\n")
			case _ => throw new IllegalArgumentException("Too many arguments. Use: " + args(0).toString + " [JSON file name]\n")
		}

		implicit val formats = DefaultFormats

    	val issues = (parse(fileContent) \ "issues").children

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
}

PrintBibucketIssues.main(args)