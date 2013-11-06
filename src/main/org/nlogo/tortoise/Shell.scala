// (C) Uri Wilensky. https://github.com/NetLogo/NetLogo

package org.nlogo.tortoise

import org.nlogo.{ api, workspace }

object Shell extends workspace.Shell {

  val src = ""
  val dim = api.WorldDimensions.square(16)

  val rhino = new Rhino

  def main(argv: Array[String]) {
    workspace.AbstractWorkspace.setHeadlessProperty()
    val (js, program, procedures) =
      Compiler.compileProcedures(src, dimensions = dim)
    rhino.eval(js)
    System.err.println("Tortoise Shell 1.0")
    for(line <- input.takeWhile(!isQuit(_)))
      try run(Compiler.compileCommands(line, procedures, program))
      catch { case e: Exception => println(e) }
  }

  def run(js: String) {
    try {
      val (output, json) = rhino.run(js)
      Seq(output) // , json)
        .filter(_.nonEmpty)
        .foreach(x => println(x.trim))
    }
    catch { case e: Exception => println(e) }
  }

}