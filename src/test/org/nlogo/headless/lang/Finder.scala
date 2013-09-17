// (C) Uri Wilensky. https://github.com/NetLogo/NetLogo

package org.nlogo.headless
package lang

import java.io.File
import org.nlogo.api.FileIO.file2String
import org.scalatest.{ FunSuite, Tag }
import org.nlogo.api
import org.nlogo.util.SlowTest

/// top level entry points

class TestCommands extends Finder {
  override def files =
    TxtsInDir("test/commands")
}
class TestReporters extends Finder {
  override def files =
    TxtsInDir("test/reporters")
}
class TestModels extends Finder {
  override def files =
    TxtsInDir("models/test")
      .filterNot(_.getName.startsWith("checksums"))
}
class TestExtensions extends Finder {
  override def files = new Iterable[File] {
    override def iterator = {
    def filesInDir(parent: File): Iterable[File] =
      parent.listFiles.flatMap{f =>
        if(f.isDirectory) filesInDir(f)
        else List(f)}
    filesInDir(new File("extensions"))
      .filter(_.getName == "tests.txt")
      .iterator
    }
  }
}

/// common infrastructure

// don't use FixtureSuite here because we may need two fixtures, not just
// one, and FixtureSuite assumes one - ST 8/7/13

trait Finder extends FunSuite with SlowTest {
  def files: Iterable[File]
  // parse tests first, then run them
  for(t <- parseFiles(files))
    test(t.fullName, new Tag(t.suiteName){}, new Tag(t.fullName){}) {
      for (mode <- t.modes)
        if (shouldRun(t, mode))
          withFixture(s"${t.fullName} ($mode)")(
            fixture => runTest(t, mode, fixture))
    }
  def withFixture[T](name: String)(body: AbstractFixture => T): T =
    Fixture.withFixture(name)(body)
  def runTest(t: LanguageTest, mode: TestMode, fixture: AbstractFixture) {
    withFixture(s"${t.fullName} ($mode)") {
      fixture =>
        val nonDecls = t.entries.filterNot(_.isInstanceOf[Declaration])
        val decls =
          t.entries.collect{case d: Declaration => d.source}
            .mkString("\n").trim
        if (nonDecls.forall(!_.isInstanceOf[Open]))
          fixture.open(ModelCreator.Model(
            code = decls,
            dimensions = fixture.defaultDimensions,
            widgets = StandardWidgets))
        else
          assert(t.entries.forall(!_.isInstanceOf[Declaration]))
        nonDecls.foreach{
          case Open(path) =>
            fixture.open(path)
          case command: Command =>
            fixture.runCommand(command, mode)
          case reporter: Reporter =>
            fixture.runReporter(reporter, mode)
          case _ =>
            throw new IllegalStateException
        }
    }
  }
  def parseFiles(files: Iterable[File]): Iterable[LanguageTest] =
    for {
      f <- files if !f.isDirectory
      test <- parseFile(f)
    } yield test

  def parseFile(f: File): List[LanguageTest] = {
    def preprocessStackTraces(s: String) =
      s.replace("\\\n  ", "\\n")
    val suiteName =
      if(f.getName == "tests.txt")
        f.getParentFile.getName
      else
        f.getName.replace(".txt", "")
    Parser.parse(suiteName, preprocessStackTraces(file2String(f.getAbsolutePath)))
  }
  // on the core branch the _3D tests are gone, but extensions tests still have them since we
  // didn't branch the extensions, so we still need to filter those out - ST 1/13/12
  def shouldRun(t: LanguageTest, mode: TestMode) =
    !t.testName.endsWith("_3D") && {
      import api.Version.useGenerator
      if (t.testName.startsWith("Generator"))
        useGenerator
      else if (t.testName.startsWith("NoGenerator"))
        !useGenerator
      else true
    }
  val StandardWidgets = {
    import ModelCreator.{ Plot, Pens, Pen }
    List(
      Plot(name = "plot1", pens = Pens(Pen(name = "pen1"), Pen(name = "pen2"))),
      Plot(name = "plot2", pens = Pens(Pen(name = "pen1"), Pen(name = "pen2"))))
  }
}

case class TxtsInDir(dir: String) extends Iterable[File] {
  override def iterator =
    new File(dir).listFiles
      .filter(_.getName.endsWith(".txt"))
      .filterNot(_.getName.containsSlice("SDM"))
      .filterNot(_.getName.containsSlice("HubNet"))
      .iterator
}