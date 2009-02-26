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

import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ChangeListener;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.FocusListener;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;
import com.xpn.xwiki.wysiwyg.client.editor.Strings;
import com.xpn.xwiki.wysiwyg.client.plugin.link.LinkConfig;
import com.xpn.xwiki.wysiwyg.client.plugin.link.LinkHTMLGenerator;
import com.xpn.xwiki.wysiwyg.client.plugin.link.LinkConfig.LinkType;

/**
 * Tab to get the information from the user to create a link towards a new wiki page.
 * 
 * @version $Id$
 */
public class LinkToNewPageTab extends AbstractWikiPageLinkTab implements ChangeListener, ClickListener, FocusListener
{
    /**
     * Panel to hold the form for creating a new space, to be shown if the user chooses to create a new space.
     */
    private Panel newSpacePanel;

    /**
     * The text box with the name of the new space.
     */
    private TextBox newSpaceNameTextBox;

    /**
     * The text box with the name of the new page.
     */
    private TextBox newPageNameTextBox;

    /**
     * The button to create the new link.
     */
    private final Button createLinkButton;

    /**
     * Creates a new tab from the default wiki, space and page names.
     * 
     * @param defaultWiki wiki default name.
     * @param defaultSpace default space
     * @param defaultPage default page
     */
    public LinkToNewPageTab(String defaultWiki, String defaultSpace, String defaultPage)
    {
        createLinkButton = new Button(Strings.INSTANCE.linkCreateLinkButon(), this);
        createLinkButton.setTitle(Strings.INSTANCE.linkToNewPageButtonTooltip());

        initWidget(buildMainPanel(defaultWiki, defaultSpace, defaultPage));
        addStyleName("xLinkToNewPage");
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
        newSpacePanel = buildNewSpacePanel();
        mainPanel.add(newSpacePanel);
        mainPanel.add(buildNewPagePanel());
        mainPanel.add(createLinkButton);

        return mainPanel;
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractWikiPageLinkTab#buildWikiPanel(String)
     */
    protected Panel buildWikiPanel(String currentWiki)
    {
        Panel wikiPanel = super.buildWikiPanel(currentWiki);
        getWikiSelector().addChangeListener(this);
        getWikiSelector().addKeyboardListener(new EnterListener(createLinkButton));

        return wikiPanel;
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractWikiPageLinkTab#buildSpacePanel(String, String)
     */
    protected Panel buildSpacePanel(String selectedWiki, String currentSpace)
    {
        Panel spacePanel = super.buildSpacePanel(selectedWiki, currentSpace);
        getSpaceSelector().addChangeListener(this);
        getSpaceSelector().addKeyboardListener(new EnterListener(createLinkButton));
        return spacePanel;
    }

    /**
     * {@inheritDoc}
     */
    protected Panel buildLabelPanel()
    {
        Panel labelPanel = super.buildLabelPanel();
        // add keyboard listener to the label text box
        getLabelTextBox().addKeyboardListener(new EnterListener(createLinkButton));

        return labelPanel;
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractWikiPageLinkTab#populateSpaceSelector(String, String, AsyncCallback)
     */
    protected void populateSpaceSelector(String selectedWiki, String currentSpace, final AsyncCallback<List<String>> cb)
    {
        super.populateSpaceSelector(selectedWiki, currentSpace, new AsyncCallback<List<String>>()
        {
            public void onFailure(Throwable caught)
            {
                if (cb != null) {
                    cb.onFailure(caught);
                }
            };

            public void onSuccess(List<String> result)
            {
                addNewSpaceOption();
                if (cb != null) {
                    cb.onSuccess(result);
                }
            }
        });
    }

    /**
     * Adds the new space option in the space drop down. To be used when the space selector is updated to add the new
     * option.
     */
    private void addNewSpaceOption()
    {
        // Add the space creation option in the build list
        int selectedIndex = getSpaceSelector().getSelectedIndex();
        getSpaceSelector().insertItem(Strings.INSTANCE.linkCreateNewSpaceText(), 0);
        getSpaceSelector().setSelectedIndex(selectedIndex + 1);
    }

    /**
     * @return a panel with the text input for the space name.
     */
    private Panel buildNewSpacePanel()
    {
        Panel spaceInputPanel = new FlowPanel();
        spaceInputPanel.setVisible(false);
        Label newSpaceLabel = new Label(Strings.INSTANCE.linkNewSpaceLabel());
        spaceInputPanel.add(newSpaceLabel);
        newSpaceNameTextBox = new TextBox();
        newSpaceNameTextBox.setText(Strings.INSTANCE.linkNewSpaceTextBox());
        newSpaceNameTextBox.setTitle(Strings.INSTANCE.linkNewSpaceTextBoxTooltip());
        newSpaceNameTextBox.addFocusListener(this);
        spaceInputPanel.add(newSpaceNameTextBox);

        return spaceInputPanel;
    }

    /**
     * @return the panel with the controls to create a new page name.
     */
    private Panel buildNewPagePanel()
    {
        Panel newPagePanel = new FlowPanel();
        Label newPageLabel = new Label(Strings.INSTANCE.linkNewPageLabel());
        newPagePanel.add(newPageLabel);
        newPageNameTextBox = new TextBox();
        newPageNameTextBox.setTitle(Strings.INSTANCE.linkNewPageTextBoxTooltip());
        newPageNameTextBox.setText(Strings.INSTANCE.linkNewPageTextBox());
        newPageNameTextBox.addFocusListener(this);
        newPageNameTextBox.addKeyboardListener(new EnterListener(createLinkButton));
        newPagePanel.add(newPageNameTextBox);

        return newPagePanel;
    }

    /**
     * {@inheritDoc}
     * 
     * @see ClickListener#onClick(Widget)
     */
    public void onClick(Widget sender)
    {
        if (sender == createLinkButton) {
            if (validateUserInput()) {
                // get the space name
                String spaceName = getSpaceSelector().getSelectedSpace();
                if (getSpaceSelector().getSelectedIndex() == 0) {
                    spaceName = newSpaceNameTextBox.getText().trim();
                }

                String pageName = newPageNameTextBox.getText().trim();
                if (pageName.equals(Strings.INSTANCE.linkNewPageTextBox())) {
                    pageName = "";
                }
                String wikiName = isMultiWiki() ? getWikiSelector().getSelectedWiki() : null;
                LinkHTMLGenerator.getInstance().getNewPageLink(getLinkLabel(), wikiName, spaceName, pageName,
                    new AsyncCallback<String>()
                    {
                        public void onFailure(Throwable caught)
                        {
                            throw new RuntimeException(caught.getMessage());
                        }

                        public void onSuccess(String result)
                        {
                            setLink(result);
                            getClickListeners().fireClick(LinkToNewPageTab.this);
                        }
                    });
            }
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
            populateSpaceSelector(selectedWiki, selectedSpace, null);
        }
        if (sender == getSpaceSelector()
            && getSpaceSelector().getSelectedSpace().trim().equals(Strings.INSTANCE.linkCreateNewSpaceText().trim())) {
            newSpacePanel.setVisible(true);
        } else {
            newSpacePanel.setVisible(false);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see HasLink#initialize()
     */
    public void initialize()
    {
        if (newPageNameTextBox.getText().trim().length() == 0) {
            newPageNameTextBox.setText(Strings.INSTANCE.linkNewPageTextBox());
        }
        if (newSpaceNameTextBox.getText().trim().length() == 0) {
            newSpaceNameTextBox.setText(Strings.INSTANCE.linkNewSpaceTextBox());
            newSpacePanel.setVisible(false);
        }
        super.initialize();
    }

    /**
     * {@inheritDoc}
     * 
     * @see HasLink#validateUserInput()
     */
    public boolean validateUserInput()
    {
        if (!super.validateUserInput()) {
            return false;
        }
        if (newPageNameTextBox.getText().equals(Strings.INSTANCE.linkNewPageTextBox())
            || newPageNameTextBox.getText().trim().length() == 0) {
            // If the page name wasn't set, then the space must have been set
            if (getSpaceSelector().getSelectedSpace().trim().equals(Strings.INSTANCE.linkCreateNewSpaceText())
                && (newSpaceNameTextBox.getText().equals(Strings.INSTANCE.linkNewSpaceTextBox()) || newSpaceNameTextBox
                    .getText().trim().length() == 0)) {
                Window.alert(Strings.INSTANCE.linkNewSpaceError());
                return false;
            }
        }
        return true;
    }

    /**
     * {@inheritDoc}
     * 
     * @see FocusListener#onFocus(Widget)
     */
    public void onFocus(Widget sender)
    {
        if (sender == newSpaceNameTextBox
            && newSpaceNameTextBox.getText().trim().equals(Strings.INSTANCE.linkNewSpaceTextBox())) {
            newSpaceNameTextBox.selectAll();
        } else if (sender == newPageNameTextBox
            && newPageNameTextBox.getText().trim().equals(Strings.INSTANCE.linkNewPageTextBox())) {
            newPageNameTextBox.selectAll();
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see FocusListener#onLostFocus(Widget)
     */
    public void onLostFocus(Widget sender)
    {
        // ignore
    }

    
    /**
     * {@inheritDoc}
     * 
     * @see AbstractWikiPageLinkTab#getLabelTextBoxTooltip()
     */
    protected String getLabelTextBoxTooltip()
    {
        return Strings.INSTANCE.linkNewPageLabelTextBoxTooltip();
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
                // if the space isn't the requested space, it means the new space panel must be activated
                if (!getSpaceSelector().getSelectedSpace().equals(config.getSpace())) {
                    getSpaceSelector().setSelectedIndex(0);
                    newSpacePanel.setVisible(true);
                    newSpaceNameTextBox.setText(config.getSpace());
                } else {
                    newSpacePanel.setVisible(false);
                }
            }

            public void onFailure(Throwable caught)
            {
            }
        });
        newPageNameTextBox.setText(config.getPage());
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractHasLinkTab#getLinkType()
     */
    public LinkType getLinkType()
    {
        return LinkType.NEW_PAGE;
    }
}
