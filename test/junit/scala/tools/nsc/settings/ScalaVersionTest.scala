package scala.tools.nsc
package settings

import org.junit.Assert._
import org.junit.Test
import scala.tools.testkit.AssertUtil.assertThrows

class ScalaVersionTest {
  // scala/bug#8711
  @Test def versionUnparse(): Unit = {
    val v = "2.11.3"

    assertEquals(v, ScalaVersion(v).unparse)
    assertEquals("2.11.3-RC4", ScalaVersion("2.11.3-rc4").unparse)
  }

  // scala/bug#9167
  @Test def `version parses with rigor`(): Unit = {
    import settings.{ SpecificScalaVersion => V }

    // no-brainers
    assertEquals(V(2,11,7,Final), ScalaVersion("2.11.7"))
    assertEquals(V(2,11,7,Final), ScalaVersion("2.11.7-FINAL"))
    assertEquals(V(2,11,7,Milestone(3)), ScalaVersion("2.11.7-M3"))
    assertEquals(V(2,11,7,RC(3)), ScalaVersion("2.11.7-RC3"))
    assertEquals(V(2,11,7,Development("devbuild")), ScalaVersion("2.11.7-devbuild"))

    // partial-brainers
    assertEquals(V(2,11,7,Milestone(3)), ScalaVersion("2.11.7-m3"))
    assertEquals(V(2,11,7,RC(3)), ScalaVersion("2.11.7-rc3"))
    assertEquals(V(2,11,7,Development("maybegood")), ScalaVersion("2.11.7-maybegood"))
    assertEquals(V(2,11,7,Development("RCCola")), ScalaVersion("2.11.7-RCCola"))
    assertEquals(V(2,11,7,Development("RC1.5")), ScalaVersion("2.11.7-RC1.5"))
    assertEquals(V(2,11,0,Development("Z")), ScalaVersion("2.11-Z"))
    assertEquals(V(2,11,7,Development("0.5")), ScalaVersion("2.11.7-0.5"))
    assertEquals(V(2,11,7,Development("devbuild\nt9167")), ScalaVersion("2.11.7-devbuild\nt9167"))
    assertEquals(V(2,11,7,Development("final")), ScalaVersion("2.11.7-final"))

    // oh really
    assertEquals(NoScalaVersion, ScalaVersion("none"))
    assertSame(NoScalaVersion, ScalaVersion("none"))
    assertEquals(Scala3Cross, ScalaVersion("3-cross"))
    assertSame(Scala3Cross, ScalaVersion("3-cross"))
    assertEquals(AnyScalaVersion, ScalaVersion("any"))
    assertSame(AnyScalaVersion, ScalaVersion("any"))

    assertThrows[NumberFormatException] { ScalaVersion("2.11.7.2") }
    assertThrows[NumberFormatException] { ScalaVersion("2.11.7.beta") }
    assertThrows[NumberFormatException] { ScalaVersion("2.x.7") }
    assertThrows[NumberFormatException] { ScalaVersion("2.-11.7") }
    assertThrows[NumberFormatException] { ScalaVersion("2. ") }
    assertThrows[NumberFormatException] { ScalaVersion("2.1 .7") }
    assertThrows[NumberFormatException] { ScalaVersion("2.") }
    assertThrows[NumberFormatException] { ScalaVersion("2..") }
    assertThrows[NumberFormatException] { ScalaVersion("2...") }
    assertThrows[NumberFormatException] { ScalaVersion("2-") }
    //assertThrows[NumberFormatException] { ScalaVersion("2-.") } // scalacheck territory
    assertEquals(V(2,0,0,Development(".")), ScalaVersion("2-."))  // build labels are arbitrary text
    assertEquals(V(2,11,0,Development("ok")), ScalaVersion("2.11-ok"))  // build labels are arbitrary text
    assertThrows[NumberFormatException] { ScalaVersion("2.11.7-") }
    assertThrows[NumberFormatException] { ScalaVersion("any.7") }

    assertThrows[NumberFormatException] ( ScalaVersion("2.11-"), _ ==
      "Bad version (2.11-) not major[.minor[.revision]][-suffix]" )

  }

  // scala/bug#9377
  @Test def `missing version is as good as none`(): Unit = {
    assertEquals(NoScalaVersion, ScalaVersion(""))
  }

  @Test def `missing ops`(): Unit = {
    assertEquals(ScalaVersion("2.13"), ScalaVersion("2.12") max ScalaVersion("2.13"))
    assertEquals(ScalaVersion("2.12"), ScalaVersion("2.12") min ScalaVersion("2.13"))
    assertEquals(ScalaVersion("2.13"), ScalaVersion("2.13") max ScalaVersion("2.12"))
    assertEquals(ScalaVersion("2.12"), ScalaVersion("2.13") min ScalaVersion("2.12"))
    assertEquals(ScalaVersion("2.13"), ScalaVersion("2.13") max ScalaVersion("2.13"))
    assertEquals(ScalaVersion("2.13"), ScalaVersion("2.13") min ScalaVersion("2.13"))
  }
}
