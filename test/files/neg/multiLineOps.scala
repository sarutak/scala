//> using options -Werror -Xlint -Xsource:3-cross

class Test {
  val x = 1
    + 2
    +3 // warning: a pure expression does nothing in statement position
}
