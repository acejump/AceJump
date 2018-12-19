import com.intellij.openapi.actionSystem.IdeActions
import com.intellij.openapi.fileTypes.PlainTextFileType
import com.intellij.testFramework.fixtures.LightCodeInsightFixtureTestCase
import com.intellij.util.ui.UIUtil
import org.acejump.control.AceAction
import org.acejump.view.Canvas
import kotlin.system.measureTimeMillis

/**
 * Functional test cases and end-to-end performance tests.
 *
 * TODO: Add more structure to test cases, use test resources to define files.
 */

class AceTest : LightCodeInsightFixtureTestCase() {
  fun `test that scanner finds all occurrences of single character`() =
    assertEquals("test test test".lookFor("t"), setOf(0, 3, 5, 8, 10, 13))

  fun `test empty results for an absent query`() =
    assertEmpty("test test test".lookFor("best"))

  fun `test sticky results on a query with extra characters`() =
    assertEquals("test test test".lookFor("testz"), setOf(0, 5, 10))

  fun `test a query inside text with some variations`() =
    assertEquals("abcd dabc cdab".lookFor("cd"), setOf(2, 10))

  fun `test a query containing a space character`() =
    assertEquals("abcd dabc cdab".lookFor("cd "), setOf(2))

  fun `test a query containing a { character`() =
    assertEquals("abcd{dabc cdab".lookFor("cd{"), setOf(2))

  fun `test that jumping to first occurrence succeeds`() {
    "<caret>testing 1234".lookFor("1")

    myFixture.performEditorAction(IdeActions.ACTION_EDITOR_ENTER)

    myFixture.checkResult("testing <caret>1234")
  }

  fun `test that jumping to second occurrence succeeds`() {
    "<caret>testing 1234".lookFor("ti")

    myFixture.performEditorAction(IdeActions.ACTION_EDITOR_ENTER)

    myFixture.checkResult("tes<caret>ting 1234")
  }

  fun `test shift selection`() {
    "<caret>testing 1234".lookFor("4")
    val firstTag = Canvas.jumpLocations.first().tag!!
    myFixture.type(firstTag)
    assertEquals("testing 123", myFixture.editor.selectionModel.selectedText)
  }

  // Enforces the results are available in less than 100ms
  private fun String.lookFor(query: String) =
    myFixture.run {
      maybeWarmUp(this@lookFor, query)
      val queryTime = measureTimeMillis { this@lookFor.justDoQuery(query) }
      assert(queryTime < 100) { "Query exceeded time limit! ($queryTime ms)" }
      editor.markupModel.allHighlighters.map { it.startOffset }.toSet()
    }

  private var shouldWarmup = true
  // Should be run exactly once to warm up the JVM
  private fun maybeWarmUp(text: String, query: String) {
    if (shouldWarmup) {
      text.justDoQuery(query)
      myFixture.performEditorAction(IdeActions.ACTION_EDITOR_ESCAPE)
      UIUtil.dispatchAllInvocationEvents()
      // Now the JVM is warm, never run this method again
      shouldWarmup = false
    }
  }

  // Just does a query without enforcing any time limit
  private fun String.justDoQuery(query: String) {
    myFixture.run {
      configureByText(PlainTextFileType.INSTANCE, this@justDoQuery)
      testAction(AceAction())
      type(query)
      UIUtil.dispatchAllInvocationEvents()
    }
  }

  override fun tearDown() {
    myFixture.performEditorAction(IdeActions.ACTION_EDITOR_ESCAPE)
    UIUtil.dispatchAllInvocationEvents()
    assertEmpty(editor.markupModel.allHighlighters)
    super.tearDown()
  }
}