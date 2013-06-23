/*
 * MnoClassifier learns MSISDN-Operator combinations to afterwards predict Operators.
 * Copyright (C) 2013 MACH Connectivity GmbH
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
import sbt._
import sbt.Keys._
import sbtrelease._
import sbtrelease.ReleasePlugin._
import sbtrelease.ReleasePlugin.ReleaseKeys._
import sbtrelease.ReleaseStateTransformations._
import xerial.sbt.Pack._
import com.untyped.sbtjs.Plugin._
import com.untyped.sbtless.Plugin._

object Build extends sbt.Build {
  lazy val projectSettings = Seq(
    name := "mnoclassifier",
    organization := "com.github.mkroli",
    scalaVersion := "2.10.1",
    scalacOptions ++= Seq("-feature", "-unchecked"))

  lazy val projectDependencies = Seq(
    libraryDependencies ++= Seq(
      "com.typesafe" % "config" % "1.0.1",
      "com.twitter" %% "chill" % "0.2.3",
      "org.json4s" %% "json4s-native" % "3.2.4",
      "com.jsuereth" %% "scala-arm" % "1.3",
      "nl.grons" %% "metrics-scala" % "2.2.0",
      "ch.qos.logback" % "logback-classic" % "1.0.13",
      "com.typesafe.akka" %% "akka-actor" % "2.1.4",
      "net.databinder" %% "unfiltered-netty" % "0.6.8",
      "net.databinder" %% "unfiltered-netty-server" % "0.6.8",
      "net.databinder" %% "unfiltered-netty-websockets" % "0.6.8"))

  lazy val projectClasspath = Seq(
    unmanagedSourceDirectories in Compile <++= baseDirectory { base =>
      Seq(
        base / "src/main/resources",
        base / "src/pack/etc")
    },
    unmanagedClasspath in Runtime <+= baseDirectory map { base =>
      Attributed.blank(base / "src/pack/etc")
    })

  lazy val projectPackSettings = Seq(
    packMain := Map(
      "start-mnoc" -> "com.github.mkroli.mnoclassifier.service.Boot",
      "stop-mnoc" -> "com.github.mkroli.mnoclassifier.service.Shutdown"),
    packJvmOpts := Map(
      "start-mnoc" -> Seq(
        "-Dlogback.configurationFile=${PROG_HOME}/etc/logback.xml",
        "-Dcom.sun.management.jmxremote.port=30750",
        "-Dcom.sun.management.jmxremote.authenticate=false",
        "-Dcom.sun.management.jmxremote.ssl=false"),
      "stop-mnoc" -> Seq(
        "-Djmx.port=30750")),
    packExtraClasspath := Map(
      "start-mnoc" -> Seq("${PROG_HOME}/etc"),
      "stop-mnoc" -> Seq("${PROG_HOME}/etc")))

  lazy val projectJsSettings = Seq(
    JsKeys.variableRenamingPolicy in Compile := VariableRenamingPolicy.OFF,
    compile in Compile <<= compile in Compile dependsOn (JsKeys.js in Compile),
    sourceDirectory in (Compile, JsKeys.js) <<= (sourceDirectory in Compile) { d =>
      d / "javascript"
    },
    resourceManaged in (Compile, JsKeys.js) <<= (resourceManaged in Compile) { d =>
      d / "com/github/mkroli/mnoclassifier/static/js"
    },
    resourceGenerators in Compile <+= (JsKeys.js in Compile),
    includeFilter in (Compile, JsKeys.js) := "*.jsm")

  lazy val projectLessSettings = Seq(
    compile in Compile <<= compile in Compile dependsOn (LessKeys.less in Compile),
    sourceDirectory in (Compile, LessKeys.less) <<= (sourceDirectory in Compile) { d =>
      d / "less"
    },
    resourceManaged in (Compile, LessKeys.less) <<= (resourceManaged in Compile) { d =>
      d / "com/github/mkroli/mnoclassifier/static/css"
    },
    resourceGenerators in Compile <+= (LessKeys.less in Compile))

  lazy val projectPomSettings = Seq(
    pomExtra :=
      <licenses>
        <license>
          <name>GNU General Public License (GPL) version 2.0</name>
          <url>http://www.gnu.org/licenses/gpl2.html</url>
          <distribution>repo</distribution>
        </license>
      </licenses>)

  lazy val projectReleaseSettings = Seq(
    releaseProcess := Seq[ReleaseStep](
      checkSnapshotDependencies,
      inquireVersions,
      runTest,
      setReleaseVersion,
      commitReleaseVersion,
      tagRelease,
      setNextVersion,
      commitNextVersion))

  lazy val mnoclassifier = Project(
    id = "mnoclassifier",
    base = file("."),
    settings = Defaults.defaultSettings ++
      projectSettings ++
      projectDependencies ++
      projectClasspath ++
      packSettings ++
      projectPackSettings ++
      jsSettings ++
      projectJsSettings ++
      lessSettings ++
      projectLessSettings ++
      projectPomSettings ++
      releaseSettings ++
      projectReleaseSettings)
}
