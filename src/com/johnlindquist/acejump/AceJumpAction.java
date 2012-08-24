package com.johnlindquist.acejump;

import com.google.common.collect.Lists;
import com.intellij.ide.ui.UISettings;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
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
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.awt.RelativePoint;
import com.intellij.ui.popup.AbstractPopup;
import com.johnlindquist.acejump.keycommands.*;
import com.johnlindquist.acejump.ui.AceCanvas;
import com.johnlindquist.acejump.ui.SearchBox;

import javax.swing.*;
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

    private AceFinder aceFinder;
    private AceJumper aceJumper;

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
        aceJumper = new AceJumper(editor, document);

        scheme = EditorColorsManager.getInstance().getGlobalScheme();

        font = new Font(scheme.getEditorFontName(), Font.BOLD, scheme.getEditorFontSize());
        searchBox = new SearchBox();

        setupSearchBoxKeys();

        searchBox.setFont(font);

        ComponentPopupBuilder popupBuilder = JBPopupFactory.getInstance().createComponentPopupBuilder(searchBox, searchBox);
        popupBuilder.setCancelKeyEnabled(true);

        popup = (AbstractPopup) popupBuilder.createPopup();


        popup.show(guessBestLocation(editor));

        Dimension dimension = new Dimension(searchBox.getFontMetrics(font).stringWidth("w") * 2, editor.getLineHeight());
        popup.setSize(dimension);
        searchBox.setPopupContainer(popup);
        searchBox.setSize(dimension);
        searchBox.setFocusable(true);
        final UISettings settings = UISettings.getInstance();

        searchBox.addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent e) {
                createAceCanvas();

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

    protected void clearSelection() {
        aceCanvas.setJumpInfos(null);
        aceCanvas.repaint();
        aceJumper.textAndOffsetHash.clear();
    }

    private void createAceCanvas() {
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


    private void showJumpLocations(List<Integer> results, int start, int end) {
        aceJumper.textAndOffsetHash.clear();
        int size = results.size();
        if (end > size) {
            end = size;
        }

        Vector<Pair<String, Point>> textPointPairs = new Vector<Pair<String, Point>>();
        for (int i = start; i < end; i++) {

            int textOffset = results.get(i);
            RelativePoint point = getPointFromVisualPosition(editor, editor.offsetToVisualPosition(textOffset));
            char resultChar = aceFinder.getAllowedCharacters().charAt(i % aceFinder.getAllowedCharacters().length());
            final String text = String.valueOf(resultChar);

            textPointPairs.add(new Pair<String, Point>(text, point.getOriginalPoint()));
            aceJumper.textAndOffsetHash.put(text, textOffset);
        }


        aceCanvas.setFont(font);
        aceCanvas.setLineHeight(editor.getLineHeight());
        aceCanvas.setLineSpacing(scheme.getLineSpacing());
        aceCanvas.setJumpInfos(Lists.reverse(textPointPairs));
        aceCanvas.setBackgroundForegroundColors(new Pair<Color, Color>(scheme.getDefaultBackground(), scheme.getDefaultForeground()));

        aceCanvas.repaint();
    }

    private void setupSearchBoxKeys() {
        Observer showJumpObserver = new Observer() {
            @Override
            public void update(Observable o, Object arg) {
                showJumpLocations(aceFinder.getResults(), aceFinder.getStartResult(), aceFinder.getEndResult());
            }
        };

        AceKeyCommand releasedHome = new ReleasedHome(searchBox, aceFinder);
        AceKeyCommand releasedEnd = new ReleasedEnd(searchBox, aceFinder);
        AceKeyCommand releasedSpace = new ReleasedSpace(searchBox, aceFinder);

        releasedHome.addObserver(showJumpObserver);
        releasedEnd.addObserver(showJumpObserver);

        searchBox.preProcessKeyReleasedMap.put(KeyEvent.VK_HOME, releasedHome);
        searchBox.preProcessKeyReleasedMap.put(KeyEvent.VK_END, releasedEnd);
        searchBox.preProcessKeyReleasedMap.put(KeyEvent.VK_SPACE, releasedSpace);


        AceKeyCommand pressedBackspace = new PressedBackspace(searchBox, aceFinder);
        AceKeyCommand pressedEnter = new PressedEnter(searchBox, aceFinder);

        pressedEnter.addObserver(showJumpObserver);

        searchBox.preProcessKeyPressedMap.put(KeyEvent.VK_BACK_SPACE, pressedBackspace);
        searchBox.preProcessKeyPressedMap.put(KeyEvent.VK_ENTER, pressedBackspace);


        DefaultKeyCommand defaultKeyCommand = new DefaultKeyCommand(searchBox, aceFinder, aceJumper);
        defaultKeyCommand.addObserver(showJumpObserver);

        searchBox.setDefaultKeyCommand(defaultKeyCommand);
    }


}
