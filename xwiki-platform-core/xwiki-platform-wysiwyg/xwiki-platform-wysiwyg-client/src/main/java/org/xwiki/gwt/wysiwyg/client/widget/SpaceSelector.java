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
package org.xwiki.gwt.wysiwyg.client.widget;

import java.util.List;

import org.xwiki.gwt.wysiwyg.client.wiki.WikiPageReference;
import org.xwiki.gwt.wysiwyg.client.wiki.WikiServiceAsync;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.ListBox;

/**
 * Selector for the spaces in a wiki.
 * 
 * @version $Id$
 */
public class SpaceSelector extends ListBox
{
    /**
     * The wiki whose spaces are listed.
     */
    private String wiki;

    /**
     * The service used to retrieve the list of space names.
     */
    private final WikiServiceAsync wikiService;

    /**
     * Creates a new space selector that uses the given service to retrieve the list of space names.
     * 
     * @param wikiService the service used to retrieve the list of space names
     */
    public SpaceSelector(WikiServiceAsync wikiService)
    {
        this.wikiService = wikiService;
    }

    /**
     * Refreshes the list of spaces for the current wiki.
     * 
     * @param currentSpace the currently selected space.
     * @param cb callback to be able to handle the asynchronous call in this function on the caller side
     */
    public void refreshList(final String currentSpace, final AsyncCallback<List<String>> cb)
    {
        this.clear();
        wikiService.getSpaceNames(wiki, new AsyncCallback<List<String>>()
        {
            public void onFailure(Throwable caught)
            {
                throw new RuntimeException(caught.getMessage());
            }

            public void onSuccess(List<String> result)
            {
                // Add all options keeping selection
                for (String s : result) {
                    addItem(new WikiPageReference(wiki, s, null).getSpacePrettyName(), s);
                    if (s.equals(currentSpace)) {
                        setSelectedIndex(getItemCount() - 1);
                    }
                }
                if (cb != null) {
                    cb.onSuccess(result);
                }
            }
        });
    }

    /**
     * @see #refreshList(String, AsyncCallback)
     * @param currentSpace the currently selected space.
     */
    public void refreshList(final String currentSpace)
    {
        refreshList(currentSpace, null);
    }

    /**
     * @return the currently selected space
     */
    public String getSelectedSpace()
    {
        if (this.getSelectedIndex() >= 0) {
            return this.getValue(this.getSelectedIndex());
        }
        return null;
    }

    /**
     * Sets this selector on the specified space.
     * 
     * @param space the space to set as selected in this selector.
     */
    public void setSelectedSpace(String space)
    {
        for (int i = 0; i < getItemCount(); i++) {
            if (getItemText(i).equals(space)) {
                setSelectedIndex(i);
            }
        }
    }

    /**
     * @return the wiki for this space selector.
     */
    public String getWiki()
    {
        return wiki;
    }

    /**
     * Sets the wiki for which this space selector fetches the spaces. This method onfly sets the value of the wiki, it
     * does not refresh the spaces list.
     * 
     * @param wiki the wiki from which this selector to fetch spaces options.
     */
    public void setWiki(String wiki)
    {
        this.wiki = wiki;
    }
}
