name := "jsvc-wrapper"

version := "1.0"

scalaVersion := "2.11.8"

libraryDependencies += "commons-daemon" % "commons-daemon" % "1.0.15"

libraryDependencies += "com.typesafe.scala-logging" %% "scala-logging" % "3.1.0"

libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.1.2"

libraryDependencies += "org.codehaus.janino" % "janino" % "2.7.7"

libraryDependencies += "net.logstash.logback" % "logstash-logback-encoder" % "3.0"

libraryDependencies += "org.apache.sshd" % "sshd-core" % "0.13.0"

libraryDependencies += "jline" % "jline" % "2.12"

assemblyJarName in assembly := "jsvc-demo-app.jar"


