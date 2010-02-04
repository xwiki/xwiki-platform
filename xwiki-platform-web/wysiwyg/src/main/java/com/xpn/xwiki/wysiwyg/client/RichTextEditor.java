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
package com.xpn.xwiki.wysiwyg.client;

import org.xwiki.gwt.dom.client.Style;
import org.xwiki.gwt.user.client.ui.LoadingPanel;
import org.xwiki.gwt.user.client.ui.MenuBar;
import org.xwiki.gwt.user.client.ui.ToolBar;
import org.xwiki.gwt.user.client.ui.rta.RichTextArea;

import com.google.gwt.event.dom.client.LoadEvent;
import com.google.gwt.event.dom.client.LoadHandler;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DeferredCommand;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;

/**
 * The user interface of the WYSIWYG editor.
 * 
 * @version $Id$
 */
public class RichTextEditor extends Composite implements LoadHandler
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
     * The panel used to indicate the loading state of the editor.
     */
    protected final LoadingPanel loadingPanel;

    /**
     * The UI container.
     */
    protected final FlowPanel container;

    /**
     * Creates a new rich text editor.
     */
    public RichTextEditor()
    {
        textArea = new RichTextArea();
        textArea.addLoadHandler(this);

        loadingPanel = new LoadingPanel();
        loadingPanel.getElement().getStyle().setProperty(Style.BACKGROUND_COLOR, "white");

        container = new FlowPanel();
        container.add(textArea);
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
     * @return the UI container
     */
    public FlowPanel getContainer()
    {
        return container;
    }

    /**
     * Set the editor loading state. While in loading state a spinner will be displayed.
     * 
     * @param loading true to display the editor in loading mode, false to remove the loading mode.
     */
    public void setLoading(boolean loading)
    {
        if (loading) {
            // NOTE: Setting and then removing visibility:hidden on the rich text area prevents it from being focused.
            loadingPanel.startLoading(textArea);
        } else {
            loadingPanel.stopLoading();
        }
    }

    /**
     * @return {@code true} if this rich text editor is currently in loading state, {@code false} otherwise
     */
    public boolean isLoading()
    {
        return loadingPanel.isLoading();
    }

    /**
     * {@inheritDoc}
     * 
     * @see Composite#onLoad()
     */
    protected void onLoad()
    {
        // This rich text editor has been attached to the document but its rich text area might not be ready yet.
        setLoading(true);
    }

    /**
     * {@inheritDoc}
     * 
     * @see LoadHandler#onLoad(LoadEvent)
     */
    public void onLoad(LoadEvent event)
    {
        if (event.getSource() == textArea) {
            // Move out of the loading state after the rest of the listeners execute their code.
            DeferredCommand.addCommand(new Command()
            {
                public void execute()
                {
                    setLoading(false);
                }
            });
        }
    }
}
