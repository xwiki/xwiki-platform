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
package com.xpn.xwiki.wysiwyg.client.plugin.link;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.xpn.xwiki.wysiwyg.client.WysiwygService;

/**
 * Generates html link blocks for all types of links.
 * 
 * @version $Id$
 */
public final class LinkGenerator
{
    /**
     * The singleton instance of this class.
     */
    private static LinkGenerator instance;

    /**
     * Class constructor, private so that the class is a singleton.
     */
    private LinkGenerator()
    {
    }

    /**
     * @return the instance of this class.
     */
    public static synchronized LinkGenerator getInstance()
    {
        if (instance == null) {
            instance = new LinkGenerator();
        }
        return instance;
    }

    /**
     * Generates link to an external url (web page or email address).
     * 
     * @param label link label
     * @param externalURL external url to link to
     * @return the html link block.
     */
    public String getExternalLink(String label, String externalURL)
    {
        return createLinkHTML(externalURL, "wikiexternallink", externalURL, label);
    }

    /**
     * Generates link to a new wiki page.
     * 
     * @param label link label
     * @param wikiName wiki of the targeted page
     * @param spaceName space of the targeted page
     * @param pageName name of the targeted page
     * @param async callback to handle async call on the caller side
     * @return the html link block.
     */
    public String getNewPageLink(final String label, final String wikiName, final String spaceName,
        final String pageName, final AsyncCallback<String> async)
    {
        WysiwygService.Singleton.getInstance().createPageURL(wikiName, spaceName, pageName, null, null,
            new AsyncCallback<String>()
            {
                public void onFailure(Throwable arg0)
                {
                    async.onFailure(arg0);
                }

                public void onSuccess(String result)
                {
                    String link =
                        createLinkHTML(getWikiPageReference(result, wikiName), "wikicreatelink", result, label);
                    async.onSuccess(link);
                }
            });
        return "";
    }

    /**
     * Generates link to an existing page.
     * 
     * @param label link label
     * @param wikiName wiki of the targeted page
     * @param spaceName space of the targeted page
     * @param pageName name of the targeted page
     * @param revision version of the page to link to
     * @param anchor anchor in the page to link to
     * @param async callback to handle async call on the caller side
     * @return the html link block.
     */
    public String getExistingPageLink(final String label, final String wikiName, final String spaceName,
        final String pageName, String revision, String anchor, final AsyncCallback<String> async)
    {
        WysiwygService.Singleton.getInstance().createPageURL(wikiName, spaceName, pageName, revision, anchor,
            new AsyncCallback<String>()
            {
                public void onFailure(Throwable arg0)
                {
                    // pass it further
                    async.onFailure(arg0);
                }

                public void onSuccess(String result)
                {
                    String link = createLinkHTML(getWikiPageReference(result, wikiName), "wikilink", result, label);
                    async.onSuccess(link);
                }
            });
        return "";
    }

    /**
     * Builds the reference of a wiki link, from the URL of the page as returned by the server.
     * 
     * @param url the URL of the page, as returned by the server
     * @param wikiName the wiki name in which the page is located. Can be <code>null</code> if this is not a multiwiki.
     * @return the wiki page reference, parsed from the returned url
     */
    private String getWikiPageReference(String url, String wikiName)
    {
        int paramsIndex = url.indexOf('?');
        int hashIndex = url.indexOf('#');
        // If the hash index is to the left of the qm index or the qm index is negative, copy from hash
        if ((hashIndex < paramsIndex && hashIndex >= 0) || (paramsIndex < 0)) {
            paramsIndex = hashIndex;
        }
        String params = "";
        String strippedUrl = url;
        if (paramsIndex > 0) {
            params = url.substring(paramsIndex);
            strippedUrl = url.substring(0, paramsIndex);
        }
        // get the page name and the space name to build the reference
        String pageName = "";
        int lastIndexOf = strippedUrl.lastIndexOf('/');
        pageName = strippedUrl.substring(lastIndexOf + 1).trim();
        if (pageName.length() == 0) {
            pageName = "WebHome";
        }
        strippedUrl = lastIndexOf > 0 ? strippedUrl.substring(0, lastIndexOf) : "";
        lastIndexOf = strippedUrl.lastIndexOf('/');
        String spaceName = strippedUrl.substring(lastIndexOf + 1).trim();
        if (spaceName.length() == 0) {
            // default space. This shouldn't happen, though, since the server should return complete urls
            spaceName = "Main";
        }

        return ((wikiName != null && wikiName.length() > 0) ? wikiName + ":" : "") + spaceName + "." + pageName
            + params;
    }

    /**
     * @param linkReference the reference of the link to create
     * @param wrappingSpanClassName the value of the class attribute of the wrapping span
     * @param anchorHref the href of the link anchor
     * @param label the label of the created link
     * @return the link html, created for the specified parameters.
     */
    private String createLinkHTML(String linkReference, String wrappingSpanClassName, String anchorHref, String label)
    {
        return "<!--startwikilink:" + linkReference + "--><span class=\"" + wrappingSpanClassName + "\"><a href=\""
            + anchorHref + "\">" + label + "</a></span><!--stopwikilink-->";
    }
}
