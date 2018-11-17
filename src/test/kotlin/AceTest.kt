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

  fun `test sticky results for a query with extra characters`() =
    assertEquals("test test test".lookFor("testz"), setOf(0, 5, 10))

  fun `test a query inside text with some variations`() =
    assertEquals("abcd dabc cdab".lookFor("cd"), setOf(2, 10))

  fun `test a query containing a space character`() =
    assertEquals("abcd dabc cdab".lookFor("cd "), setOf(2))

  fun `test a query containing a { character`() =
    assertEquals("abcd{dabc cdab".lookFor("cd{"), setOf(2))

  fun `test that jumping to first occurrence succeeds`() {
    val results = "<caret>testing 1234".lookFor("1")

    myFixture.performEditorAction(IdeActions.ACTION_EDITOR_ENTER)
    // TODO: Verify that the caret has moved to "testing <caret>1234"
  }

  // Enforces the results are available in less than 100ms
  private fun String.lookFor(query: String) =
    myFixture.run {
      maybeWarmup(this@lookFor, query)
      assert(measureTimeMillis { this@lookFor.justDoQuery(query) } < 100)
      editor.markupModel.allHighlighters.map { it.startOffset }.toSet()
    }

  var warmup = true
  // Should run exactly once to warm up the JVM
  private fun maybeWarmup(text: String, query: String) {
    if (warmup) {
      text.justDoQuery(query)
      myFixture.performEditorAction(IdeActions.ACTION_EDITOR_ESCAPE)
      UIUtil.dispatchAllInvocationEvents()
      warmup = false
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