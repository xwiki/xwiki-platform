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
package org.xwiki.gwt.wysiwyg.client;

import org.xwiki.gwt.user.client.ui.LoadingPanel;
import org.xwiki.gwt.user.client.ui.ToolBar;
import org.xwiki.gwt.user.client.ui.rta.RichTextArea;

import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.MenuBar;

/**
 * The user interface of the WYSIWYG editor.
 * 
 * @version $Id$
 */
public class RichTextEditor extends Composite
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
     * Flag indicating if the rich text editor is in loading state. We need this flag to keep the loading state while
     * the rich text editor is detached from the document.
     */
    private boolean loading;

    /**
     * Creates a new rich text editor.
     */
    public RichTextEditor()
    {
        textArea = new RichTextArea();

        loadingPanel = new LoadingPanel();
        // NOTE: Setting and then removing visibility:hidden on the rich text area prevents it from being focused.
        loadingPanel.getElement().getStyle().setBackgroundColor("white");

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
            menu.setFocusOnHoverEnabled(false);
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
        if (this.loading != loading) {
            this.loading = loading;
            if (isAttached()) {
                if (loading) {
                    loadingPanel.startLoading(textArea);
                } else {
                    loadingPanel.stopLoading();
                }
            }
        }
    }

    /**
     * @return {@code true} if this rich text editor is currently in loading state, {@code false} otherwise
     */
    public boolean isLoading()
    {
        return loading;
    }

    /**
     * {@inheritDoc}
     * 
     * @see Composite#onLoad()
     */
    @Override
    protected void onLoad()
    {
        // Synchronize the loading panel with the loading state.
        if (loading != loadingPanel.isLoading()) {
            loading = loadingPanel.isLoading();
            setLoading(!loading);
        }
    }
}
