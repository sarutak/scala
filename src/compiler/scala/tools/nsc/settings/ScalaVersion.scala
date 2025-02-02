/*
 * Scala (https://www.scala-lang.org)
 *
 * Copyright EPFL and Lightbend, Inc.
 *
 * Licensed under Apache License 2.0
 * (http://www.apache.org/licenses/LICENSE-2.0).
 *
 * See the NOTICE file distributed with this work for
 * additional information regarding copyright ownership.
 */

// $Id$

package scala
package tools.nsc.settings

/**
 * Represents a single Scala version in a manner that
 * supports easy comparison and sorting.
 */
sealed abstract class ScalaVersion extends Ordered[ScalaVersion] {
  def unparse: String
  def versionString: String = unparse
}

/** A scala version that sorts higher than all actual versions. */
sealed abstract class MaximalScalaVersion extends ScalaVersion {
  def compare(that: ScalaVersion): Int = that match {
    case _: MaximalScalaVersion => 0
    case _ => 1
  }
}

/** If "no version" is specified, assume a maximal version, "the latest". */
case object NoScalaVersion extends MaximalScalaVersion {
  def unparse = "none"
}

/** Same as `NoScalaVersion` but with a different toString */
case object Scala3Cross extends MaximalScalaVersion {
  def unparse = "3-cross"
}

/**
 * A specific Scala version, not one of the magic min/max versions. An SpecificScalaVersion
 * may or may not be a released version - i.e. this same class is used to represent
 * final, release candidate, milestone, and development builds. The build argument is used
 * to segregate builds
 */
case class SpecificScalaVersion(major: Int, minor: Int, rev: Int, build: ScalaBuild) extends ScalaVersion {
  def unparse = s"${major}.${minor}.${rev}${build.unparse}"
  override def versionString = s"${major}.${minor}.${rev}"

  def compare(that: ScalaVersion): Int =  that match {
    case SpecificScalaVersion(thatMajor, thatMinor, thatRev, thatBuild) =>
      // this could be done more cleanly by importing scala.math.Ordering.Implicits, but we have to do these
      // comparisons a lot so I'm using brute force direct style code
      if (major < thatMajor) -1
      else if (major > thatMajor) 1
      else if (minor < thatMinor) -1
      else if (minor > thatMinor) 1
      else if (rev < thatRev) -1
      else if (rev > thatRev) 1
      else build compare thatBuild
    case AnyScalaVersion => 1
    case _: MaximalScalaVersion => -1
  }
}

/**
 * A Scala version that sorts lower than all actual versions
 */
case object AnyScalaVersion extends ScalaVersion {
  def unparse = "any"

  def compare(that: ScalaVersion): Int = that match {
    case AnyScalaVersion => 0
    case _ => -1
  }
}

/**
 * Factory methods for producing ScalaVersions
 */
object ScalaVersion {
  private val dot   = """\."""
  private val dash  = "-"
  private val vchar = """\d""" //"[^-+.]"
  private val vpat  = s"(?s)($vchar+)(?:$dot($vchar+)(?:$dot($vchar+))?)?(?:$dash(.+))?".r
  private val rcpat = """(?i)rc(\d*)""".r
  private val mspat = """(?i)m(\d*)""".r

  def apply(versionString: String, errorHandler: String => Unit): ScalaVersion = {
    def error() = errorHandler(
      s"Bad version (${versionString}) not major[.minor[.revision]][-suffix]"
    )

    def toInt(s: String) = s match {
      case null | "" => 0
      case _         => s.toInt
    }

    def toBuild(s: String) = s match {
      case null | "FINAL" => Final
      case rcpat(i)       => RC(toInt(i))
      case mspat(i)       => Milestone(toInt(i))
      case _ /* | "" */   => Development(s)
    }

    versionString match {
      case "none" | "" => NoScalaVersion
      case "3-cross"   => Scala3Cross
      case "any"       => AnyScalaVersion
      case vpat(majorS, minorS, revS, buildS) =>
        SpecificScalaVersion(toInt(majorS), toInt(minorS), toInt(revS), toBuild(buildS))
      case _           => error(); AnyScalaVersion
    }
  }

  def apply(versionString: String): ScalaVersion =
      apply(versionString, msg => throw new NumberFormatException(msg))

  /**
   * The version of the compiler running now
   */
  val current = apply(util.Properties.versionNumberString)

  implicit class `not in Ordered`(private val v: ScalaVersion) extends AnyVal {
    def min(other: ScalaVersion): ScalaVersion = if (v <= other) v else other
    def max(other: ScalaVersion): ScalaVersion = if (v >= other) v else other
  }
}

/**
 * Represents the data after the dash in major.minor.rev-build
 */
abstract class ScalaBuild extends Ordered[ScalaBuild] {
  /**
   * Return a version of this build information that can be parsed back into the
   * same ScalaBuild
   */
  def unparse: String
}
/**
 * A development, test, integration, snapshot or other "unofficial" build
 */
case class Development(id: String) extends ScalaBuild {
  def unparse = s"-${id}"

  def compare(that: ScalaBuild) = that match {
    // sorting two development builds based on id is reasonably valid for two versions created with the same schema
    // otherwise it's not correct, but since it's impossible to put a total ordering on development build versions
    // this is a pragmatic compromise
    case Development(thatId) => id compare thatId
    // assume a development build is newer than anything else, that's not really true, but good luck
    // mapping development build versions to other build types
    case _ => 1
  }
}
/**
 * A final final
 */
case object Final extends ScalaBuild {
  def unparse = ""

  def compare(that: ScalaBuild) = that match {
    case Final => 0
    // a final is newer than anything other than a development build or another final
    case Development(_) => -1
    case _ => 1
  }
}

/**
 * A candidate for final release
 */
case class RC(n: Int) extends ScalaBuild {
  def unparse = s"-RC${n}"

  def compare(that: ScalaBuild) = that match {
    // compare two rcs based on their RC numbers
    case RC(thatN) => n - thatN
    // an rc is older than anything other than a milestone or another rc
    case Milestone(_) => 1
    case _ => -1
  }
}

/**
 * An intermediate release
 */
case class Milestone(n: Int) extends ScalaBuild {
  def unparse = s"-M${n}"

  def compare(that: ScalaBuild) = that match {
    // compare two milestones based on their milestone numbers
    case Milestone(thatN) => n - thatN
    // a milestone is older than anything other than another milestone
    case _ => -1

  }
}
