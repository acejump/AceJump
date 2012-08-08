package com.johnlindquist.acejump;

import com.intellij.find.FindManager;
import com.intellij.find.FindModel;
import com.intellij.find.FindResult;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.actionSystem.impl.ActionManagerImpl;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.Result;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.CaretModel;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.VisualPosition;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.colors.EditorColorsScheme;
import com.intellij.openapi.editor.impl.DocumentImpl;
import com.intellij.openapi.editor.impl.EditorImpl;
import com.intellij.openapi.editor.impl.FoldingModelImpl;
import com.intellij.openapi.editor.impl.ScrollingModelImpl;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.*;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import com.intellij.ui.awt.RelativePoint;
import com.intellij.ui.popup.AbstractPopup;
import com.intellij.usageView.UsageInfo;
import com.intellij.usages.UsageInfo2UsageAdapter;
import org.jetbrains.annotations.Nullable;

import javax.help.HomeAction;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
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

    private CharSequence allowedCharacters = "abcdefghijklmnopqrstuvwxyz0123456789-=[];',./";
    private Font font;

    public void actionPerformed(AnActionEvent e) {
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

//        font = editor.getComponent().getFont();

        EditorColorsScheme scheme = EditorColorsManager.getInstance().getGlobalScheme();
        font = new Font(scheme.getEditorFontName(), Font.BOLD, scheme.getEditorFontSize());
        searchBox = new SearchBox();

        searchBox.setFont(font);

        ComponentPopupBuilder popupBuilder = JBPopupFactory.getInstance().createComponentPopupBuilder(searchBox, searchBox);
        popupBuilder.setCancelKeyEnabled(true);

        final List<Pair<ActionListener, KeyStroke>>
                keyboardActions = Collections.singletonList(Pair.<ActionListener, KeyStroke>create(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                searchBox.hideBalloons();
            }
        }, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0)));


        popup = (AbstractPopup) popupBuilder.createPopup();

//        popup.getContent().setBorder(new BlockBorder());

        popup.show(guessBestLocation(editor));

        popup.addListener(new JBPopupAdapter() {
            @Override
            public void onClosed(LightweightWindowEvent event) {
                searchBox.hideBalloons();
            }
        });


        searchBox.setFocusable(true);
        searchBox.requestFocus();

//        searchBox.findText();
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
        RelativePoint pointFromVisualPosition = getPointFromVisualPosition(editor, logicalPosition);
        pointFromVisualPosition.getOriginalPoint().translate(0, -searchBox.getHeight());
        return pointFromVisualPosition;
    }

    protected RelativePoint getPointFromVisualPosition(Editor editor, VisualPosition logicalPosition) {
        Point p = editor.visualPositionToXY(new VisualPosition(logicalPosition.line + 1, logicalPosition.column));
        return new RelativePoint(editor.getContentComponent(), p);
    }

    protected void moveCaret(Integer offset) {
        editor.getCaretModel().moveToOffset(offset);
//        editor.getScrollingModel().scrollToCaret(ScrollType.CENTER);
    }

    protected void addNewLineAfterCaret() {
        ActionManager actionManager = ActionManagerImpl.getInstance();
        final AnAction action = actionManager.getAction(IdeActions.ACTION_EDITOR_START_NEW_LINE);
        AnActionEvent event = new AnActionEvent(null, editor.getDataContext(), IdeActions.ACTION_EDITOR_START_NEW_LINE, inputEvent.getPresentation(), ActionManager.getInstance(), 0);
        action.actionPerformed(event);
    }

    protected void addNewLineBeforeCaret() {
        ActionManager actionManager = ActionManagerImpl.getInstance();
        AnAction action = actionManager.getAction(IdeActions.ACTION_EDITOR_MOVE_CARET_UP);
        AnActionEvent event = new AnActionEvent(null, editor.getDataContext(), IdeActions.ACTION_EDITOR_COMPLETE_STATEMENT, inputEvent.getPresentation(), ActionManager.getInstance(), 0);
        action.actionPerformed(event);

        addNewLineAfterCaret();
    }

    protected void addSpaceBeforeCaret() {
        addSpace();
        moveCaretRight();
    }

    private void addSpace() {
        ApplicationManager.getApplication().runWriteAction(new Runnable() {
            @Override
            public void run() {
                document.insertString(caretModel.getOffset(), " ");
            }
        });
    }

    protected void addSpaceAfterCaret() {
        moveCaretRight();
        addSpaceBeforeCaret();
    }

    private void moveCaretRight() {
        ActionManager actionManager = ActionManagerImpl.getInstance();
        AnAction action = actionManager.getAction(IdeActions.ACTION_EDITOR_MOVE_CARET_RIGHT);
        AnActionEvent event = new AnActionEvent(null, editor.getDataContext(), IdeActions.ACTION_EDITOR_MOVE_CARET_RIGHT, inputEvent.getPresentation(), ActionManager.getInstance(), 0);
        action.actionPerformed(event);
    }

    private void moveCaretLeft() {
        ActionManager actionManager = ActionManagerImpl.getInstance();
        AnAction action = actionManager.getAction(IdeActions.ACTION_EDITOR_MOVE_CARET_LEFT);
        AnActionEvent event = new AnActionEvent(null, editor.getDataContext(), IdeActions.ACTION_EDITOR_MOVE_CARET_LEFT, inputEvent.getPresentation(), ActionManager.getInstance(), 0);
        action.actionPerformed(event);
    }

    protected void applyModifier(KeyEvent e) {
        if (e.isShiftDown() && e.isControlDown()) {
            addNewLineBeforeCaret();
        } else if (e.isAltDown() && e.isControlDown()) {
            addSpaceBeforeCaret();
        } else if (e.isControlDown()) {
            addNewLineAfterCaret();
        } else if (e.isAltDown()) {
            addSpaceAfterCaret();
        }
    }

    protected void completeCaretMove(Integer offset) {
    }

    protected void clearSelection() {
        popup.cancel();
        editor.getSelectionModel().removeSelection();
    }

    protected class SearchBox extends JTextField {
        private ArrayList<JBPopup> resultPopups = new ArrayList<JBPopup>();
        protected HashMap<String, Integer> hashMap = new HashMap<String, Integer>();
        protected int key;
        protected List<Integer> results;
        protected int startResult;
        protected int endResult;
        private SearchArea searchArea;
        private boolean searchMode = true;



        @Override
        protected void processKeyEvent(final KeyEvent e) {
            super.processKeyEvent(e);
            //only handle "press" events
            if(e.getID() == KeyEvent.KEY_RELEASED) return;

            char keyChar = e.getKeyChar();
            key = Character.getNumericValue(keyChar);
            int keyCode = e.getKeyCode();


            boolean hasChar = getText().length() == 1;


            if (hasChar && !searchMode) {
                System.out.println("navigating" + e.getKeyChar());
//                System.out.println("value: " + key + " code " + keyCode + " char " + e.getKeyChar() + " location: " + e.getKeyLocation());
//                System.out.println("---------passed: " + "value: " + key + " code " + keyCode + " char " + e.getKeyChar() + " location: " + e.getKeyLocation());


                Integer offset = hashMap.get(getLowerCaseStringFromChar(keyChar));
                if (offset != null) {
                    clearSelection();
                    if (e.isShiftDown()) {
                        editor.getSelectionModel().removeSelection();
                        int caretOffset = caretModel.getOffset();
                        int offsetModifer = 1;
                        if (offset < caretOffset) {
                            offset = offset + searchBox.getText().length();
                            offsetModifer = -1;
                        }
                        editor.getSelectionModel().setSelection(caretOffset, offset + offsetModifer);
                    } else {
                        moveCaret(offset);
                    }
                    new WriteCommandAction(project) {
                        @Override
                        protected void run(Result result) throws Throwable {
                            applyModifier(e);
                        }
                    }.execute();
                    try {
                        completeCaretMove(offset);
                    } catch (Exception e1) {
                        e1.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                    }
                }

            } else if (keyCode == KeyEvent.VK_BACK_SPACE) {
                hideBalloons();
            }

            if (hasChar && searchMode) {
                System.out.println("searching " + e.getKeyChar() + "\n");
                startFindText();
                searchMode = false;
            }

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


        private void startFindText() {
            String text = getText();

            int width = 11 + getFontMetrics(getFont()).stringWidth(getText());
            popup.setSize(new Dimension(width, editor.getLineHeight()));
            setSize(width, editor.getLineHeight());
            findText();
        }

        private void findText() {
            findModel.setRegularExpressions(false);
            String text = getText();
            if (text.equals(" ")) {
                text = "^.";
                findModel.setRegularExpressions(true);
            }
            findModel.setStringToFind(text);
            if (text.equals("\n")) {
                findModel.setRegularExpressions(true);
            }
            findModel.setStringToFind(text);

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

                    showBalloons(results, startResult, endResult);//To change body of implemented methods use File | Settings | File Templates.
                }
            });
        }

        private void showBalloons(List<Integer> results, int start, int end) {
            hideBalloons();


            int size = results.size();
            if (end > size) {
                end = size;
            }


            final HashMap<JBPopup, RelativePoint> jbPopupRelativePointHashMap = new HashMap<JBPopup, RelativePoint>();
            for (int i = start; i < end; i++) {

                int textOffset = results.get(i);
                RelativePoint point = getPointFromVisualPosition(editor, editor.offsetToVisualPosition(textOffset));
                Point originalPoint = point.getOriginalPoint();
                originalPoint.translate(-3, -editor.getLineHeight());
//                System.out.println(originalPoint.getX() + " " + originalPoint.getY());

                GridBagLayout layout = new GridBagLayout();
                JPanel jPanel = new JPanel(layout);
//                jPanel.setBackground(new Color(255, 255, 255));
                char resultChar = allowedCharacters.charAt(i % allowedCharacters.length());
                String text = String.valueOf(resultChar);

                JLabel jLabel = new JLabel(text);
                jLabel.setFont(font);


//                jLabel.setBackground(new Color(192, 192, 192));
                jLabel.setHorizontalAlignment(CENTER);
                jLabel.setFocusable(false);
                jLabel.setSize(jLabel.getWidth(), editor.getLineHeight());

                jPanel.add(jLabel);

                GridBagConstraints constraints = new GridBagConstraints();
                constraints.ipady = editor.getLineHeight() / 4;
                layout.setConstraints(jLabel, constraints);

//                jPanel.setPreferredSize(new Dimension(3, editor.getLineHeight()));


                ComponentPopupBuilder componentPopupBuilder = JBPopupFactory.getInstance().createComponentPopupBuilder(jPanel, jPanel);
                componentPopupBuilder.setCancelOnClickOutside(true);
                componentPopupBuilder.setCancelOnOtherWindowOpen(true);
                componentPopupBuilder.setCancelKeyEnabled(true);
                componentPopupBuilder.setMovable(false);
                componentPopupBuilder.setFocusable(false);
                componentPopupBuilder.setBelongsToGlobalPopupStack(false);
                JBPopup jPanelPopup = componentPopupBuilder.createPopup();
                jbPopupRelativePointHashMap.put(jPanelPopup, point);


                resultPopups.add(jPanelPopup);
                hashMap.put(text, textOffset);
            }

            Collections.sort(resultPopups, new Comparator<JBPopup>() {
                @Override
                public int compare(JBPopup o1, JBPopup o2) {
                    RelativePoint point1 = jbPopupRelativePointHashMap.get(o1);
                    RelativePoint point2 = jbPopupRelativePointHashMap.get(o2);

                    if (point1.getOriginalPoint().y < point2.getOriginalPoint().y) {
                        return 1;
                    } else if (point1.getOriginalPoint().y == point2.getOriginalPoint().y) {
                        return 0;
                    } else {
                        return -1;
                    }
                }
            });

            for (JBPopup balloon : resultPopups) {
                RelativePoint point = jbPopupRelativePointHashMap.get(balloon);
                balloon.show(point);
            }


        }

        private void hideBalloons() {
            for (JBPopup balloon1 : resultPopups) {
                balloon1.dispose();
            }
            resultPopups.clear();
            hashMap.clear();
        }

        @Nullable
        protected java.util.List<Integer> findAllVisible() {
//            System.out.println("----- findAllVisible");
            int offset = searchArea.getOffset();
            int endOffset = searchArea.getEndOffset();
            CharSequence text = searchArea.getText();
            PsiFile psiFile = searchArea.getPsiFile();
            Rectangle visibleArea = searchArea.getVisibleArea();


            List<Integer> offsets = new ArrayList<Integer>();
            while (offset < endOffset) {
//                System.out.println("offset: " + offset + "/" + endOffset);

//                System.out.println("Finding: " + findModel.getStringToFind() + " = " + offset);
                FindResult result = findManager.findString(text, offset, findModel, virtualFile);
                if (!result.isStringFound()) {
//                    System.out.println(findModel.getStringToFind() + ": not found");
                    break;
                }

//                System.out.println("result: " + result.toString());

                UsageInfo2UsageAdapter usageAdapter = new UsageInfo2UsageAdapter(new UsageInfo(psiFile, result.getStartOffset(), result.getEndOffset()));
                Point point = editor.logicalPositionToXY(editor.offsetToLogicalPosition(usageAdapter.getUsageInfo().getNavigationOffset()));
                if (visibleArea.contains(point)) {
                    UsageInfo usageInfo = usageAdapter.getUsageInfo();
                    int navigationOffset = usageInfo.getNavigationOffset();
                    if (navigationOffset != caretModel.getOffset()) {
                        if (!results.contains(navigationOffset)) {
//                            System.out.println("Adding: " + navigationOffset + "-> " + usageAdapter.getPlainText());
                            offsets.add(navigationOffset);
                        }
                    }
                }


                final int prevOffset = offset;
                offset = result.getEndOffset();


                if (prevOffset == offset) {
                    ++offset;
                }
            }

            return offsets;
        }

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

            public Rectangle getVisibleArea() {
                return visibleArea;
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
                visibleArea = scrollingModel.getVisibleArea();

                double height = visibleArea.getHeight();
                //TODO: Can this be more accurate?
                double linesAbove = viewportY / editor.getLineHeight();
                height += editor.getLineHeight() * 4;
                double visibleLines = height / editor.getLineHeight();
                double padding = 20;
                visibleLines += padding;
                //            System.out.println("visibleLines: " + visibleLines);

                if (linesAbove < 0) linesAbove = 0;
                offset = document.getLineStartOffset((int) linesAbove);
                int endLine = (int) (linesAbove + visibleLines);
                int lineCount = document.getLineCount() - 1;
                if (endLine > lineCount) {
                    endLine = lineCount;
                }
                endOffset = document.getLineEndOffset(endLine);
            }
        }
    }
}
