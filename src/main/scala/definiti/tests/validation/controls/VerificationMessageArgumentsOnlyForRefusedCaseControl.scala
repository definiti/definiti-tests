package definiti.tests.validation.controls

import definiti.common.ast.{Library, Location}
import definiti.common.control.{Control, ControlLevel, ControlResult}
import definiti.common.validation.Alert
import definiti.tests.AST
import definiti.tests.AST._

object VerificationMessageArgumentsOnlyForRefusedCaseControl extends Control[TestsContext] {
  override def description: String = "Control if message arguments are given only for 'accept' cases"

  override def defaultLevel: ControlLevel.Value = ControlLevel.error

  override def control(context: AST.TestsContext, library: Library): ControlResult = {
    context.testVerifications.map(controlTestVerification(_, context, library))
  }

  private def controlTestVerification(testVerification: TestVerification, context: TestsContext, library: Library): ControlResult = {
    testVerification.cases.map(controlTestCase)
  }

  private def controlTestCase(testCase: Case): ControlResult = {
    if (testCase.kind == CaseKind.refuse) {
      ControlResult.OK
    } else {
      testCase.subCases.map(controlSubCaseOnAccept)
    }
  }

  private def controlSubCaseOnAccept(subCase: SubCase): ControlResult = {
    if (subCase.messageArguments.isEmpty) {
      ControlResult.OK
    } else {
      unacceptedMessageArguments(subCase.location)
    }
  }

  def unacceptedMessageArguments(location: Location): Alert = {
    alert(s"Message arguments ('as' part) are not accepted for 'accept' cases", location)
  }
}
