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

import org.xwiki.gwt.wysiwyg.client.wiki.WikiServiceAsync;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.ListBox;

/**
 * Selector to choose the wiki from the list of wikis in this instance.
 * 
 * @version $Id$
 */
public class WikiSelector extends ListBox
{
    /**
     * The service used to retrieve the list of virtual wiki names.
     */
    private final WikiServiceAsync wikiService;

    /**
     * Builds a wiki selector. Note that this function does not actually fill the list of wikis, you need to explicitly
     * call {@link #refreshList(String, AsyncCallback)} after this constructor.
     * 
     * @param wikiService the service used to retrieve the list of virtual wiki names
     */
    public WikiSelector(WikiServiceAsync wikiService)
    {
        this.wikiService = wikiService;
    }

    /**
     * Get the wikis list and populate the wikis list box.
     * 
     * @param currentWiki the currently selected wiki, to restore selection.
     * @param cb callback to be able to handle the asynchronous call in this function on the caller side
     */
    public void refreshList(final String currentWiki, final AsyncCallback<List<String>> cb)
    {
        this.clear();
        wikiService.getVirtualWikiNames(new AsyncCallback<List<String>>()
        {
            public void onFailure(Throwable caught)
            {
                throw new RuntimeException(caught.getMessage());
            }

            public void onSuccess(List<String> result)
            {
                for (String wiki : result) {
                    addItem(wiki);
                    if (wiki.equals(currentWiki)) {
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
     * @param currentWiki the currently selected wiki, to restore selection.
     */
    public void refreshList(final String currentWiki)
    {
        refreshList(currentWiki, null);
    }

    /**
     * @return the currently selected wiki.
     */
    public String getSelectedWiki()
    {
        if (this.getSelectedIndex() >= 0) {
            return this.getItemText(this.getSelectedIndex());
        }
        return null;
    }

    /**
     * Sets this selector on the specified wiki.
     * 
     * @param wiki the wiki to set as selected in this selector.
     */
    public void setSelectedWiki(String wiki)
    {
        for (int i = 0; i < getItemCount(); i++) {
            if (getItemText(i).equals(wiki)) {
                setSelectedIndex(i);
            }
        }
    }
}
