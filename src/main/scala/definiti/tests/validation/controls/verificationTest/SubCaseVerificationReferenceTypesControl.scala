package definiti.tests.validation.controls.verificationTest

import definiti.common.ast._
import definiti.common.control.{Control, ControlLevel, ControlResult}
import definiti.common.validation.Alert
import definiti.tests.ast.{Case, Expression, SubCase, TestVerification}
import definiti.tests.validation.ValidationContext
import definiti.tests.validation.helpers.{ScopedExpression, ScopedType}

object SubCaseVerificationReferenceTypesControl extends Control[ValidationContext] {
  override def description: String = "Control that sub case to a verification have the right input types"

  override def defaultLevel: ControlLevel.Value = ControlLevel.error

  override def control(context: ValidationContext, library: Library): ControlResult = {
    context.testVerifications.map(controlTestVerification(_, context))
  }

  private def controlTestVerification(testVerification: TestVerification, context: ValidationContext): ControlResult = {
    context.getVerification(testVerification.verification)
      .map { verification =>
        ControlResult.squash {
          testVerification.cases.map(controlTestCase(_, verification, context))
        }
      }
      .getOrElse(ignored)
  }

  private def controlTestCase(testCase: Case, verification: Verification, context: ValidationContext): ControlResult = {
    testCase.subCases.map(controlTestSubCase(_, verification, context))
  }

  private def controlTestSubCase(subCase: SubCase, verification: Verification, context: ValidationContext): ControlResult = {
    if (verification.parameters.length == subCase.arguments.length) {
      verification.parameters.zip(subCase.arguments)
        .map { case (verificationParameter, caseArgument) =>
          controlExpression(ScopedExpression(caseArgument, context), ScopedType(verificationParameter.typeReference, verification, context))
        }
    } else {
      invalidNumberOfParameters(verification.parameters.length, subCase.arguments.length, subCase.location)
    }
  }

  private def controlExpression(expression: ScopedExpression[Expression], scopedType: ScopedType): ControlResult = {
    if (scopedType.isSameAs(expression.typeOfExpression)) {
      ControlResult.OK
    } else {
      invalidType(scopedType.typeReference, expression.location)
    }
  }

  def invalidNumberOfParameters(expectedNumber: Int, gotNumber: Int, location: Location): Alert = {
    alert(s"The number of parameters for verification does not match (expected: ${expectedNumber}, got: ${gotNumber})", location)
  }

  def invalidType(expectedType: AbstractTypeReference, location: Location): Alert = {
    alert(s"The expression does not match the expected type ${expectedType.readableString}", location)
  }
}
