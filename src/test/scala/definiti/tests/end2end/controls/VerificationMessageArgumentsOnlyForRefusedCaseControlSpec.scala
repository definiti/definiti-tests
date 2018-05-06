package definiti.tests.end2end.controls

import definiti.core.Ko
import definiti.core.ast.Root
import definiti.tests.ConfigurationMock
import definiti.tests.end2end.EndToEndSpec
import definiti.tests.validation.controls.VerificationMessageArgumentsOnlyForRefusedCaseControl

class VerificationMessageArgumentsOnlyForRefusedCaseControlSpec extends EndToEndSpec {
  import VerificationMessageArgumentsOnlyForRefusedCaseControlSpec._

  "TestsValidation" should "validate a 'refuse' sub case with no message argument" in {
    val output = processFile("controls.verificationMessageArgumentsOnlyForRefusedCase.noMessageArgumentsOnRefuse", configuration)
    output shouldBe ok[Root]
  }

  it should "validate a 'accept' sub case with no message argument" in {
    val output = processFile("controls.verificationMessageArgumentsOnlyForRefusedCase.noMessageArgumentsOnAccept", configuration)
    output shouldBe ok[Root]
  }

  it should "validate a 'refuse' sub case with message argument" in {
    val output = processFile("controls.verificationMessageArgumentsOnlyForRefusedCase.messageArgumentsOnRefuse", configuration)
    output shouldBe ok[Root]
  }

  it should "invalidate a 'accept' sub case with message argument" in {
    val output = processFile("controls.verificationMessageArgumentsOnlyForRefusedCase.messageArgumentsOnAccept", configuration)
    output should beResult(Ko[Root](
      VerificationMessageArgumentsOnlyForRefusedCaseControl.unacceptedMessageArguments(messageArgumentsOnAcceptLocation(16, 12, 42))
    ))
  }
}

object VerificationMessageArgumentsOnlyForRefusedCaseControlSpec {
  import EndToEndSpec._

  val configuration = ConfigurationMock()

  val messageArgumentsOnAcceptLocation = LocationPath.control(VerificationMessageArgumentsOnlyForRefusedCaseControl.name, "messageArgumentsOnAccept")
}