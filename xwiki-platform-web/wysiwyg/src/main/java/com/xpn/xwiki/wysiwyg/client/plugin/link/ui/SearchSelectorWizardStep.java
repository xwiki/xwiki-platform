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

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyPressEvent;
import com.google.gwt.event.dom.client.KeyPressHandler;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TextBox;
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
public class SearchSelectorWizardStep extends AbstractPageListSelectorWizardStep implements ClickHandler,
    KeyPressHandler
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
        searchButton.addClickHandler(this);
        searchBox.addKeyPressHandler(this);
        searchPanel.add(searchBox);
        searchPanel.add(searchButton);
        getMainPanel().insert(searchPanel, getMainPanel().getWidgetIndex(getPagesList()));
    }

    /**
     * {@inheritDoc}
     * 
     * @see ClickHandler#onClick(ClickEvent)
     */
    public void onClick(ClickEvent event)
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
     * 
     * @see KeyPressHandler#onKeyPress(KeyPressEvent)
     */
    public void onKeyPress(KeyPressEvent event)
    {
        // if the key is enter in the search box, search
        if (event.getSource() == searchBox && event.getNativeEvent().getKeyCode() == KeyCodes.KEY_ENTER
            && !event.isAnyModifierKeyDown()) {
            // should send the correct sender
            onClick(null);
        }
    }
}
