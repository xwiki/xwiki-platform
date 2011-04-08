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
 * Selector for the pages from a space in a wiki.
 * 
 * @version $Id$
 */
public class PageSelector extends ListBox
{
    /**
     * The wiki from which to get the pages for this selector.
     */
    private String wiki;

    /**
     * The space from which to get the pages for this selector.
     * 
     * @see #PageSelector(String, String)
     */
    private String space;

    /**
     * The service used to retrieve the list of page names.
     */
    private final WikiServiceAsync wikiService;

    /**
     * Creates a new page selector that uses the given service to get the list of page names.
     * 
     * @param wikiService the service used to retrieve the list of page names
     */
    public PageSelector(WikiServiceAsync wikiService)
    {
        this.wikiService = wikiService;
    }

    /**
     * Refreshes the list of pages for this selector.
     * 
     * @param currentPage the currently selected page, to restore selection after update.
     * @param cb callback to be able to handle the asynchronous call in this function on the caller side
     */
    public void refreshList(final String currentPage, final AsyncCallback<List<String>> cb)
    {
        this.clear();
        wikiService.getPageNames(wiki, space, new AsyncCallback<List<String>>()
        {
            public void onFailure(Throwable caught)
            {
                throw new RuntimeException(caught.getMessage());
            }

            public void onSuccess(List<String> result)
            {
                for (String s : result) {
                    addItem(s);
                    if (s.equals(currentPage)) {
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
     * @param currentPage the currently selected page, to restore selection after update.
     */
    public void refreshList(String currentPage)
    {
        refreshList(currentPage, null);
    }

    /**
     * @return the currently selected page in this selector.
     */
    public String getSelectedPage()
    {
        if (this.getSelectedIndex() >= 0) {
            return this.getItemText(this.getSelectedIndex());
        }
        return null;
    }

    /**
     * Sets this selector on the specified page.
     * 
     * @param page the page to set as selected in this selector.
     */
    public void setSelectedPage(String page)
    {
        for (int i = 0; i < getItemCount(); i++) {
            if (getItemText(i).equals(page)) {
                setSelectedIndex(i);
            }
        }
    }

    /**
     * @return the wiki
     */
    public String getWiki()
    {
        return wiki;
    }

    /**
     * Sets the current wiki for this selector. Note that this function does not update the list of pages, you must
     * explicitly call {@link #refreshList(String)} after setting this value.
     * 
     * @param wiki the wiki to set
     */
    public void setWiki(String wiki)
    {
        this.wiki = wiki;
    }

    /**
     * Sets the space for this selector. Note that this function does not update the list of pages, you must explicitly
     * call {@link #refreshList(String)} after setting this value.
     * 
     * @return the space
     */
    public String getSpace()
    {
        return space;
    }

    /**
     * @param space the space to set
     */
    public void setSpace(String space)
    {
        this.space = space;
    }
}
