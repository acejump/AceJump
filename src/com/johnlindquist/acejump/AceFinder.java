package com.johnlindquist.acejump;

import com.intellij.find.FindManager;
import com.intellij.find.FindModel;
import com.intellij.find.FindResult;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.FoldRegion;
import com.intellij.openapi.editor.impl.DocumentImpl;
import com.intellij.openapi.editor.impl.EditorImpl;
import com.intellij.openapi.editor.impl.FoldingModelImpl;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.util.*;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: johnlindquist
 * Date: 8/23/12
 * Time: 3:47 PM
 */
public class AceFinder extends Observable {

    public static final CharSequence ALLOWED_CHARACTERS = "abcdefghijklmnopqrstuvwxyz";

    public static final String END_OF_LINE = "\\n|\\Z";
    public static final String BEGINNING_OF_LINE = "^.|\\n(?<!.\\n)";
    public static final String CODE_INDENTS = "^\\s*\\S";

    private int startResult;
    private int endResult;

    private final Project project;
    private final DocumentImpl document;
    private final EditorImpl editor;
    private final VirtualFile virtualFile;
    private FindManager findManager;
    private FindModel findModel;
    private final int allowedCount;
    public boolean getEndOffset;

    public List<Integer> getResults() {
        return results;
    }

    private List<Integer> results;

    public int getStartResult() {
        return startResult;
    }

    public int getEndResult() {
        return endResult;
    }

    public CharSequence getAllowedCharacters() {
        return ALLOWED_CHARACTERS;
    }

    public AceFinder(Project project, DocumentImpl document, EditorImpl editor, VirtualFile virtualFile) {
        this.project = project;
        this.document = document;
        this.editor = editor;
        this.virtualFile = virtualFile;
        allowedCount = ALLOWED_CHARACTERS.length();
    }

    protected FindModel createFindModel(FindManager findManager) {
        FindModel clone = (FindModel) findManager.getFindInFileModel().clone();
        clone.setFindAll(true);
        clone.setFromCursor(true);
        clone.setForward(true);
        clone.setRegularExpressions(false);
        clone.setWholeWordsOnly(false);
        clone.setCaseSensitive(false);
        clone.setSearchHighlighters(true);
        clone.setPreserveCase(false);

        return clone;
    }

    public void findText(String text, boolean isRegEx) {
        findManager = FindManager.getInstance(project);
        findModel = createFindModel(findManager);

        findModel.setStringToFind(text);
        findModel.setRegularExpressions(isRegEx);

        ApplicationManager.getApplication().runReadAction(new Runnable() {
            @Override
            public void run() {
                results = findAllVisible();
            }

        });

        ApplicationManager.getApplication().invokeLater(new Runnable() {
            @Override
            public void run() {

                final int caretOffset = editor.getCaretModel().getOffset();
                int lineNumber = document.getLineNumber(caretOffset);
                final int lineStartOffset = document.getLineStartOffset(lineNumber);
                final int lineEndOffset = document.getLineEndOffset(lineNumber);


                Collections.sort(results, new Comparator<Integer>() {
                    @Override
                    public int compare(Integer o1, Integer o2) {
                        int i1 = Math.abs(caretOffset - o1);
                        int i2 = Math.abs(caretOffset - o2);
                        boolean o1OnSameLine = o1 >= lineStartOffset && o1 <= lineEndOffset;
                        boolean o2OnSameLine = o2 >= lineStartOffset && o2 <= lineEndOffset;

                        if (i1 > i2) {
                            if (!o2OnSameLine && o1OnSameLine) {
                                return -1;
                            }
                            return 1;
                        } else if (i1 == i2) {
                            return 0;
                        } else {
                            if (!o1OnSameLine && o2OnSameLine) {
                                return 1;
                            }
                            return -1;
                        }
                    }
                });

                startResult = 0;
                endResult = allowedCount;

                setChanged();
                synchronized (this) {
                    notifyObservers();
                }
            }
        });
    }

    @Nullable
    protected java.util.List<Integer> findAllVisible() {
        //System.out.println("----- findAllVisible");
        int offset = 0;//searchArea.getOffset();
        int endOffset = document.getTextLength();//searchArea.getEndOffset();
        CharSequence text = document.getCharsSequence();//searchArea.getText();
        List<Integer> offsets = new ArrayList<Integer>();
        Rectangle visibleArea = editor.getScrollingModel().getVisibleArea();
        //add a lineHeight of padding
        visibleArea.setRect(visibleArea.x, visibleArea.y - editor.getLineHeight(), visibleArea.getWidth(), visibleArea.getHeight() + editor.getLineHeight() * 2);
        int maxLength = document.getCharsSequence().length();
        while (offset < endOffset) {
            //skip folded regions. Re-think approach.
            offset = checkFolded(offset);
            if (offset > maxLength) offset = maxLength;


            FindResult result = findManager.findString(text, offset, findModel, virtualFile);
            if (!result.isStringFound()) {
                //System.out.println(findModel.getStringToFind() + ": not found");
                break;
            }
            int resultOffset;
            if (getEndOffset) {
                resultOffset = result.getEndOffset() - 1;
            } else {
                resultOffset = result.getStartOffset();
            }
            if (visibleArea.contains(editor.logicalPositionToXY(editor.offsetToLogicalPosition(resultOffset)))) {
                offsets.add(resultOffset);
            }
            offset = result.getEndOffset();
        }

        return offsets;
    }


    private int checkFolded(int offset) {
        /*
            A contributor said casting as "FoldingModelImpl" fixes a bug on mac... So I'm leaving the casting in even
            thought I can't verify.
         */
        FoldingModelImpl foldingModel = (FoldingModelImpl) editor.getFoldingModel();
        for (FoldRegion foldRegion : foldingModel.fetchCollapsedAt(offset)) {
            offset = foldRegion.getEndOffset() + 1;
        }
        return offset;
    }

    public void expandResults() {
        startResult += allowedCount;
        endResult += allowedCount;
        checkForReset();
    }

    public void contractResults() {
        startResult -= allowedCount;
        endResult -= allowedCount;
        checkForReset();
    }

    protected void checkForReset() {
        if (startResult < 0) {
            startResult = 0;
        }
        if (endResult < allowedCount) {
            endResult = allowedCount;
        }
    }


}
