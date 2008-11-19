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
import com.xpn.xwiki.wysiwyg.client.plugin.link.LinkGenerator;

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
        getWikiListBox().addChangeListener(this);
        getWikiListBox().addKeyboardListener(new EnterListener(createLinkButton));

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
        getSpaceListBox().addChangeListener(this);
        getSpaceListBox().addKeyboardListener(new EnterListener(createLinkButton));
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
     * @see AbstractWikiPageLinkTab#populateSpaceListBox(String, String, AsyncCallback)
     */
    protected void populateSpaceListBox(String selectedWiki, String currentSpace, final AsyncCallback<List<String>> cb)
    {
        super.populateSpaceListBox(selectedWiki, currentSpace, new AsyncCallback<List<String>>()
        {
            public void onFailure(Throwable caught)
            {
                if (cb != null) {
                    cb.onFailure(caught);
                }
            };

            public void onSuccess(List<String> result)
            {
                // Add the space creation option in the build list
                int selectedIndex = getSpaceListBox().getSelectedIndex();
                getSpaceListBox().insertItem(Strings.INSTANCE.linkCreateNewSpaceText(), 0);
                getSpaceListBox().setSelectedIndex(selectedIndex);
                if (cb != null) {
                    cb.onSuccess(result);
                }
            }
        });
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
                String spaceName = getSpaceListBox().getItemText(getSpaceListBox().getSelectedIndex());
                if (getSpaceListBox().getSelectedIndex() == 0) {
                    spaceName = newSpaceNameTextBox.getText().trim();
                }

                String pageName = newPageNameTextBox.getText().trim();
                if (pageName.equals(Strings.INSTANCE.linkNewPageTextBox())) {
                    pageName = "";
                }
                String wikiName =
                    isMultiWiki() ? getWikiListBox().getItemText(getWikiListBox().getSelectedIndex()) : null;
                LinkGenerator.getInstance().getNewPageLink(getLinkLabel(), wikiName, spaceName, pageName,
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
        if (sender == getWikiListBox()) {
            // Wiki selection changed, update accordingly spaces list, which will trigger pages list update.
            final String selectedWiki = getWikiListBox().getItemText(getWikiListBox().getSelectedIndex());
            final String selectedSpace = getSpaceListBox().getItemText(getSpaceListBox().getSelectedIndex());
            populateSpaceListBox(selectedWiki, selectedSpace, null);
        }
        if (sender == getSpaceListBox() && getSpaceListBox().getSelectedIndex() == 0) {
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
        newPageNameTextBox.setText(Strings.INSTANCE.linkNewPageTextBox());
        newSpaceNameTextBox.setText(Strings.INSTANCE.linkNewSpaceTextBox());
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
            if (getSpaceListBox().getSelectedIndex() == 0
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
            && newSpaceNameTextBox.getText().trim().equals(Strings.INSTANCE.linkNewPageTextBox())) {
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
}
