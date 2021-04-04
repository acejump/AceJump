import junit.framework.TestCase
import org.acejump.boundaries.StandardBoundaries.*
import org.acejump.search.Pattern.*
import org.acejump.session.*
import org.acejump.test.util.BaseTest

/**
 * Test [org.acejump.ExternalUsage] endpoints.
 */

class ExternalUsageTest: BaseTest() {
  fun `test externally tagged results and listener notification`() {
    makeEditor("test externally tagged results")

    SessionManager.start(myFixture.editor)
      .markResults(sortedSetOf(4, 10, 15))

    TestCase.assertEquals(3, session.tags.size)

    var shouldBeTrueAfterFinished = false
    session.addAceJumpListener(object: AceJumpListener {
      override fun finished() {
        shouldBeTrueAfterFinished = true
      }
    })

    typeAndWaitForResults(session.tags[0].key)

    TestCase.assertTrue(shouldBeTrueAfterFinished)
  }

  fun `test external pattern usage`() {
    makeEditor("test external pattern usage")

    SessionManager.start(myFixture.editor)
      .startRegexSearch(ALL_WORDS, WHOLE_FILE)

    TestCase.assertEquals(4, session.tags.size)
  }

  fun `test external regex usage`() {
    makeEditor("test external regex usage")

    SessionManager.start(myFixture.editor)
      .startRegexSearch("[aeiou]+", WHOLE_FILE)

    TestCase.assertEquals(8, session.tags.size)
  }
}
