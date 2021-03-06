package definiti.tests

import definiti.common.ast.{Library, Location}
import definiti.common.plugin.ContextPlugin
import definiti.common.program.ProgramResult
import definiti.common.validation.{Invalid, SimpleError, Valid, Validated}
import definiti.tests.ast.TestsContext
import definiti.tests.json.JsonAST
import definiti.tests.parser.TestsContextParser
import definiti.tests.validation.TestsValidation
import spray.json._

class TestsPlugin extends ContextPlugin[TestsContext] {
  val configuration: Configuration = new FileConfiguration().load()

  override def name: String = "tests"

  override def contextName: String = "tests"

  override def parse(content: String, packageName: String, imports: Map[String, String], location: Location): TestsContext = {
    new TestsContextParser(packageName, imports, location).parse(content) match {
      case Valid(value) => value
      case Invalid(errors) => throw new RuntimeException(s"errors during parsing of context tests: ${errors.map(_.prettyPrint).mkString("\n")}")
    }
  }

  override def validate(context: TestsContext, library: Library): Validated[ProgramResult.NoResult] = {
    CoreGenerators.coreGenerators
      .flatMap { generators =>
        val result = new TestsValidation(library, configuration, generators).validate(context)
        if (result.alerts.nonEmpty) {
          Invalid(result.alerts.map(alert => SimpleError(alert.prettyPrint)))
        } else {
          Valid(ProgramResult.NoResult)
        }
      }
  }

  override def contextToJson(context: TestsContext): String = JsonAST.format.write(context).compactPrint

  override def contextFromJson(json: String): TestsContext = JsonAST.format.read(json.parseJson)
}
