package definiti.tests.end2end.controls

import definiti.common.ast.Root
import definiti.common.program.Ko
import definiti.common.tests.{ConfigurationMock, LocationPath}
import definiti.tests.ConfigurationBuilder
import definiti.tests.end2end.EndToEndSpec
import definiti.tests.validation.controls.VerificationReferenceForVerificationTestControl

class VerificationReferenceForVerificationTestControlSpec extends EndToEndSpec {
  import VerificationReferenceForVerificationTestControlSpec._

  "TestsValidation" should "validate a reference to a verification" in {
    val output = processFile("controls.verificationReferenceForVerificationTest.validReference", configuration)
    output shouldBe ok[Root]
  }

  it should "validate a reference to a verification when a package in provided" in {
    val output = processFile("controls.verificationReferenceForVerificationTest.validReferenceInPackage", configuration)
    output shouldBe ok[Root]
  }

  it should "invalidate a reference to a verification when the verification does not exist" in {
    val output = processFile("controls.verificationReferenceForVerificationTest.invalidReference", configuration)
    output should beResult(Ko[Root](
      VerificationReferenceForVerificationTestControl.unknownReference("Unknown", invalidReferenceLocation(4, 1, 29))
    ))
  }
}

object VerificationReferenceForVerificationTestControlSpec {
  val configuration = ConfigurationBuilder().withOnlyControls(VerificationReferenceForVerificationTestControl).build()

  val invalidReferenceLocation = LocationPath.control(VerificationReferenceForVerificationTestControl, "invalidReference")
}