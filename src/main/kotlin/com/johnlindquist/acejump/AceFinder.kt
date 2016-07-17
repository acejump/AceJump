package com.johnlindquist.acejump

import com.intellij.find.FindManager
import com.intellij.find.FindModel
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.impl.DocumentImpl
import com.intellij.openapi.editor.impl.EditorImpl
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.EventDispatcher
import java.util.*
import javax.swing.event.ChangeEvent
import javax.swing.event.ChangeListener

class AceFinder(val project: Project, val document: DocumentImpl, val editor: EditorImpl, val virtualFile: VirtualFile) {
    companion object {
        val ALLOWED_CHARACTERS = "abcdefghijklmnopqrstuvwxyz"
        val END_OF_LINE = "\\n"
        val BEGINNING_OF_LINE = "^.|\\n(?<!.\\n)"
        val CODE_INDENTS = "^\\s*\\S"
        val WHITE_SPACE = "\\s+\\S(?<!^\\s*\\S)"
    }

    val eventDispatcher: EventDispatcher<ChangeListener>? = EventDispatcher.create(ChangeListener::class.java)


    val findManager = FindManager.getInstance(project)!!
    val findModel: FindModel = createFindModel(findManager)

    var startResult: Int = 0
    var endResult: Int = 0
    var allowedCount: Int = getAllowedCharacters().length
    var results: List<Int>? = null
    var getEndOffset: Boolean = false
    var firstChar: String = ""
    var customOffset: Int = 0
    var isTargetMode: Boolean = false

    fun createFindModel(findManager: FindManager): FindModel {
        val clone = findManager.findInFileModel.clone()
        clone.isFindAll = true
        clone.isFromCursor = true
        clone.isForward = true
        clone.isRegularExpressions = false
        clone.isWholeWordsOnly = false
        clone.isCaseSensitive = false
        clone.setSearchHighlighters(true)
        clone.isPreserveCase = false

        return clone
    }

    fun findText(text: String, isRegEx: Boolean) {
        findModel.stringToFind = text
        findModel.isRegularExpressions = isRegEx

        val application = ApplicationManager.getApplication()
        application?.runReadAction({ results = findAllVisible() })

        application?.invokeLater({
            val caretOffset = editor.caretModel.offset
            val lineNumber = document.getLineNumber(caretOffset)
            val lineStartOffset = document.getLineStartOffset(lineNumber)
            val lineEndOffset = document.getLineEndOffset(lineNumber)


            results = results?.sortedWith(object : Comparator<Int?> {
                override fun equals(other: Any?): Boolean {
                    throw UnsupportedOperationException()
                }

                override fun compare(p0: Int?, p1: Int?): Int {
                    val i1: Int = Math.abs(caretOffset - p0!!)
                    val i2: Int = Math.abs(caretOffset - p1!!)
                    val o1OnSameLine: Boolean = p0 >= lineStartOffset && p0 <= lineEndOffset
                    val o2OnSameLine: Boolean = p1 >= lineStartOffset && p1 <= lineEndOffset
                    if (i1 > i2) {
                        if (!o2OnSameLine && o1OnSameLine) {
                            return -1
                        }
                        return 1
                    } else
                        if (i1 == i2) {
                            return 0
                        } else {
                            if (!o1OnSameLine && o2OnSameLine) {
                                return 1
                            }
                            return -1
                        }
                }
            })

            startResult = 0
            endResult = allowedCount

            eventDispatcher?.multicaster?.stateChanged(ChangeEvent("AceFinder"))
        })
    }

    fun findAllVisible(): List<Int> {
        //System.out.println("----- findAllVisible");
        val visualLineAtTopOfScreen = getVisualLineAtTopOfScreen(editor)
        val firstLine = visualLineToLogicalLine(editor, visualLineAtTopOfScreen)
        val startOffset = getLineStartOffset(editor, firstLine)

        val height = getScreenHeight(editor)
        val lastLine = visualLineToLogicalLine(editor, visualLineAtTopOfScreen + height)
        val endOffset = normalizeOffset(editor, lastLine, getLineEndOffset(editor, lastLine, true), true)

        val text = document.charsSequence.toString().substring(startOffset, endOffset)
        val offsets = ArrayList<Int>()

        var offset = 0
        var result = findManager.findString(text, offset, findModel, virtualFile)
        while (result.isStringFound) {
            var resultOffset =
                    if (getEndOffset)
                        result.endOffset - 1
                    else
                        result.startOffset

            offsets.add(startOffset + resultOffset + customOffset)
            offset = result.endOffset

            result = findManager.findString(text, offset, findModel, virtualFile)
        }

        return offsets
    }

    fun expandResults() {
        startResult += allowedCount
        endResult += allowedCount
        checkForReset()
    }

    fun contractResults() {
        startResult -= allowedCount
        endResult -= allowedCount
        checkForReset()
    }

    fun checkForReset() {
        if (startResult < 0) startResult = 0
        if (endResult < allowedCount) endResult = allowedCount
    }

    fun addResultsReadyListener(changeListener: ChangeListener) {
        eventDispatcher?.addListener(changeListener)
    }

    fun getAllowedCharacters(): CharSequence {
        return ALLOWED_CHARACTERS
    }
}