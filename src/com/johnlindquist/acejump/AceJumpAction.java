package com.johnlindquist.acejump;

import com.google.common.collect.Lists;
import com.intellij.ide.ui.UISettings;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.colors.EditorColorsScheme;
import com.intellij.openapi.editor.impl.DocumentImpl;
import com.intellij.openapi.editor.impl.EditorImpl;
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
    protected SearchBox searchBox;
    protected AnActionEvent actionEvent;

    private Font font;
    private AceCanvas aceCanvas;
    private EditorColorsScheme scheme;
    private boolean mnemonicsDisabled;

    private AceFinder aceFinder;
    private AceJumper aceJumper;

    public HashMap<String, Integer> textAndOffsetHash = new HashMap<String, Integer>();


    public void actionPerformed(AnActionEvent actionEvent) {
        this.actionEvent = actionEvent;

        project = actionEvent.getData(PlatformDataKeys.PROJECT);
        editor = (EditorImpl) actionEvent.getData(PlatformDataKeys.EDITOR);
        virtualFile = actionEvent.getData(PlatformDataKeys.VIRTUAL_FILE);
        document = (DocumentImpl) editor.getDocument();

        scheme = EditorColorsManager.getInstance().getGlobalScheme();
        font = new Font(scheme.getEditorFontName(), Font.BOLD, scheme.getEditorFontSize());

        aceFinder = new AceFinder(project, document, editor, virtualFile);
        aceJumper = new AceJumper(editor, document);

        aceCanvas = new AceCanvas();
        configureAceCanvas();

        searchBox = new SearchBox();
        configureSearchBox();
    }

    protected void configureSearchBox() {

        setupSearchBoxKeys();

        searchBox.setFont(font);

        ComponentPopupBuilder popupBuilder = JBPopupFactory.getInstance().createComponentPopupBuilder(searchBox, searchBox);
        popupBuilder.setCancelKeyEnabled(true);

        popup = (AbstractPopup) popupBuilder.createPopup();
        popup.show(AceUtil.guessBestLocation(editor));

        Dimension dimension = new Dimension(searchBox.getFontMetrics(font).stringWidth("w") * 2, editor.getLineHeight());
        popup.setSize(dimension);
        searchBox.setPopupContainer(popup);
        searchBox.setSize(dimension);
        searchBox.setFocusable(true);
        final UISettings settings = UISettings.getInstance();

        searchBox.addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent e) {
                addAceCanvas();

                /*
                    this is the worst hack to allow you to hit "alt+*" to select the word. Any better suggestions would
                    be much appreciated.
                 */

                mnemonicsDisabled = settings.DISABLE_MNEMONICS;
                if (!mnemonicsDisabled) {
                    settings.DISABLE_MNEMONICS = true;
                    settings.fireUISettingsChanged();
                }
            }

            @Override
            public void focusLost(FocusEvent e) {
                exit();
                if (!mnemonicsDisabled) {
                    settings.DISABLE_MNEMONICS = false;
                    settings.fireUISettingsChanged();
                }
            }
        });


        searchBox.requestFocus();
    }

    protected void configureAceCanvas() {
        aceCanvas.setFont(font);
        aceCanvas.setLineHeight(editor.getLineHeight());
        aceCanvas.setLineSpacing(scheme.getLineSpacing());
        aceCanvas.setBackgroundForegroundColors(new Pair<Color, Color>(scheme.getDefaultBackground(), scheme.getDefaultForeground()));

    }

    protected void setupSearchBoxKeys() {
        Observer showJumpObserver = new Observer() {
            @Override
            public void update(Observable o, Object arg) {
                setupJumpLocations(aceFinder.getResults(), aceFinder.getStartResult(), aceFinder.getEndResult());
            }
        };

        AceKeyCommand releasedHome = new ReleasedHome(searchBox, aceFinder);
        AceKeyCommand releasedEnd = new ReleasedEnd(searchBox, aceFinder);

        releasedHome.addObserver(showJumpObserver);
        releasedEnd.addObserver(showJumpObserver);

        //todo: consider other "special" searches and loading these from a config. Tab, Insert, Delete, Page Up, etc?
        searchBox.addPreProcessReleaseKey(KeyEvent.VK_HOME, releasedHome);
        searchBox.addPreProcessReleaseKey(KeyEvent.VK_END, releasedEnd);


        AceKeyCommand pressedBackspace = new PressedBackspace(aceCanvas);
        AceKeyCommand pressedEnter = new PressedEnter(searchBox, aceFinder, aceJumper);

        pressedEnter.addObserver(showJumpObserver);

        searchBox.addPreProcessPressedKey(KeyEvent.VK_BACK_SPACE, pressedBackspace);
        searchBox.addPreProcessPressedKey(KeyEvent.VK_ENTER, pressedEnter);



        DefaultKeyCommand defaultKeyCommand = new DefaultKeyCommand(searchBox, aceFinder, aceJumper, textAndOffsetHash);
        defaultKeyCommand.addObserver(showJumpObserver);

        searchBox.setDefaultKeyCommand(defaultKeyCommand);
    }

    protected void addAceCanvas() {
        JComponent contentComponent = editor.getContentComponent();

        contentComponent.add(aceCanvas);
        JViewport viewport = editor.getScrollPane().getViewport();
        //the 1000s are for the panels on the sides, hopefully user testing will find any holes
        aceCanvas.setBounds(0, 0, viewport.getWidth() + 1000, viewport.getHeight() + 1000);

        JRootPane rootPane = editor.getComponent().getRootPane();
        Point locationOnScreen = SwingUtilities.convertPoint(aceCanvas, aceCanvas.getLocation(), rootPane);
        aceCanvas.setLocation(-locationOnScreen.x, -locationOnScreen.y);
    }

    protected void setupJumpLocations(List<Integer> results, int start, int end) {
        textAndOffsetHash.clear();
        int size = results.size();
        if (end > size) {
            end = size;
        }

        Vector<Pair<String, Point>> textPointPairs = new Vector<Pair<String, Point>>();
        for (int i = start; i < end; i++) {

            int textOffset = results.get(i);
            RelativePoint point = AceUtil.getPointFromVisualPosition(editor, editor.offsetToVisualPosition(textOffset));
            char resultChar = aceFinder.getAllowedCharacters().charAt(i % aceFinder.getAllowedCharacters().length());
            final String text = String.valueOf(resultChar);

            textPointPairs.add(new Pair<String, Point>(text, point.getOriginalPoint()));
            textAndOffsetHash.put(text, textOffset);
        }


        showJumpers(textPointPairs);
    }


    protected void showJumpers(Vector<Pair<String, Point>> textPointPairs) {
        aceCanvas.setJumpInfos(Lists.reverse(textPointPairs));
        aceCanvas.repaint();
    }

    protected void exit() {
        project = null;
        aceCanvas.clear();
        aceCanvas.getParent().remove(aceCanvas);
        aceCanvas = null;
        textAndOffsetHash.clear();
    }
}
