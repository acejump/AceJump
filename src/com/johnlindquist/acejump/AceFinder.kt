package com.johnlindquist.acejump

import com.intellij.find.FindManager
import com.intellij.find.FindModel
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.impl.DocumentImpl
import com.intellij.openapi.editor.impl.EditorImpl
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.EventDispatcher
import com.maddyhome.idea.vim.helper.EditorHelper
import java.awt.Rectangle
import java.util.ArrayList
import java.util.Comparator
import javax.swing.event.ChangeEvent
import javax.swing.event.ChangeListener

public class AceFinder(val project: Project, val document: DocumentImpl, val editor: EditorImpl, val virtualFile: VirtualFile) {
    class object {
        val ALLOWED_CHARACTERS = "abcdefghijklmnopqrstuvwxyz"
        val END_OF_LINE = "\\n"
        val BEGINNING_OF_LINE = "^.|\\n(?<!.\\n)"
        val CODE_INDENTS = "^\\s*\\S"
        val WHITE_SPACE = "\\s+\\S(?<!^\\s*\\S)"
        val DEFAULT = CODE_INDENTS + "|" + END_OF_LINE
    }

    val eventDispatcher: EventDispatcher<ChangeListener>? = EventDispatcher.create(javaClass<ChangeListener>())


    val findManager = FindManager.getInstance(project)!!
    val findModel: FindModel = createFindModel(findManager)

    public var startResult: Int = 0
    public var endResult: Int = 0
    public var allowedCount: Int = getAllowedCharacters()!!.length()
    public var results: List<Int?>? = null
    public var getEndOffset: Boolean = false
    public var firstChar: String = ""
    public var customOffset: Int = 0
    public var isTargetMode: Boolean = false

    fun createFindModel(findManager: FindManager): FindModel {
        val clone = findManager.getFindInFileModel().clone() as FindModel
        clone.setFindAll(true)
        clone.setFromCursor(true)
        clone.setForward(true)
        clone.setRegularExpressions(false)
        clone.setWholeWordsOnly(false)
        clone.setCaseSensitive(false)
        clone.setSearchHighlighters(true)
        clone.setPreserveCase(false)

        return clone
    }

    fun findText(text: String, isRegEx: Boolean) {
        findModel.setStringToFind(text)
        findModel.setRegularExpressions(isRegEx)

        val application = ApplicationManager.getApplication()
        application?.runReadAction(object:Runnable{
            public override fun run() {
                results = findAllVisible()
            }

        })

        application?.invokeLater(object:Runnable{
            public override fun run() {
                var caretOffset = editor.getCaretModel().getOffset()
                var lineNumber = document.getLineNumber(caretOffset)
                var lineStartOffset = document.getLineStartOffset(lineNumber)
                var lineEndOffset = document.getLineEndOffset(lineNumber)


                results = results!!.sort(object : Comparator<Int?>{
                    public override fun equals(p0: Any?): Boolean {
                        throw UnsupportedOperationException()
                    }
                    public override fun compare(p0: Int?, p1: Int?): Int {
                        var i1: Int = Math.abs(caretOffset - p0!!)
                        var i2: Int = Math.abs(caretOffset - p1!!)
                        var o1OnSameLine: Boolean = p0 >= lineStartOffset && p0 <= lineEndOffset
                        var o2OnSameLine: Boolean = p1 >= lineStartOffset && p1 <= lineEndOffset
                        if (i1 > i2)
                        {
                            if (!o2OnSameLine && o1OnSameLine)
                            {
                                return -1
                            }
                            return 1
                        }
                        else
                            if (i1 == i2)
                            {
                                return 0
                            }
                            else
                            {
                                if (!o1OnSameLine && o2OnSameLine)
                                {
                                    return 1
                                }
                                return -1
                            }
                    }
                })

                startResult = 0;
                endResult = allowedCount;

                eventDispatcher?.getMulticaster()?.stateChanged(ChangeEvent("AceFinder"));
            }

        });
    }
    fun findAllVisible(): List<Int> {
        //System.out.println("----- findAllVisible");
        val visualLineAtTopOfScreen = EditorHelper.getVisualLineAtTopOfScreen(editor)
        val firstLine = EditorHelper.visualLineToLogicalLine(editor, visualLineAtTopOfScreen)
        var offset = EditorHelper.getLineStartOffset(editor, firstLine)

        val height = EditorHelper.getScreenHeight(editor)
        val top = EditorHelper.getVisualLineAtTopOfScreen(editor)

        var lastLine = top + height
        lastLine = EditorHelper.visualLineToLogicalLine(editor, lastLine)

        var endOffset = EditorHelper.normalizeOffset(editor, lastLine, EditorHelper.getLineEndOffset(editor, lastLine, true), true)
        var text = document.getCharsSequence().toString().get(offset, endOffset)!!
        var offsets = ArrayList<Int>()

        var foundOffset = 0
        while (0 < text.length) {
            var result = findManager.findString(text, foundOffset, findModel, virtualFile);
            if (!result.isStringFound()) {
                //System.out.println(findModel.getStringToFind() + ": not found");
                break;
            }
            var resultOffset: Int
            if (getEndOffset) {
                resultOffset = result.getEndOffset() - 1;
            } else {
                resultOffset = result.getStartOffset();
            }
            offsets.add(resultOffset + offset + customOffset);
            foundOffset = result.getEndOffset();
        }

        return offsets;
    }

    public fun expandResults() {
        startResult += allowedCount
        endResult += allowedCount
        checkForReset()
    }

    public fun contractResults() {
        startResult -= allowedCount
        endResult -= allowedCount
        checkForReset()
    }

    fun checkForReset() {
        if(startResult < 0) startResult = 0
        if(endResult < allowedCount) endResult = allowedCount
    }

    private fun checkFolded(offset: Int): Int {
        val foldingModelImpl = editor.getFoldingModel()

        var offsetResult:Int = offset;
        for(foldRegion in foldingModelImpl.fetchCollapsedAt(offset)?.iterator()){
            offsetResult = foldRegion.getEndOffset() + 1
        }

        return offsetResult
    }

    public fun addResultsReadyListener(changeListener: ChangeListener) {
        eventDispatcher?.addListener(changeListener)
    }

    public fun getAllowedCharacters(): CharSequence? {
        return ALLOWED_CHARACTERS
    }

}