import com.intellij.openapi.actionSystem.IdeActions.ACTION_EDITOR_ENTER
import com.intellij.openapi.actionSystem.IdeActions.ACTION_EDITOR_START_NEW_LINE
import org.acejump.action.AceAction
import org.acejump.config.*
import org.acejump.config.AceConfig.Companion.enablePinyin
import org.acejump.test.util.BaseTest

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.IdeActions.*
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.fileTypes.PlainTextFileType
import com.intellij.psi.PsiFile
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.util.ui.UIUtil
import junit.framework.TestCase
import org.acejump.config.AceConfig
import org.acejump.session.*
import kotlin.random.Random
import kotlin.system.measureTimeMillis
import java.io.File

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

    typeAndWaitForResults(session.tags[0].key.toUpperCase())

    myFixture.checkResult("<selection>testing 123<caret></selection>4")
  }

  fun `test words before caret action`() {
    makeEditor("test words <caret> before caret is two")

    takeAction(AceAction.StartAllWordsBackwardsMode)

    assertEquals(2, session.tags.size)
  }

  fun `test words after caret action`() {
    makeEditor("test words <caret> after caret is four")

    takeAction(AceAction.StartAllWordsForwardMode)

    assertEquals(4, session.tags.size)
  }

  fun `test word mode`() {
    makeEditor("test word action")

    takeAction(AceAction.StartAllWordsMode)

    assertEquals(3, session.tags.size)

    typeAndWaitForResults(session.tags[1].key)

    myFixture.checkResult("test <caret>word action")
  }

  fun `test target mode`() {
    "<caret>test target action".search("target")

    takeAction(AceAction.ToggleTargetMode)
    typeAndWaitForResults(session.tags[0].key)

    myFixture.checkResult("test <selection>target<caret></selection> action")
  }

  fun `test line mode`() {
    makeEditor("    test\n    three\n    lines\n")

    takeAction(AceAction.StartAllLineMarksMode)

    assertEquals(8, session.tags.size) // last empty line does not count
  }

  fun `test pinyin selection`() {
    AceConfig.settings.enablePinyin = true

    "test 拼音 selection".search("py")

    takeAction(AceAction.ToggleTargetMode)

    typeAndWaitForResults(session.tags[0].key)

    myFixture.checkResult("test <selection>拼音<caret></selection> selection")
  }

  fun `test external usage`() {
    makeEditor("test externally annotated fragment")

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
}
