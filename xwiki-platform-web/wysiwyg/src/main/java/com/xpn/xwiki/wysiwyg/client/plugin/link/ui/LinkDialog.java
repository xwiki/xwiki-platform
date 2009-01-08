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

import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DeferredCommand;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.SourcesTabEvents;
import com.google.gwt.user.client.ui.TabListener;
import com.google.gwt.user.client.ui.TabPanel;
import com.google.gwt.user.client.ui.Widget;
import com.xpn.xwiki.wysiwyg.client.editor.Strings;
import com.xpn.xwiki.wysiwyg.client.widget.CompositeDialogBox;

/**
 * Extends {@link com.xpn.xwiki.wysiwyg.client.widget.CompositeDialogBox}.
 * 
 * @version $Id$
 */
public class LinkDialog extends CompositeDialogBox implements ClickListener, TabListener
{
    /**
     * Helper command to set the selection in this dialog to the stored selected.
     */
    private class SelectCommand implements Command
    {
        /**
         * {@inheritDoc}
         */
        public void execute()
        {
            tabs.selectTab(selectedTabIndex);
        }
    }

    /**
     * Tabs to add to this dialog.
     */
    private final TabPanel tabs;

    /**
     * This variable contains the index of the selected tab.
     */
    private int selectedTabIndex;

    /**
     * True if the dialog is close by the close button, and false if the dialog is close by <i>Create link</i> button.
     */
    private boolean closeByCreateLink;

    /**
     * Builds the dialog to take data from the user for the creation of a link.
     * 
     * @param currentWiki The name of current wiki.
     * @param currentSpace The name of current space.
     * @param currentPage The name of current page.
     */
    public LinkDialog(final String currentWiki, String currentSpace, String currentPage)
    {
        super(false, true);
        LinkToWebPageTab linkToWebPageTab = new LinkToWebPageTab();
        LinkToEmailAddressTab linkToEmailAddressTab = new LinkToEmailAddressTab();
        LinkToNewPageTab linkToNewPageTab = new LinkToNewPageTab(currentWiki, currentSpace, currentPage);
        LinkToExistingPageTab linkToExistingPageTab = new LinkToExistingPageTab(currentWiki, currentSpace, currentPage);

        tabs = new TabPanel();
        tabs.addTabListener(this);

        addTab(linkToExistingPageTab, Strings.INSTANCE.linkExistingPageTab());        
        addTab(linkToNewPageTab, Strings.INSTANCE.linkNewPageTab());
        addTab(linkToWebPageTab, Strings.INSTANCE.linkWebPageTab());
        addTab(linkToEmailAddressTab, Strings.INSTANCE.linkEmailTab());        

        tabs.selectTab(0);
        selectedTabIndex = 0;

        getDialog().setText(Strings.INSTANCE.link());
        getDialog().setAnimationEnabled(false);
        getDialog().addStyleName("linkDialog");

        initWidget(tabs);

        // set the initial label HTML to void string, as if no selection was made.
        setLabel("", "", false);
    }

    /**
     * Sets the label for the link created by this dialog. This label has an HTML form and a text form, the stripped
     * version of the HTML version. If the text form is edited by the user, that label will be used, otherwise the HTML
     * version, with formatting.
     * 
     * @param labelHTML the HTML of the label for the created link.
     * @param labelText the text of the label for the created link (stripped of HTML tags)
     * @param readOnly specifies if the link label will be exposed as readonly to the user
     */
    public void setLabel(String labelHTML, String labelText, boolean readOnly)
    {
        // pass the label to all the tabs in this dialog's tab panel
        for (int i = 0; i < tabs.getWidgetCount(); i++) {
            Widget tab = tabs.getWidget(i);
            if (tab instanceof HasLink) {
                ((HasLink) tab).setLabel(labelHTML, labelText, readOnly);
            }
        }
    }

    /**
     * @param content Widget that would be add in tab.
     * @param tabName The name of the tab.
     */
    private void addTab(Widget content, String tabName)
    {
        if (content instanceof HasLink) {
            tabs.add(content, tabName);
            ((HasLink) content).addClickListener(this);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see ClickListener#onClick(Widget)
     */
    public void onClick(Widget sender)
    {
        if (sender instanceof HasLink) {
            closeByCreateLink = true;
            hide();
        }
    }

    /**
     * @return the url from the selected tab.
     */
    public String getLink()
    {
        if (closeByCreateLink) {
            closeByCreateLink = false;
            return getSelectedTab().getLink();
        } else {
            return null;
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see TabListener#onBeforeTabSelected(SourcesTabEvents, int)
     */
    public boolean onBeforeTabSelected(SourcesTabEvents sender, int tabIndex)
    {
        return true;
    }

    /**
     * {@inheritDoc}
     * 
     * @see TabListener#onTabSelected(SourcesTabEvents, int)
     */
    public void onTabSelected(SourcesTabEvents sender, int tabIndex)
    {
        if (sender == tabs) {
            selectedTabIndex = tabIndex;
            getSelectedTab().initialize();
        }
    }

    /**
     * Gets the widget from the tab corresponding with the selected index.
     * 
     * @return the widget from the current tab.
     */
    private HasLink getSelectedTab()
    {
        return (HasLink) tabs.getWidget(selectedTabIndex);
    }

    /**
     * {@inheritDoc}
     * 
     * @see CompositeDialog#center()
     */
    public void center()
    {
        super.center();
        DeferredCommand.addCommand(new SelectCommand());
    }
}
