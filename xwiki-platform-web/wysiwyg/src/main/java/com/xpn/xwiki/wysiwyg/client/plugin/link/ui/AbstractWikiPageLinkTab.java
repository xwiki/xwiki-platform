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
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Panel;
import com.xpn.xwiki.wysiwyg.client.WysiwygService;
import com.xpn.xwiki.wysiwyg.client.editor.Strings;
import com.xpn.xwiki.wysiwyg.client.widget.SpaceSelector;
import com.xpn.xwiki.wysiwyg.client.widget.WikiSelector;

/**
 * Superclass for the tabs getting wiki pages links (the existing page and the new page). This class will store common
 * functions and data such as: the wiki select box or the spaces select box with their loading functions.
 * 
 * @version $Id$
 */
public abstract class AbstractWikiPageLinkTab extends AbstractHasLinkTab
{
    /**
     * List box containing virtual wiki names.
     */
    private WikiSelector wikiSelector;

    /**
     * List box containing all the spaces in the selected wiki.
     */
    private SpaceSelector spaceSelector;

    /**
     * Whether the current wiki is part of a multiwiki or not.
     */
    private boolean isMultiWiki = true;

    /**
     * @return the label panel to hold the wiki label input.
     */
    protected Panel buildLabelPanel()
    {
        Panel labelPanel = new FlowPanel();
        Label labelLabel = new Label(Strings.INSTANCE.linkLabelLabel());
        labelPanel.add(labelLabel);
        labelPanel.add(getLabelTextBox());

        return labelPanel;
    }

    /**
     * @param currentWiki the currently selected wiki, to restore selection.
     * @return the panel with information about wiki. Will get the information about the virtual wikis in this instance
     *         and set the wiki panel visible or not visible, setting the options in the same time.
     */
    protected Panel buildWikiPanel(final String currentWiki)
    {
        final Panel wikiPanel = new FlowPanel();
        wikiSelector = new WikiSelector();
        // Check if this wiki is a multi wiki, to print the wikiPanel or not
        WysiwygService.Singleton.getInstance().isMultiWiki(new AsyncCallback<Boolean>()
        {
            public void onFailure(Throwable caught)
            {
                throw new RuntimeException(caught.getMessage());
            }

            public void onSuccess(Boolean result)
            {
                if (!result) {
                    wikiPanel.setVisible(false);
                    isMultiWiki = false;
                } else {
                    populateWikiSelector(currentWiki);
                }

            }
        });
        Label chooseWikiLabel = new Label(Strings.INSTANCE.chooseWiki());
        wikiPanel.add(chooseWikiLabel);
        wikiPanel.add(wikiSelector);
        return wikiPanel;
    }

    /**
     * Get the wikis list and populate the wikis list box.
     * 
     * @param currentWiki the currently selected wiki, to restore selection.
     */
    protected void populateWikiSelector(final String currentWiki)
    {
        wikiSelector.refreshList(currentWiki);
    }

    /**
     * @param selectedWiki the currently selected wiki, for which to build the space panel.
     * @param currentSpace the currently selected space, to restore selection in the list box.
     * @return the panel with information about spaces. Will get the spaces on the server and populate the spaces list
     *         box.
     */
    protected Panel buildSpacePanel(String selectedWiki, String currentSpace)
    {
        Panel spacePanel = new FlowPanel();
        Label chooseSpaceLabel = new Label(Strings.INSTANCE.chooseSpace());
        spacePanel.add(chooseSpaceLabel);
        spaceSelector = new SpaceSelector(selectedWiki);
        populateSpaceSelector(selectedWiki, currentSpace, null);
        spacePanel.add(spaceSelector);

        return spacePanel;
    }

    /**
     * Make a request to the server for all the spaces from a wiki, and populate the list box. When is need, the current
     * space will be selected.
     * 
     * @param selectedWiki the currently selected wiki, for which to get the spaces
     * @param currentSpace the currently selected space, to preserve selection in the listbox.
     * @param cb callback to handle async call to caller level. Set this to null if there is no need for async call
     *            handle.
     */
    protected void populateSpaceSelector(String selectedWiki, final String currentSpace,
        final AsyncCallback<List<String>> cb)
    {
        spaceSelector.setWiki(selectedWiki);
        spaceSelector.refreshList(currentSpace, cb);
    }

    /**
     * {@inheritDoc}
     * 
     * @see HasLink#initialize()
     */
    public void initialize()
    {
        setLink(null);
        if (getLabelTextBox().getText().trim().length() == 0) {
            getLabelTextBox().setFocus(true);
        } else {
            if (isMultiWiki()) {
                getWikiSelector().setFocus(true);
            } else {
                getSpaceSelector().setFocus(true);
            }
        }
    }

    /**
     * @return the {@link WikiSelector} with the wiki names.
     */
    protected WikiSelector getWikiSelector()
    {
        return wikiSelector;
    }

    /**
     * @return the {@link SpaceSelector} with the space names in the selected wiki.
     */
    protected SpaceSelector getSpaceSelector()
    {
        return spaceSelector;
    }

    /**
     * @return true if the current is a virtual wiki, false otherwise.
     */
    protected boolean isMultiWiki()
    {
        return isMultiWiki;
    }
}
