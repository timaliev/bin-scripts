#!/bin/bash
L=`stat -f $0`
L=`dirname $L`/lib
cp="`echo ${L}/*.jar | sed 's/ /:/g'`"

exec scala -toolcp ${L} -deprecation -feature -savecompiled -classpath $cp $0 $0 $@

exit
!#

import scala.language.postfixOps

import scala.io._

import grizzled.slf4j.Logging

object pcapConverter extends Logging {

  def hex2bytes(hex: String): Array[Byte] = hex.replaceAll("[^0-9A-Fa-f]", "").sliding(2, 2).toArray.map(Integer.parseInt(_, 16).toByte)
 
  def bytes2hex(bytes: Array[Byte], sep: Option[String] = None): String = {
    sep match {
    case None =>  bytes.map("%02x".format(_)).mkString
    case _ =>  bytes.map("%02x".format(_)).mkString(sep.get)
    }
    // bytes.foreach(println)
  }

/* Converts PCAP hexadecimal TCP stream representation (such as from Wireshark @see https://www.wireshark.org/) to the List of Bytes
 *
 * File format example:
 * 00000000  47 45 54 20 2f 69 73 73  2f 69 70 20 48 54 54 50 GET /iss /ip HTTP
 * 00000010  2f 31 2e 31 0d 0a 48 6f  73 74 3a 20 66 73 73 70 /1.1..Ho st: fssp
 * ...
 *     00000000  48 54 54 50 2f 31 2e 31  20 32 30 30 20 4f 4b 0d HTTP/1.1  200 OK.
 *     00000010  0a 53 65 72 76 65 72 3a  20 6e 67 69 6e 78 0d 0a .Server:  nginx..
 * ...
*/
  def getHexSubstring(l: List[String]): List[Byte] = {
  	debug(s"In getHexSubstring(l: List[String])")
  	val rex = "^\\s{4}[0-9A-Fa-f]{8}".r // Four spaces followed by eight digits
  	l.flatMap { str =>
  		debug(s"String = [${str}]")
	  	val substr = rex.findFirstIn(str) match {
	  		case Some(s) => str.substring(14,62)
	  		case None => str.substring(10,58)
	  	}
		debug(s"Converted [${str}] to [${substr}] ...")
		val bytesArr = hex2bytes(substr)
		debug(s"... and to ${bytesArr.mkString("[", ":","]")}")
		bytesArr
  	}
  }

	def start(args: Array[String])  {

		val fileContent : List[String] = args.length match {
			case 2 => { info("Reading hexadecimal PCAP dump from File: " + args(1).toString)
						Source.fromFile(args(1)).getLines.toList
			}
			case _ => throw new IllegalArgumentException("Too many or too little arguments.\n\n\tUse:\n\n\t\t" + args(0).toString + " [File name of Hexadecimal text dump from PCAP] to convert it to UTF-8 text\n\n")
		}
		val bytesList = getHexSubstring(fileContent)
		debug(s"Got List of Bytes $bytesList")
		val res = bytesList.map { b =>
			b.toChar
		}
		val bytesArray = bytesList.toArray
		val res2 = new String(bytesArray,"windows-1251")
		println(s"${res2}")
	}
}

pcapConverter.start(args)