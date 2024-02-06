#!/usr/bin/env -S scala-cli shebang
// > using platform native
//> using scala 3
// Use 1.8 JVM for docker package:
// scala-cli package --docker -f scala-cli-test.sc --docker-image-repository scala-os
// > using jvm 1.8
// > using jvm 14
//> using dep "com.lihaoyi::os-lib:0.9.3"

println(s"Current working directory: ${os.pwd}")
println(s"OS: ${System.getProperty("os.name")}")
