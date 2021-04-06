import com.intellij.util.ui.UIUtil
import junit.framework.TestCase
import org.acejump.action.AceAction
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
      override fun finished(mark: String?, query: String?) {
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

  fun `test listener query and mark`() {
    "<caret>testing 1234".search("g")

    var detectedMark: String? = null
    var detectedQuery: String? = null
    session.addAceJumpListener(object: AceJumpListener {
      override fun finished(mark: String?, query: String?) {
        detectedMark = mark
        detectedQuery = query
      }
    })

    val mark = session.tags[0].key
    typeAndWaitForResults(mark)

    TestCase.assertEquals(mark, detectedMark)
    TestCase.assertEquals("g", detectedQuery)
  }

  fun `test listener after escape`() {
    "<caret>testing 1234".search("g")

    var detectedMark: String? = null
    var detectedQuery: String? = null
    session.addAceJumpListener(object: AceJumpListener {
      override fun finished(mark: String?, query: String?) {
        detectedMark = mark
        detectedQuery = query
      }
    })

    myFixture.performEditorAction("EditorEscape")
    UIUtil.dispatchAllInvocationEvents()

    TestCase.assertEquals(null, detectedMark)
    TestCase.assertEquals(null, detectedQuery)
  }

  fun `test listener for word motion`() {
    makeEditor("test word action")

    takeAction(AceAction.StartAllWordsMode)

    var detectedMark: String? = null
    var detectedQuery: String? = null
    session.addAceJumpListener(object: AceJumpListener {
      override fun finished(mark: String?, query: String?) {
        detectedMark = mark
        detectedQuery = query
      }
    })

    val mark = session.tags[1].key
    typeAndWaitForResults(mark)

    TestCase.assertEquals(mark, detectedMark)
    TestCase.assertEquals("", detectedQuery)
  }
}
