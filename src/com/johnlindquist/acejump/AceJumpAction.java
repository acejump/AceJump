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
import com.intellij.openapi.editor.impl.DocumentImpl;
import com.intellij.openapi.editor.impl.EditorImpl;
import com.intellij.openapi.editor.impl.FoldingModelImpl;
import com.intellij.openapi.editor.impl.ScrollingModelImpl;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.*;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import com.intellij.psi.codeStyle.NameUtil;
import com.intellij.psi.impl.cache.impl.id.IdTableBuilding;
import com.intellij.ui.awt.RelativePoint;
import com.intellij.ui.popup.AbstractPopup;
import com.intellij.usageView.UsageInfo;
import com.intellij.usages.UsageInfo2UsageAdapter;
import com.intellij.util.ArrayUtil;
import com.intellij.util.ui.BlockBorder;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.Timer;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.*;
import java.util.List;

/**
 * User: John Lindquist
 * Date: 9/8/11
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

    private CharSequence alphabet = "abcdefghijklmnopqrstuvwxyz";
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

        font = new Font("Verdana", Font.BOLD, 11);
        searchBox = new SearchBox();

        searchBox.setFont(font);
        searchBox.setSize(searchBox.getPreferredSize());

        ComponentPopupBuilder popupBuilder = JBPopupFactory.getInstance().createComponentPopupBuilder(searchBox, searchBox);
        popup = (AbstractPopup) popupBuilder.createPopup();

        popup.getContent().setBorder(new BlockBorder());

        popup.show(guessBestLocation(editor));
        popup.addListener(new JBPopupAdapter() {
            @Override
            public void onClosed(LightweightWindowEvent event) {
                searchBox.hideBalloons();
            }
        });
        searchBox.requestFocus();

        searchBox.findText();
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
        pointFromVisualPosition.getOriginalPoint().translate(-4, -searchBox.getHeight() - editor.getLineHeight() + 2);
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
        private static final int ALLOWED_RESULTS = 26;
        private ArrayList<Balloon> balloons = new ArrayList<Balloon>();
        protected HashMap<String, Integer> hashMap = new HashMap<String, Integer>();
        protected int key;
        protected List<Integer> results;
        protected int startResult;
        protected int endResult;
        private SearchArea searchArea;

        private SearchBox() {

            addKeyListener(new KeyAdapter() {
                @Override
                public void keyPressed(final KeyEvent e) {
                    char keyChar = e.getKeyChar();
                    key = Character.getNumericValue(keyChar);
                    int keyCode = e.getKeyCode();

                    if (searchBox.getText().length() == 1) {
                        System.out.println("value: " + key + " code " + keyCode + " char " + e.getKeyChar() + " location: " + e.getKeyLocation());
                        System.out.println("---------passed: " + "value: " + key + " code " + keyCode + " char " + e.getKeyChar() + " location: " + e.getKeyLocation());

                        final Integer offset = hashMap.get(String.valueOf(keyChar));
                        if (offset != null) {
                            clearSelection();
                            moveCaret(offset);
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
                    }
                    else if(keyCode == KeyEvent.VK_SPACE){

                    }

                    else if (keyCode == KeyEvent.VK_BACK_SPACE) {
                        hideBalloons();
                    } else {
                        showBalloons(results, startResult, endResult);
                    }


                }
            });

            getDocument().addDocumentListener(new DocumentListener() {
                @Override
                public void insertUpdate(DocumentEvent e) {
                    startFindText();
                }

                @Override
                public void removeUpdate(DocumentEvent e) {
                    startFindText();
                }

                @Override
                public void changedUpdate(DocumentEvent e) {
                }
            });

        }

        private void startFindText() {

            int delay = 100;

            String text = getText();
            int length = text.length();

            int width = 11 + getFontMetrics(getFont()).stringWidth(getText());
            int height = getHeight();
            popup.setSize(new Dimension(width, height));
            setSize(width, height);
//            System.out.println("the single char is: " + text);

            char c = text.charAt(0);
            findText();
        }

        private void findText() {
            findModel.setRegularExpressions(false);
            String text = getText();
            if(text.equals(" ")) {
                text = "^.";
                findModel.setRegularExpressions(true);
            }
            findModel.setStringToFind(text);

            ApplicationManager.getApplication().runReadAction(new Runnable() {
                @Override
                public void run() {
                    searchArea = new SearchArea();
                    searchArea.invoke();
                    if (searchArea.getPsiFile() == null) return;

                    results = findAllVisible();

                    //camelCase logic
//                            findCamelCase();
                }

            });

            ApplicationManager.getApplication().invokeLater(new Runnable() {
                @Override
                public void run() {
//                            System.out.println("results: " + results);

                    final int caretOffset = editor.getCaretModel().getOffset();
                    RelativePoint caretPoint = getPointFromVisualPosition(editor, editor.offsetToVisualPosition(caretOffset));
                    final Point cP = caretPoint.getOriginalPoint();
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
                    endResult = ALLOWED_RESULTS;

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


            final HashMap<Balloon, RelativePoint> balloonPointHashMap = new HashMap<Balloon, RelativePoint>();
            for (int i = start; i < end; i++) {

                int textOffset = results.get(i);
                RelativePoint point = getPointFromVisualPosition(editor, editor.offsetToVisualPosition(textOffset));
                Point originalPoint = point.getOriginalPoint();
                originalPoint.translate(0, -editor.getLineHeight() / 2);
//                System.out.println(originalPoint.getX() + " " + originalPoint.getY());

                JPanel jPanel = new JPanel(new GridLayout());
                jPanel.setBackground(new Color(255, 255, 255));
                int resultIndex = i % ALLOWED_RESULTS;
                String text = String.valueOf(alphabet.charAt(i % ALLOWED_RESULTS));

                JLabel jLabel = new JLabel(text);
//                Font jLabelFont = new Font(jLabel.getFont().getName(), Font.BOLD, 11);
                jLabel.setFont(font);
                jLabel.setBackground(new Color(192, 192, 192));
                jLabel.setHorizontalAlignment(CENTER);
                jLabel.setFocusable(false);
                jLabel.setSize(jLabel.getWidth(), 5);
                jPanel.setFocusable(false);
                jPanel.add(jLabel);

                if (text.equals("Enter")) {
                    jPanel.setPreferredSize(new Dimension(45, 13));

                } else {
                    jPanel.setPreferredSize(new Dimension(19, 13));

                }

                BalloonBuilder balloonBuilder = JBPopupFactory.getInstance().createBalloonBuilder(jPanel);
                balloonBuilder.setFadeoutTime(0);
                balloonBuilder.setAnimationCycle(0);
                balloonBuilder.setHideOnClickOutside(true);
                balloonBuilder.setHideOnKeyOutside(true);
                balloonBuilder.setHideOnAction(true);
                balloonBuilder.setFillColor(new Color(221, 221, 221));
                balloonBuilder.setBorderColor(new Color(136, 136, 136));

                Balloon balloon = balloonBuilder.createBalloon();
                balloonPointHashMap.put(balloon, point);


                balloons.add(balloon);
                hashMap.put(text, textOffset);
            }

            Collections.sort(balloons, new Comparator<Balloon>() {
                @Override
                public int compare(Balloon o1, Balloon o2) {
                    RelativePoint point1 = balloonPointHashMap.get(o1);
                    RelativePoint point2 = balloonPointHashMap.get(o2);

                    if (point1.getOriginalPoint().y < point2.getOriginalPoint().y) {
                        return 1;
                    } else if (point1.getOriginalPoint().y == point2.getOriginalPoint().y) {
                        return 0;
                    } else {
                        return -1;
                    }
                }
            });

            for (int i = 0, balloonsSize = balloons.size(); i < balloonsSize; i++) {
                Balloon balloon = balloons.get(i);
                RelativePoint point = balloonPointHashMap.get(balloon);
                balloon.show(point, Balloon.Position.above);
            }


        }

        private void hideBalloons() {
            for (Balloon balloon1 : balloons) {
                balloon1.dispose();
            }
            balloons.clear();
            hashMap.clear();
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(20, 20);
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


    protected String[] calcWords(final String prefix, Editor editor) {
        final NameUtil.Matcher matcher = (NameUtil.Matcher) NameUtil.buildMatcher(prefix, 0, true, true);
        final Set<String> words = new HashSet<String>();
        CharSequence chars = editor.getDocument().getCharsSequence();

        IdTableBuilding.scanWords(new IdTableBuilding.ScanWordProcessor() {
            @Override
            public void run(CharSequence charSequence, @Nullable char[] chars, int start, int end) {
                final String word = charSequence.subSequence(start, end).toString();
                if (matcher.matches(word)) {
                    words.add(word);
                }
            }
        }, chars, 0, chars.length());

        ArrayList<String> sortedWords = new ArrayList<String>(words);
        Collections.sort(sortedWords);

        return ArrayUtil.toStringArray(sortedWords);
    }
}
