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
package com.xpn.xwiki.wysiwyg.client.plugin.link.ui;

import java.util.List;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.KeyboardListener;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;
import com.xpn.xwiki.gwt.api.client.Document;
import com.xpn.xwiki.wysiwyg.client.WysiwygService;
import com.xpn.xwiki.wysiwyg.client.editor.Strings;
import com.xpn.xwiki.wysiwyg.client.util.ResourceName;
import com.xpn.xwiki.wysiwyg.client.widget.ListItem;

/**
 * Wizard step to select the wiki page to link to, from the page search results for a keyword.
 * 
 * @version $Id$
 */
public class SearchSelectorWizardStep extends AbstractPageListSelectorWizardStep implements ClickListener,
    KeyboardListener
{
    /**
     * Loading class for the time to load the step to which it has been toggled.
     */
    private static final String STYLE_LOADING = "loading";

    /**
     * Loading class for the time to load the step to which it has been toggled.
     */
    private static final String STYLE_ERROR = "errormessage";

    /**
     * The search keyword.
     */
    private String keyword;

    /**
     * The search text box.
     */
    private TextBox searchBox;

    /**
     * Builds a search selector wizard step for the passed edited resource.
     * 
     * @param editedResource the currently edited resource (page for which editing is done)
     */
    public SearchSelectorWizardStep(ResourceName editedResource)
    {
        super(editedResource);

        getMainPanel().addStyleName("xPagesSearch");
        // create the search widget
        FlowPanel searchPanel = new FlowPanel();
        searchPanel.addStyleName("xSearchForm");
        searchBox = new TextBox();
        searchBox.setTitle(Strings.INSTANCE.linkWikipageSearchTooltip());
        Button searchButton = new Button(Strings.INSTANCE.linkWikipageSearchButton());
        searchButton.addClickListener(this);
        searchBox.addKeyboardListener(this);
        searchPanel.add(searchBox);
        searchPanel.add(searchButton);
        getMainPanel().insert(searchPanel, getMainPanel().getWidgetIndex(getPagesList()));
    }

    /**
     * {@inheritDoc}
     */
    public void onClick(Widget sender)
    {
        // set the keyword from the search input
        keyword = searchBox.getText().trim();
        // set loading on the pages list
        getPagesList().clear();
        getPagesList().addStyleName(STYLE_LOADING);
        // refresh the results list
        refreshPagesList(new AsyncCallback<Object>()
        {
            public void onSuccess(Object result)
            {
                getPagesList().removeStyleName(STYLE_LOADING);
                getPagesList().setVisible(true);
            }

            public void onFailure(Throwable caught)
            {
                getPagesList().setVisible(true);
                getPagesList().removeStyleName(STYLE_LOADING);
                Label error = new Label(Strings.INSTANCE.linkErrorLoadingData());
                error.addStyleName(STYLE_ERROR);
                ListItem errorListItem = new ListItem();
                errorListItem.add(error);
                getPagesList().addItem(errorListItem);
            }
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void refreshPagesList(final AsyncCallback< ? > cb)
    {
        WysiwygService.Singleton.getInstance().getMatchingPages(getKeyword(), 0, 20,
            new AsyncCallback<List<Document>>()
            {
                public void onSuccess(List<Document> result)
                {
                    fillPagesList(result);
                    cb.onSuccess(null);
                }

                public void onFailure(Throwable caught)
                {
                    cb.onFailure(caught);
                }
            });
    }

    /**
     * @return the keyword
     */
    public String getKeyword()
    {
        return keyword == null ? "" : keyword;
    }

    /**
     * @param keyword the keyword to set
     */
    public void setKeyword(String keyword)
    {
        this.keyword = keyword;
    }

    /**
     * {@inheritDoc}
     */
    public void onKeyDown(Widget sender, char keyCode, int modifiers)
    {
        // nothing
    }

    /**
     * {@inheritDoc}
     */
    public void onKeyPress(Widget sender, char keyCode, int modifiers)
    {
        // if the key is enter in the searchbox, search
        if (sender == searchBox && keyCode == KEY_ENTER && modifiers == 0) {
            // should send the correct sender
            onClick(null);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void onKeyUp(Widget sender, char keyCode, int modifiers)
    {
        // nothing
    }
}
