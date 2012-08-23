package com.johnlindquist.acejump;

import com.google.common.collect.Lists;
import com.intellij.codeInsight.editorActions.SelectWordUtil;
import com.intellij.find.FindManager;
import com.intellij.find.FindModel;
import com.intellij.ide.ui.UISettings;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.actionSystem.impl.ActionManagerImpl;
import com.intellij.openapi.editor.CaretModel;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.VisualPosition;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.colors.EditorColorsScheme;
import com.intellij.openapi.editor.impl.DocumentImpl;
import com.intellij.openapi.editor.impl.EditorImpl;
import com.intellij.openapi.editor.impl.FoldingModelImpl;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.ComponentPopupBuilder;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.awt.RelativePoint;
import com.intellij.ui.popup.AbstractPopup;

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

    private Font font;
    private AceCanvas aceCanvas;
    private EditorColorsScheme scheme;
    private boolean mnemonicsDisabled;

    private boolean searchMode = true;


    protected HashMap<String, Integer> offsetHash = new HashMap<String, Integer>();
    private AceFinder aceFinder;

    public void actionPerformed(AnActionEvent e) {
        inputEvent = e;

        project = e.getData(PlatformDataKeys.PROJECT);
        editor = (EditorImpl) e.getData(PlatformDataKeys.EDITOR);
        virtualFile = e.getData(PlatformDataKeys.VIRTUAL_FILE);
        document = (DocumentImpl) editor.getDocument();
        foldingModel = (FoldingModelImpl) editor.getFoldingModel();
        dataContext = e.getDataContext();
        caretModel = editor.getCaretModel();

        aceFinder = new AceFinder(project, document, editor, virtualFile);

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
        final UISettings settings = UISettings.getInstance();

        searchBox.addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent e) {
                createAceCanvas(settings);

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


        searchBox.requestFocus();
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

    private void createAceCanvas(UISettings settings) {
        JComponent contentComponent = editor.getContentComponent();
        aceCanvas = new AceCanvas();

        contentComponent.add(aceCanvas);
        JViewport viewport = editor.getScrollPane().getViewport();
        //the 1000s are for the panels on the sides, hopefully user testing will find any holes
        aceCanvas.setBounds(0, 0, viewport.getWidth() + 1000, viewport.getHeight() + 1000);
//                    aceCanvas.setBounds(0, 0, viewport.getWidth(), viewport.getHeight());
        //System.out.println(aceCanvas.getWidth());

        JRootPane rootPane = editor.getComponent().getRootPane();
        Point locationOnScreen = SwingUtilities.convertPoint(aceCanvas, aceCanvas.getLocation(), rootPane);
        aceCanvas.setLocation(-locationOnScreen.x, -locationOnScreen.y);

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


    private void showBalloons(List<Integer> results, int start, int end) {
        offsetHash.clear();
        int size = results.size();
        if (end > size) {
            end = size;
        }

        Vector<Pair<String, Point>> ballonInfos = new Vector<Pair<String, Point>>();
        //todo: move all font-based positioning logic into the canvas
        float hOffset = font.getSize() - (font.getSize() * scheme.getLineSpacing());
        for (int i = start; i < end; i++) {

            int textOffset = results.get(i);
            RelativePoint point = getPointFromVisualPosition(editor, editor.offsetToVisualPosition(textOffset));
            Point originalPoint = point.getOriginalPoint();
            originalPoint.translate(0, (int) -hOffset);
            char resultChar = aceFinder.getAllowedCharacters().charAt(i % aceFinder.getAllowedCharacters().length());
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


    protected class SearchBox extends JTextField {
        protected int key;

        @Override
        protected void paintBorder(Graphics g) {
            //do nothing
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(getFontMetrics(getFont()).stringWidth("w"), editor.getLineHeight());
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
                        findText("^.|\\n(?<!.\\n)", true);
                        showBalloons(aceFinder.getResults(), aceFinder.getStartResult(), aceFinder.getEndResult());
                        //the textfield needs to have a char to read/delete for consistent behavior
                        setText(" ");
                        searchMode = false;
                        isSpecialChar = true;
                        break;
                    case KeyEvent.VK_END:
                        findText("\\n|\\Z", true);
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
                            aceFinder.expandResults();

                        } else {
                            aceFinder.contractResults();
                        }

                        aceFinder.checkForReset();

                        showBalloons(aceFinder.getResults(), aceFinder.getStartResult(), aceFinder.getEndResult());
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
                Integer offset = offsetHash.get(AceKeyUtil.getLowerCaseStringFromChar(keyChar));
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

    }

    private void findText(String textToFind, boolean isRegEx) {
        aceFinder.addObserver(new Observer() {
            @Override
            public void update(Observable o, Object arg) {
                showBalloons(aceFinder.getResults(), aceFinder.getStartResult(), aceFinder.getEndResult());
            }
        });

        aceFinder.findText(textToFind, isRegEx);
    }

}
