package com.twitter.server.tests

import com.twitter.inject.Test
import com.twitter.server.internal.FinagleBuildRevision
import org.apache.commons.lang.RandomStringUtils

class FinagleBuildRevisionTest extends Test {

  val revision = "e748e2fe89da63923e525b71f82f398b18c9a60c"

  test("parse revision") {
    FinagleBuildRevision.convertBuildRevision(revision) should be(993360281225L)
  }

  test("fail revision parsing") {
    FinagleBuildRevision.convertBuildRevision(RandomStringUtils.randomAlphanumeric(20)) should be(-1L)
  }

}
