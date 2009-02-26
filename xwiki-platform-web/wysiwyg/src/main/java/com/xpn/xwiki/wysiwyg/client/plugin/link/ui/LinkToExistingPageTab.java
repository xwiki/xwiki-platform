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
import com.google.gwt.user.client.ui.ChangeListener;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.Widget;
import com.xpn.xwiki.wysiwyg.client.editor.Strings;
import com.xpn.xwiki.wysiwyg.client.plugin.link.LinkConfig;
import com.xpn.xwiki.wysiwyg.client.plugin.link.LinkHTMLGenerator;
import com.xpn.xwiki.wysiwyg.client.plugin.link.LinkConfig.LinkType;
import com.xpn.xwiki.wysiwyg.client.widget.PageSelector;

/**
 * Tab to get the information from the user to create a link towards an existing page.
 * 
 * @version $Id$
 */
public class LinkToExistingPageTab extends AbstractWikiPageLinkTab implements ChangeListener, ClickListener
{
    /**
     * List box with all the pages in the currently selected wiki and in the currently selected space.
     */
    private PageSelector pageSelector;

    /**
     * Button to create a link to a wiki.
     */
    private final Button linkToWikiButton;

    /**
     * Button to create a link to a space.
     */
    private final Button linkToSpaceButton;

    /**
     * Button to create a link to a page.
     */
    private final Button linkToPageButton;

    /**
     * Creates a new tab from the default wiki, space and page names.
     * 
     * @param defaultWiki wiki default name.
     * @param defaultSpace default space
     * @param defaultPage default page
     */
    public LinkToExistingPageTab(String defaultWiki, String defaultSpace, String defaultPage)
    {
        linkToWikiButton = new Button(Strings.INSTANCE.linkCreateLinkButon());
        linkToWikiButton.addClickListener(this);
        linkToWikiButton.setTitle(Strings.INSTANCE.linkToWikiButtonTooltip());
        linkToSpaceButton = new Button(Strings.INSTANCE.linkCreateLinkButon());
        linkToSpaceButton.addClickListener(this);
        linkToSpaceButton.setTitle(Strings.INSTANCE.linkToSpaceButtonTooltip());
        linkToPageButton = new Button(Strings.INSTANCE.linkCreateLinkButon());
        linkToPageButton.addClickListener(this);
        linkToPageButton.setTitle(Strings.INSTANCE.linkToExistingPageButtonTooltip());

        initWidget(buildMainPanel(defaultWiki, defaultSpace, defaultPage));
        addStyleName("xLinkToExistingPage");
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractWikiPageLinkTab#buildWikiPanel(String)
     */
    protected Panel buildWikiPanel(String currentWiki)
    {
        // get the panel built by the superclass
        Panel wikiPanel = super.buildWikiPanel(currentWiki);
        // add custom listeners and buttons
        getWikiSelector().addChangeListener(this);
        getWikiSelector().addKeyboardListener(new EnterListener(linkToWikiButton));
        wikiPanel.add(linkToWikiButton);
        return wikiPanel;
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractWikiPageLinkTab#buildSpacePanel(String, String)
     */
    protected Panel buildSpacePanel(String selectedWiki, String currentSpace)
    {
        // get the panel built by the superclass
        Panel spacePanel = super.buildSpacePanel(selectedWiki, currentSpace);
        // add custom buttons and listeners
        getSpaceSelector().addChangeListener(this);
        // change default tooltip for the spaces selector
        getSpaceSelector().setTitle(Strings.INSTANCE.linkExistingSpacesListBoxTooltip());
        getSpaceSelector().addKeyboardListener(new EnterListener(linkToSpaceButton));
        spacePanel.add(linkToSpaceButton);

        return spacePanel;
    }

    /**
     * @param selectedWiki the currently selected wiki
     * @param selectedSpace the currently selected space
     * @param currentPage the currently selected page
     * @return the panel with information about page.
     */
    private Panel buildPagePanel(String selectedWiki, String selectedSpace, String currentPage)
    {
        Panel pagePanel = new FlowPanel();
        Label choosePageLabel = new Label(Strings.INSTANCE.choosePage());
        pagePanel.add(choosePageLabel);
        pageSelector = new PageSelector(selectedWiki, selectedSpace);
        pageSelector.setTitle(Strings.INSTANCE.linkPageSelectorTooltip());
        pageSelector.addKeyboardListener(new EnterListener(linkToPageButton));
        pagePanel.add(pageSelector);
        populatePageSelector(selectedWiki, selectedSpace, currentPage);
        pagePanel.add(linkToPageButton);

        return pagePanel;
    }

    /**
     * @param currentWiki the current wiki
     * @param currentSpace the space of the current document
     * @param currentPage the page name of the document for which we instantiate the editor.
     * @return the main panel of this tab.
     */
    private Panel buildMainPanel(String currentWiki, String currentSpace, String currentPage)
    {
        Panel mainPanel = new FlowPanel();
        mainPanel.add(buildLabelPanel());
        mainPanel.add(buildWikiPanel(currentWiki));
        mainPanel.add(buildSpacePanel(currentWiki, currentSpace));
        mainPanel.add(buildPagePanel(currentWiki, currentSpace, currentPage));

        return mainPanel;
    }

    /**
     * Make a request to the server for all the pages from a space, and populate the list box. The list box will
     * preserve page selection, if the page name still remains in the newly fetched list.
     * 
     * @param selectedWiki the name of the currently selected wiki
     * @param selectedSpace The name of the currently selected space
     * @param currentPage The name of the currently selected page, to restore selection.
     */
    public void populatePageSelector(String selectedWiki, String selectedSpace, final String currentPage)
    {
        pageSelector.setWiki(selectedWiki);
        pageSelector.setSpace(selectedSpace);
        pageSelector.refreshList(currentPage);
    }

    /**
     * Set the value for <i>url</i> field and notify the listeners.
     * 
     * @param link The url received from server.
     */
    private void handleLinkGeneratorResponse(String link)
    {
        // Set the data of this tab
        setLink(link);
        // Notify listeners
        getClickListeners().fireClick(this);
    }

    /**
     * {@inheritDoc}
     * 
     * @see ClickListener#onClick(Widget)
     */
    public void onClick(Widget sender)
    {
        String wikiName;
        String spaceName;
        String pageName;

        // validate the user input
        if (validateUserInput()) {
            // Get the wiki name
            if (!isMultiWiki()) {
                wikiName = null;
            } else {
                wikiName = getWikiSelector().getSelectedWiki();
            }
            // Get the space name and the page name
            spaceName = getSpaceSelector().getSelectedSpace();
            pageName = pageSelector.getSelectedPage();

            // If the senders are the wiki or space buttons, invalidate page (and space) choices.
            if (sender == linkToWikiButton) {
                spaceName = null;
                pageName = null;
            } else if (sender == linkToSpaceButton) {
                pageName = null;
            }

            // Create the link
            LinkHTMLGenerator.getInstance().getExistingPageLink(getLinkLabel(), wikiName, spaceName, pageName, null,
                null, new AsyncCallback<String>()
                {
                    public void onFailure(Throwable caught)
                    {
                        throw new RuntimeException(caught.getMessage());
                    }

                    public void onSuccess(String result)
                    {
                        handleLinkGeneratorResponse(result);
                    }
                });
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see ChangeListener#onChange(Widget)
     */
    public void onChange(Widget sender)
    {
        if (sender == getWikiSelector()) {
            // Wiki selection changed, update accordingly spaces list, which will trigger pages list update.
            final String selectedWiki = getWikiSelector().getSelectedWiki();
            final String selectedSpace = getSpaceSelector().getSelectedSpace();
            populateSpaceSelector(selectedWiki, selectedSpace, new AsyncCallback<List<String>>()
            {
                public void onSuccess(List<String> result)
                {
                    // populate the pages list box according to the newly selected space, after spaces list update
                    populatePageSelector(selectedWiki, selectedSpace, pageSelector.getSelectedPage());
                }

                public void onFailure(Throwable caught)
                {
                }
            });
        } else if (sender == getSpaceSelector()) {
            // Space selection changed, update page selection according.
            String selectedSpace = getSpaceSelector().getSelectedSpace();
            populatePageSelector(null, selectedSpace, pageSelector.getSelectedPage());

        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see HasLink#validateUserInput()
     */
    public boolean validateUserInput()
    {
        // check the super class validation
        if (!super.validateUserInput()) {
            return false;
        }
        // No need to check anything in the current implementation because everything is a combobox, so user input is
        // "safe"
        return true;
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractWikiPageLinkTab#getLabelTextBoxTooltip()
     */
    protected String getLabelTextBoxTooltip()
    {
        return Strings.INSTANCE.linkExistingPageLabelTextBoxTooltip();
    }
    
    /**
     * {@inheritDoc}
     * 
     * @see AbstractWikiPageLinkTab#updateSpaceSelector(LinkConfig)
     */
    protected void updateSpaceSelector(final LinkConfig config)
    {
        populateSpaceSelector(config.getWiki(), config.getSpace(), new AsyncCallback<List<String>>()
        {
            public void onSuccess(List<String> result)
            {
                populatePageSelector(config.getWiki(), config.getSpace(), config.getPage());
            }

            public void onFailure(Throwable caught)
            {
            }
        });
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractWikiPageLinkTab#getLinkType()
     */
    public LinkType getLinkType()
    {
        return LinkType.EXISTING_PAGE;
    }
}
