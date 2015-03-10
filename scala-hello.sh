#!/bin/bash
exec scala "$0" "$@"
!#

// Say hello to the first argument 

object HelloWorld {
  def main(args: Array[String]) {
    println("Hello, world! " + args.toList)
  }
}
HelloWorld.main(args)