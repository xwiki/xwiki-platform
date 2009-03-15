/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package com.xpn.xwiki.wysiwyg.client.editor;

import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DeferredCommand;
import com.google.gwt.user.client.ui.ChangeListener;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.FocusListener;
import com.google.gwt.user.client.ui.Hidden;
import com.google.gwt.user.client.ui.LoadListener;
import com.google.gwt.user.client.ui.LoadListenerCollection;
import com.google.gwt.user.client.ui.SourcesLoadEvents;
import com.google.gwt.user.client.ui.Widget;
import com.xpn.xwiki.wysiwyg.client.util.Console;
import com.xpn.xwiki.wysiwyg.client.widget.HiddenConfig;
import com.xpn.xwiki.wysiwyg.client.widget.MenuBar;
import com.xpn.xwiki.wysiwyg.client.widget.ToolBar;
import com.xpn.xwiki.wysiwyg.client.widget.rta.RichTextArea;

/**
 * The user interface of the WYSIWYG editor.
 * 
 * @version $Id$
 */
public class RichTextEditor extends Composite implements SourcesLoadEvents, FocusListener, ChangeListener, LoadListener
{
    /**
     * The menu bar.
     */
    protected MenuBar menu;

    /**
     * The tool bar.
     */
    protected ToolBar toolbar;

    /**
     * The rich text area.
     */
    protected final RichTextArea textArea;

    /**
     * Additional data to be sent to the server.
     */
    protected final HiddenConfig config;

    /**
     * Input hidden that stores the content of the rich text area. This content will be sent to the server when the form
     * in which this editor is placed will be submitted.
     */
    protected final Hidden content;

    /**
     * The list of listeners that are notified when the UI is loaded.
     */
    private final LoadListenerCollection loadListeners = new LoadListenerCollection();

    /**
     * Creates a new rich text editor.
     */
    public RichTextEditor()
    {
        textArea = new RichTextArea();
        textArea.addFocusListener(this);
        textArea.addChangeListener(this);
        // Workaround till GWT provides a way to detect when the rich text area has finished loading.
        if (textArea.getBasicFormatter() != null && textArea.getBasicFormatter() instanceof SourcesLoadEvents) {
            ((SourcesLoadEvents) textArea.getBasicFormatter()).addLoadListener(this);
        }

        config = new HiddenConfig();

        content = new Hidden();
        content.setDefaultValue("");

        FlowPanel container = new FlowPanel();
        container.add(textArea);
        container.add(content);
        container.add(config);
        container.addStyleName("xRichTextEditor");
        initWidget(container);
    }

    /**
     * @return the menu bar of this editor.
     */
    public MenuBar getMenu()
    {
        if (menu == null) {
            menu = new MenuBar();
            ((FlowPanel) textArea.getParent()).insert(menu, 0);
        }
        return menu;
    }

    /**
     * @return the tool bar of this editor.
     */
    public ToolBar getToolbar()
    {
        if (toolbar == null) {
            toolbar = new ToolBar();
            ((FlowPanel) textArea.getParent()).insert(toolbar, menu == null ? 0 : 1);
        }
        return toolbar;
    }

    /**
     * @return the text area of this editor.
     */
    public RichTextArea getTextArea()
    {
        return textArea;
    }

    /**
     * @return the group of hidden inputs that can be used to sent additional data to the server when the HTML form in
     *         which this editor is placed is submitted.
     */
    public HiddenConfig getConfig()
    {
        return config;
    }

    /**
     * {@inheritDoc}
     * 
     * @see Composite#onLoad()
     */
    protected void onLoad()
    {
        if (textArea.getBasicFormatter() == null || !(textArea.getBasicFormatter() instanceof SourcesLoadEvents)) {
            // We defer the notification in order to allow the rich text area to complete its initialization.
            DeferredCommand.addCommand(new Command()
            {
                public void execute()
                {
                    try {
                        loadListeners.fireLoad(RichTextEditor.this);
                    } catch (Throwable t) {
                        Console.getInstance().error(t, RichTextEditor.class.getName(),
                            LoadListenerCollection.class.getName());
                    }
                }
            });
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see LoadListener#onLoad(Widget)
     */
    public void onLoad(Widget sender)
    {
        loadListeners.fireLoad(this);
    }

    /**
     * {@inheritDoc}
     * 
     * @see LoadListener#onError(Widget)
     */
    public void onError(Widget sender)
    {
        loadListeners.fireError(this);
    }

    /**
     * {@inheritDoc}
     * 
     * @see SourcesLoadEvents#addLoadListener(LoadListener)
     */
    public void addLoadListener(LoadListener listener)
    {
        loadListeners.add(listener);
    }

    /**
     * {@inheritDoc}
     * 
     * @see SourcesLoadEvents#removeLoadListener(LoadListener)
     */
    public void removeLoadListener(LoadListener listener)
    {
        loadListeners.remove(listener);
    }

    /**
     * {@inheritDoc}
     * 
     * @see ChangeListener#onChange(Widget)
     */
    public void onChange(Widget sender)
    {
        if (sender == textArea) {
            content.setValue(textArea.getHTML());
            if (textArea.getName() != null) {
                content.setName(textArea.getName());
                content.setID(textArea.getName());
            }
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see FocusListener#onFocus(Widget)
     */
    public void onFocus(Widget sender)
    {
        // ignore
    }

    /**
     * {@inheritDoc}
     * 
     * @see FocusListener#onLostFocus(Widget)
     */
    public void onLostFocus(Widget sender)
    {
        if (sender == textArea) {
            content.setValue(textArea.getHTML());
        }
    }
}
