import com.intellij.mock.MockVirtualFile
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.markup.HighlighterLayer
import com.intellij.openapi.editor.markup.HighlighterTargetArea
import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.util.ui.UIUtil
import it.unimi.dsi.fastutil.ints.IntArrayList
import junit.framework.TestCase
import org.acejump.action.AceAction
import org.acejump.boundaries.Boundaries
import org.acejump.boundaries.EditorOffsetCache
import org.acejump.boundaries.StandardBoundaries.WHOLE_FILE
import org.acejump.input.JumpMode
import org.acejump.search.Pattern.ALL_WORDS
import org.acejump.session.AceJumpListener
import org.acejump.session.SessionManager
import org.acejump.test.util.BaseTest

/**
 * Test [org.acejump.ExternalUsage] endpoints.
 */

class ExternalUsageTest: BaseTest() {
  fun `test externally tagged results and listener notification`() {
    makeEditor("test externally tagged results")

    SessionManager.start(myFixture.editor).markResults(sortedSetOf(4, 10, 15))

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
  
  fun `test externally tagged results with multiple editors`() {
    val fileA = MockVirtualFile("a.txt", "first file")
    val fileB = MockVirtualFile("b.txt", "second file with more markers")
    manager?.openFile(fileA, true)
    manager?.openFile(fileB, false)
    
    val mainEditor = (manager?.selectedEditor as TextEditor).editor
    val editorA = (manager?.getEditors(fileA)?.single() as TextEditor).editor
    val editorB = (manager?.getEditors(fileB)?.single() as TextEditor).editor
    
    val session = SessionManager.start(mainEditor, listOf(editorA, editorB))
    
    session.markResults(mapOf(
      editorA to IntArrayList(intArrayOf(0, 6)),
      editorB to IntArrayList(intArrayOf(0, 7, 22))
    ))
  
    TestCase.assertEquals(5, session.tags.size)
    TestCase.assertEquals(2, session.tags.count { it.value.editor === editorA })
    TestCase.assertEquals(3, session.tags.count { it.value.editor === editorB })
    
    TestCase.assertEquals(listOf(0, 6), session.tags
      .filter { it.value.editor === editorA }
      .map { it.value.offset }
      .sorted())
    
    TestCase.assertEquals(listOf(0, 7, 22), session.tags
      .filter { it.value.editor === editorB }
      .map { it.value.offset }
      .sorted())
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

    TestCase.assertEquals(9, session.tags.size)
  }

  fun `test external jump with bounds`() {
    makeEditor("test word and word usage")

    SessionManager.start(myFixture.editor)
      .toggleJumpMode(JumpMode.JUMP, object : Boundaries {
        override fun getOffsetRange(editor: Editor, cache: EditorOffsetCache): IntRange {
          return 14..18
        }

        override fun isOffsetInside(editor: Editor, offset: Int, cache: EditorOffsetCache): Boolean {
          return offset in 14..18
        }
      })

    typeAndWaitForResults("word")

    TestCase.assertEquals(1, session.tags.size)
    TestCase.assertEquals(14, session.tags.single().value.offset)
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

    takeAction(AceAction.StartAllWordsMode())

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

  fun `test do not remove other highlights when the session ends`() {
    makeEditor("test do not remove other highlights when the session ends")

    val markupModel = myFixture.editor.markupModel
    val layer = HighlighterLayer.SELECTION - 1
    val existedHighlighter = markupModel.addRangeHighlighter(0, 1, layer, null, HighlighterTargetArea.EXACT_RANGE)

    takeAction(AceAction.StartAllWordsMode())
    val mark = session.tags[0].key
    typeAndWaitForResults(mark)

    TestCase.assertEquals("last session should be disposed", null, SessionManager[myFixture.editor])
    TestCase.assertTrue("existed highlighter should not be removed", existedHighlighter.isValid)

    existedHighlighter.dispose()
  }
}
