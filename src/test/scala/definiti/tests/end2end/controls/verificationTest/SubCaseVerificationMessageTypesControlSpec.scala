package definiti.tests.end2end.controls.verificationTest

import definiti.common.ast.{Root, TypeReference}
import definiti.common.program.Ko
import definiti.common.tests.LocationPath
import definiti.tests.ConfigurationBuilder
import definiti.tests.end2end.EndToEndSpec
import definiti.tests.validation.controls.verificationTest.SubCaseVerificationMessageTypesControl


class SubCaseVerificationMessageTypesControlSpec extends EndToEndSpec {
  import SubCaseVerificationMessageTypesControlSpec._

  "TestsValidation" should "validate any sub case with no message argument" in {
    val output = processFile("controls.verificationTest.subCaseVerificationMessageTypes.noMessageArguments", configuration)
    output shouldBe ok[Root]
  }

  it should "invalidate a sub case having message arguments for literal messages" in {
    val output = processFile("controls.verificationTest.subCaseVerificationMessageTypes.invalidMessageArgumentsForLiteral", configuration)
    output should beResult(Ko[Root](
      SubCaseVerificationMessageTypesControl.invalidNumberOfParameters(0, 1, invalidMessageArgumentsForLiteralLocation(19, 12, 40)),
      SubCaseVerificationMessageTypesControl.invalidNumberOfParameters(0, 1, invalidMessageArgumentsForLiteralLocation(23, 12, 30))
    ))
  }

  it should "invalidate a sub case having an invalid number of message arguments for typed messages" in {
    val output = processFile("controls.verificationTest.subCaseVerificationMessageTypes.invalidNumberOfMessageArgumentsForTyped", configuration)
    output should beResult(Ko[Root](
      SubCaseVerificationMessageTypesControl.invalidNumberOfParameters(1, 2, invalidNumberOfMessageArgumentsForTypedLocation(27, 12, 43)),
      SubCaseVerificationMessageTypesControl.invalidNumberOfParameters(1, 2, invalidNumberOfMessageArgumentsForTypedLocation(31, 12, 33))
    ))
  }

  it should "invalidate a sub case having an invalid argument type for typed messages" in {
    val output = processFile("controls.verificationTest.subCaseVerificationMessageTypes.invalidTypeOfMessageArgumentsForTyped", configuration)
    output should beResult(Ko[Root](
      SubCaseVerificationMessageTypesControl.invalidType(TypeReference("Number"), invalidTypeOfMessageArgumentsForTypedLocation(27, 38, 41)),
      SubCaseVerificationMessageTypesControl.invalidType(TypeReference("Number"), invalidTypeOfMessageArgumentsForTypedLocation(31, 28, 32))
    ))
  }
}

object SubCaseVerificationMessageTypesControlSpec {
  val configuration = ConfigurationBuilder().withOnlyControls(SubCaseVerificationMessageTypesControl).build()

  val controlDirectory = s"verificationTest/${SubCaseVerificationMessageTypesControl.name}"
  val invalidMessageArgumentsForLiteralLocation = LocationPath.control(controlDirectory, "invalidMessageArgumentsForLiteral")
  val invalidNumberOfMessageArgumentsForTypedLocation = LocationPath.control(controlDirectory, "invalidNumberOfMessageArgumentsForTyped")
  val invalidTypeOfMessageArgumentsForTypedLocation = LocationPath.control(controlDirectory, "invalidTypeOfMessageArgumentsForTyped")
}