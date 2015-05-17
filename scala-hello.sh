#!/bin/bash
L=`stat -f $0`
L=`dirname $L`/lib
cp="`echo ${L}/*.jar | sed 's/ /:/g'`"

exec scala -toolcp ${L} -deprecation -feature -savecompiled -classpath $cp $0 $0 $@

exit
!#

// Say hello to the first argument 

object HelloWorld {
  def main(args: Array[String]) {
    println("Hello, world! " + args.toList)
  }
}
HelloWorld.main(args)