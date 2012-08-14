package com.johnlindquist.acejump;

import com.google.common.collect.Lists;
import com.intellij.codeInsight.editorActions.SelectWordUtil;
import com.intellij.find.FindManager;
import com.intellij.find.FindModel;
import com.intellij.find.FindResult;
import com.intellij.ide.ui.UISettings;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.actionSystem.impl.ActionManagerImpl;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.*;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.colors.EditorColorsScheme;
import com.intellij.openapi.editor.impl.DocumentImpl;
import com.intellij.openapi.editor.impl.EditorImpl;
import com.intellij.openapi.editor.impl.FoldingModelImpl;
import com.intellij.openapi.editor.impl.ScrollingModelImpl;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.ComponentPopupBuilder;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import com.intellij.ui.awt.RelativePoint;
import com.intellij.ui.popup.AbstractPopup;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.text.BadLocationException;
import java.awt.*;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.util.*;
import java.util.List;

/**
 * User: John Lindquist
 * Date: 8/6/2012
 * Time: 12:10 AM
 */
public class AceJumpAction extends AnAction {

    protected Project project;
    protected EditorImpl editor;
    protected FindModel findModel;
    protected FindManager findManager;
    protected AbstractPopup popup;
    protected VirtualFile virtualFile;
    protected DocumentImpl document;
    protected FoldingModelImpl foldingModel;
    protected SearchBox searchBox;
    protected DataContext dataContext;
    protected AnActionEvent inputEvent;
    protected CaretModel caretModel;

    private CharSequence allowedCharacters = "abcdefghijklmnopqrstuvwxyz0123456789";//-=[];',./";
    private Font font;
    private Graphics2D aceGraphics;
    private AceCanvas aceCanvas;
    private EditorColorsScheme scheme;
    private int allowedCount;

    protected HashMap<String, Integer> offsetHash = new HashMap<String, Integer>();

    public void actionPerformed(AnActionEvent e) {

        allowedCount = allowedCharacters.length();
        inputEvent = e;

        project = e.getData(PlatformDataKeys.PROJECT);
        editor = (EditorImpl) e.getData(PlatformDataKeys.EDITOR);
        virtualFile = e.getData(PlatformDataKeys.VIRTUAL_FILE);
        document = (DocumentImpl) editor.getDocument();
        foldingModel = editor.getFoldingModel();
        dataContext = e.getDataContext();
        caretModel = editor.getCaretModel();

        findManager = FindManager.getInstance(project);
        findModel = createFindModel(findManager);

        scheme = EditorColorsManager.getInstance().getGlobalScheme();

        font = new Font(scheme.getEditorFontName(), Font.BOLD, scheme.getEditorFontSize());
        searchBox = new SearchBox();

        searchBox.setFont(font);

        ComponentPopupBuilder popupBuilder = JBPopupFactory.getInstance().createComponentPopupBuilder(searchBox, searchBox);
        popupBuilder.setCancelKeyEnabled(true);

        popup = (AbstractPopup) popupBuilder.createPopup();


        popup.show(guessBestLocation(editor));

        Dimension dimension = new Dimension(searchBox.getFontMetrics(font).stringWidth("w") * 2, editor.getLineHeight());
        popup.setSize(dimension);
        searchBox.setSize(dimension);
        searchBox.setFocusable(true);
        searchBox.requestFocus();
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

    public RelativePoint guessBestLocation(Editor editor) {
        VisualPosition logicalPosition = editor.getCaretModel().getVisualPosition();
        return getPointFromVisualPosition(editor, logicalPosition);
    }

    protected static RelativePoint getPointFromVisualPosition(Editor editor, VisualPosition logicalPosition) {
        Point p = editor.visualPositionToXY(new VisualPosition(logicalPosition.line, logicalPosition.column));
        return new RelativePoint(editor.getContentComponent(), p);
    }

    protected void moveCaret(Integer offset) {
        editor.getCaretModel().moveToOffset(offset);
    }

    protected void clearSelection() {
        aceCanvas.setBallonInfos(null);
        aceCanvas.repaint();
        offsetHash.clear();
    }

    protected class SearchBox extends JTextField {
        private ArrayList<JBPopup> resultPopups = new ArrayList<JBPopup>();
        protected int key;
        protected List<Integer> results;
        protected int startResult;
        protected int endResult;
        private SearchArea searchArea;
        private boolean searchMode = true;
        private boolean mnemonicsDisabled;


        @Override
        protected void paintBorder(Graphics g) {
            //do nothing
        }


        @Override
        public Dimension getPreferredSize() {
            return new Dimension(getFontMetrics(getFont()).stringWidth("w"), editor.getLineHeight());
        }

        //todo: refactor, extract and clean up
        public SearchBox() {
            final UISettings settings = UISettings.getInstance();
            addFocusListener(new FocusListener() {
                @Override
                public void focusGained(FocusEvent e) {
                    JComponent contentComponent = editor.getContentComponent();
                    aceCanvas = new AceCanvas();

                    contentComponent.add(aceCanvas);
                    JViewport viewport = editor.getScrollPane().getViewport();
                    //the 1000s are for the panels on the sides, hopefully user testing will find any holes
                    aceCanvas.setBounds(0, 0, viewport.getWidth() + 1000, viewport.getHeight() + 1000);
                    //System.out.println(aceCanvas.getWidth());

                    JRootPane rootPane = editor.getComponent().getRootPane();
                    Point locationOnScreen = SwingUtilities.convertPoint(aceCanvas, aceCanvas.getLocation(), rootPane);
                    aceCanvas.setLocation(-locationOnScreen.x, -locationOnScreen.y);

                    aceGraphics = (Graphics2D) aceCanvas.getGraphics();
                    aceGraphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    aceGraphics.setClip(0, 0, aceCanvas.getWidth(), aceCanvas.getHeight());
                    mnemonicsDisabled = settings.DISABLE_MNEMONICS;

                    if (!mnemonicsDisabled) {
                        settings.DISABLE_MNEMONICS = true;
                        settings.fireUISettingsChanged();
                    }
                }

                @Override
                public void focusLost(FocusEvent e) {
                    clearSelection();
                    if (!mnemonicsDisabled) {
                        settings.DISABLE_MNEMONICS = false;
                        settings.fireUISettingsChanged();
                    }
                }
            });
        }

        //todo: clean up keys
        @Override
        protected void processKeyEvent(final KeyEvent e) {

            if (getText().length() == 0) {
                searchMode = true;
            }

            //todo: refactor to behaviors, just spiking for now
            boolean isSpecialChar = false;

            if (e.getID() == KeyEvent.KEY_RELEASED) {
                switch (e.getKeyCode()) {
                    case KeyEvent.VK_HOME:
                        findText("^.", true);
                        //the textfield needs to have a char to read/delete for consistent behavior
                        setText(" ");
                        searchMode = false;
                        isSpecialChar = true;
                        break;
                    case KeyEvent.VK_END:
                        findText("\n", true);
                        setText(" ");
                        searchMode = false;
                        isSpecialChar = true;
                        break;

                    case KeyEvent.VK_SPACE:
                        searchMode = false;
                        isSpecialChar = true;
                        break;

                }
            }

            if (e.getID() == KeyEvent.KEY_PRESSED) {
                switch (e.getKeyCode()) {
                    case KeyEvent.VK_BACK_SPACE:
                        searchMode = false;
                        clearSelection();
                        break;

                    case KeyEvent.VK_ENTER:
                        if (!e.isShiftDown()) {
                            startResult += allowedCount;
                            endResult += allowedCount;
                        } else {
                            startResult -= allowedCount;
                            endResult -= allowedCount;
                        }
                        if (startResult < 0) {
                            startResult = 0;
                        }
                        if (endResult < allowedCount) {
                            endResult = allowedCount;
                        }
                        showBalloons(results, startResult, endResult);
                        isSpecialChar = true;
                        break;
                }
            }

            //System.out.println("start: " + startResult + " end: " + endResult);
            if (isSpecialChar) {
                return;
            }
            super.processKeyEvent(e);

            //only watch "key_typed" events
            if (e.getID() != KeyEvent.KEY_TYPED) {
                return;
            }


            char keyChar = e.getKeyChar();
            key = Character.getNumericValue(keyChar);

            if (!searchMode) {
                Integer offset = offsetHash.get(getLowerCaseStringFromChar(keyChar));
                if (offset != null) {

                    clearSelection();
                    popup.cancel();
                    if (e.isShiftDown()) {
                        editor.getSelectionModel().removeSelection();
                        int caretOffset = caretModel.getOffset();
                        int offsetModifer = 1;
                        if (offset < caretOffset) {
                            offset = offset + searchBox.getText().length();
                            offsetModifer = -2;
                        }
                        editor.getSelectionModel().setSelection(caretOffset, offset + offsetModifer);
                    } else if (e.isAltDown()) {
                        moveCaret(offset);
                        selectWordAtCaret();

                        ActionManager actionManager = ActionManagerImpl.getInstance();
                        final AnAction action = actionManager.getAction(IdeActions.ACTION_CODE_COMPLETION);
                        AnActionEvent event = new AnActionEvent(null, editor.getDataContext(), IdeActions.ACTION_CODE_COMPLETION, inputEvent.getPresentation(), ActionManager.getInstance(), 0);
                        action.actionPerformed(event);

                    } else {
                        moveCaret(offset);
                    }
                }

            }

            if (searchMode && getText().length() == 1) {
                findText(getText(), false);
                searchMode = false;
                return;
            }

            if (getText().length() == 2) {
                try {
                    setText(getText(0, 1));
                } catch (BadLocationException e1) {
                    e1.printStackTrace();
                }
            }

        }

        protected void selectWordAtCaret() {
            CharSequence text = document.getCharsSequence();
            List<TextRange> ranges = new ArrayList<TextRange>();
            SelectWordUtil.addWordSelection(false, text, editor.getCaretModel().getOffset(), ranges);
            if (ranges.isEmpty()) return;

            int startWordOffset = Math.max(0, ranges.get(0).getStartOffset());
            int endWordOffset = Math.min(ranges.get(0).getEndOffset(), document.getTextLength());

            if (ranges.size() == 2 && editor.getSelectionModel().getSelectionStart() == startWordOffset &&
                    editor.getSelectionModel().getSelectionEnd() == endWordOffset) {
                startWordOffset = Math.max(0, ranges.get(1).getStartOffset());
                endWordOffset = Math.min(ranges.get(1).getEndOffset(), document.getTextLength());
            }

            editor.getSelectionModel().setSelection(startWordOffset, endWordOffset);
        }

        /*todo: I hate this. Strict mapping to my USA keyboard :(*/
        private String getLowerCaseStringFromChar(char keyChar) {

            String s = String.valueOf(keyChar);
            if (s.equals("!")) {
                return "1";

            } else if (s.equals("@")) {
                return "2";

            } else if (s.equals("#")) {
                return "3";

            } else if (s.equals("$")) {
                return "4";

            } else if (s.equals("%")) {
                return "5";

            } else if (s.equals("^")) {
                return "6";

            } else if (s.equals("&")) {
                return "7";

            } else if (s.equals("*")) {
                return "8";

            } else if (s.equals("(")) {
                return "9";

            } else if (s.equals(")")) {
                return "0";
            } else if (s.equals("_")) {
                return "-";
            } else if (s.equals("+")) {
                return "=";
            } else if (s.equals("{")) {
                return "[";
            } else if (s.equals("}")) {
                return "]";
            } else if (s.equals("|")) {
                return "\\";
            } else if (s.equals(":")) {
                return ";";
            } else if (s.equals("<")) {
                return ",";
            } else if (s.equals(">")) {
                return ".";
            } else if (s.equals("?")) {
                return "/";
            }
            return s.toLowerCase();
        }

        private void findText(String text, boolean isRegEx) {
            findModel.setStringToFind(text);
            findModel.setRegularExpressions(isRegEx);

            ApplicationManager.getApplication().runReadAction(new Runnable() {
                @Override
                public void run() {
                    searchArea = new SearchArea();
                    searchArea.invoke();
                    if (searchArea.getPsiFile() == null) return;
                    results = new ArrayList<Integer>();
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
                    endResult = allowedCharacters.length();

                    showBalloons(results, startResult, endResult);
                }
            });
        }

        private void showBalloons(List<Integer> results, int start, int end) {
            offsetHash.clear();
            int size = results.size();
            if (end > size) {
                end = size;
            }

            Vector<Pair<String, Point>> ballonInfos = new Vector<Pair<String, Point>>();
            float hOffset = font.getSize() - (font.getSize() * scheme.getLineSpacing());
            for (int i = start; i < end; i++) {

                int textOffset = results.get(i);
                RelativePoint point = getPointFromVisualPosition(editor, editor.offsetToVisualPosition(textOffset));
                Point originalPoint = point.getOriginalPoint();
                originalPoint.translate(0, (int) -hOffset);
                char resultChar = allowedCharacters.charAt(i % allowedCharacters.length());
                final String text = String.valueOf(resultChar);

                ballonInfos.add(new Pair<String, Point>(text, originalPoint));
                offsetHash.put(text, textOffset);
            }


            aceCanvas.setFont(font);
            aceCanvas.setLineHeight(editor.getLineHeight());
            aceCanvas.setLineSpacing(scheme.getLineSpacing());
            aceCanvas.setBallonInfos(Lists.reverse(ballonInfos));
            aceCanvas.setBackgroundForegroundColors(new Pair<Color, Color>(scheme.getDefaultBackground(), scheme.getDefaultForeground()));

            aceCanvas.repaint();
        }


        @Nullable
        protected java.util.List<Integer> findAllVisible() {
            //System.out.println("----- findAllVisible");
            int offset = searchArea.getOffset();
            int endOffset = searchArea.getEndOffset();
            CharSequence text = searchArea.getText();
            List<Integer> offsets = new ArrayList<Integer>();
            while (offset < endOffset) {
                //skip folded regions. Re-think approach.
                offset = checkFolded(offset);

                FindResult result = findManager.findString(text, offset, findModel, virtualFile);
                if (!result.isStringFound()) {
                    //System.out.println(findModel.getStringToFind() + ": not found");
                    break;
                }
                offsets.add(result.getStartOffset());
                offset = result.getEndOffset();
            }

            return offsets;
        }

        //todo: can probably refactor this out now
        public class SearchArea {
            private PsiFile psiFile;
            private CharSequence text;
            private Rectangle visibleArea;
            private int offset;
            private int endOffset;

            public PsiFile getPsiFile() {
                return psiFile;
            }

            public CharSequence getText() {
                return text;
            }

            public int getOffset() {
                return offset;
            }

            public int getEndOffset() {
                return endOffset;
            }

            public void invoke() {
                psiFile = PsiDocumentManager.getInstance(project).getPsiFile(document);
                if (psiFile == null) {
                    return;
                }

                text = document.getCharsSequence();

                JViewport viewport = editor.getScrollPane().getViewport();
                double viewportY = viewport.getViewPosition().getY();

                ScrollingModelImpl scrollingModel = (ScrollingModelImpl) editor.getScrollingModel();
                //you need the "visibleArea" to see if the point is inside of it
                visibleArea = scrollingModel.getVisibleArea();
                //it seems like visibleArea can miss a top line, so I'm manually adding one. Investigate later.
                visibleArea.setRect(visibleArea.x, visibleArea.y - editor.getLineHeight(), visibleArea.width, visibleArea.height + editor.getLineHeight());

                //TODO: Can this be more accurate?
                double linesAbove = viewportY / editor.getLineHeight();
                double visibleLines = visibleArea.getHeight() / editor.getLineHeight();
                if (linesAbove < 0) linesAbove = 0;
                offset = document.getLineStartOffset((int) linesAbove);
                int endLine = (int) (linesAbove + visibleLines);
                int lineCount = document.getLineCount() - 1;
                int foldedLinesCountBefore = editor.getFoldingModel().getFoldedLinesCountBefore(document.getTextLength() + 1);
                if (endLine + foldedLinesCountBefore > lineCount) {
                    endLine = lineCount;
                }
                endOffset = document.getLineEndOffset(endLine);
            }
        }
    }

    private int checkFolded(int offset) {
        for (FoldRegion foldRegion : editor.getFoldingModel().fetchCollapsedAt(offset)) {
            offset = foldRegion.getEndOffset() + 1;
        }
        return offset;
    }
}
