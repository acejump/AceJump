import com.intellij.openapi.actionSystem.IdeActions.ACTION_EDITOR_ENTER
import com.intellij.openapi.actionSystem.IdeActions.ACTION_EDITOR_START_NEW_LINE
import com.intellij.openapi.editor.actions.EnterAction
import org.acejump.action.AceAction
import org.acejump.config.AceConfig
import org.acejump.test.util.BaseTest

/**
 * Functional test cases and end-to-end performance tests.
 *
 * TODO: Add more structure to test cases, use test resources to define files.
 */

class AceTest : BaseTest() {
  fun `test that scanner finds all occurrences of single character`() =
    assertEquals("test test test".search("t"), setOf(0, 3, 5, 8, 10, 13))

  fun `test empty results for an absent query`() =
    assertEmpty("test test test".search("best"))

  fun `test sticky results on a query with extra characters`() =
    assertEquals("test test test".search("testz"), setOf(0, 5, 10))

  fun `test a query inside text with some variations`() =
    assertEquals("abcd dabc cdab".search("cd"), setOf(2, 10))

  fun `test a query containing a space character`() =
    assertEquals("abcd dabc cdab".search("cd "), setOf(2))

  fun `test a query containing a { character`() =
    assertEquals("abcd{dabc cdab".search("cd{"), setOf(2))

  fun `test that jumping to first occurrence succeeds`() {
    "<caret>testing 1234".search("1")

    takeAction(ACTION_EDITOR_ENTER)

    myFixture.checkResult("testing <caret>1234")
  }

  fun `test that jumping to second occurrence succeeds`() {
    "<caret>testing 1234".search("ti")

    takeAction(ACTION_EDITOR_ENTER)

    myFixture.checkResult("tes<caret>ting 1234")
  }

  fun `test that jumping to previous occurrence succeeds`() {
    "te<caret>sting 1234".search("t")

    takeAction(ACTION_EDITOR_START_NEW_LINE)

    myFixture.checkResult("<caret>testing 1234")
  }

  fun `test tag selection`() {
    "<caret>testing 1234".search("g")

    typeAndWaitForResults(session.tags[0].key)

    myFixture.checkResult("testin<caret>g 1234")
  }

  fun `test shift selection`() {
    "<caret>testing 1234".search("4")

    typeAndWaitForResults(session.tags[0].key.uppercase())

    myFixture.checkResult("<selection>testing 123<caret></selection>4")
  }

  fun `test words before caret action`() {
    makeEditor("test words <caret> before caret is two")

    takeAction(AceAction.StartAllWordsBackwardsMode())

    assertEquals(2, session.tags.size)
  }

  fun `test words after caret action`() {
    makeEditor("test words <caret> after caret is four")

    takeAction(AceAction.StartAllWordsForwardMode())

    assertEquals(4, session.tags.size)
  }

  fun `test word mode`() {
    makeEditor("test word action")

    takeAction(AceAction.StartAllWordsMode())

    assertEquals(3, session.tags.size)

    typeAndWaitForResults(session.tags[1].key)

    myFixture.checkResult("test <caret>word action")
  }

  fun `test target mode`() {
    "<caret>test target action".search("target")

    takeAction(AceAction.ToggleTargetMode())
    typeAndWaitForResults(session.tags[0].key)

    myFixture.checkResult("test <selection>target<caret></selection> action")
  }

  fun `test cache invalidation`() {
    "first line".search("first")
    typeAndWaitForResults(session.tags[0].key)

    repeat(3) { takeAction(EnterAction()) }

    takeAction(AceAction.ToggleTargetMode())
    typeAndWaitForResults("first")
    typeAndWaitForResults(session.tags[0].key)

    myFixture.checkResult("\n\n\n<selection>first<caret></selection> line")
  }

  fun `test line mode`() {
    makeEditor("    test\n    three\n    lines", visibleLines = 3)

    takeAction(AceAction.StartAllLineMarksMode())

    assertEquals(9, session.tags.size)
  }

  fun `test chinese selection`() {
    AceConfig.settings.mapToASCII = true

    "test 拼音 selection".search("py")

    takeAction(AceAction.ToggleTargetMode())

    typeAndWaitForResults(session.tags[0].key)

    myFixture.checkResult("test <selection>拼音<caret></selection> selection")
  }

  fun `test japanese selection`() {
    AceConfig.settings.mapToASCII = true

    "あみだにょらい".search("am")

    takeAction(AceAction.ToggleTargetMode())

    typeAndWaitForResults(session.tags[0].key)

    myFixture.checkResult("<selection>あみだにょらい<caret></selection>")
  }

  fun `test query repitition`() {
    "test test test".search("t")

    typeAndWaitForResults("t")

    typeAndWaitForResults(session.tags[1].key)

    myFixture.checkResult("tes<caret>t test test")
  }

  // https://github.com/acejump/AceJump/issues/355
  fun `ignore test a word that is difficult to tag`() {
    makeEditor("aaCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789")

    takeAction(AceAction.ActivateOrCycleMode())

    typeAndWaitForResults("c")

    assertEquals(2, session.tags.size)
  }
}
