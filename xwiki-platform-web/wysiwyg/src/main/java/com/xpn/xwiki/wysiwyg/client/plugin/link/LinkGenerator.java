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
import com.xpn.xwiki.wysiwyg.client.util.StringUtils;

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
        WysiwygService.Singleton.getInstance().getPageLink(wikiName, spaceName, pageName, null, null,
            new AsyncCallback<LinkConfig>()
            {
                public void onFailure(Throwable t)
                {
                    async.onFailure(t);
                }

                public void onSuccess(LinkConfig result)
                {
                    String link =
                        createLinkHTML(getWikiPageReference(result), "wikicreatelink", result.getUrl(), label);
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
        WysiwygService.Singleton.getInstance().getPageLink(wikiName, spaceName, pageName, revision, anchor,
            new AsyncCallback<LinkConfig>()
            {
                public void onFailure(Throwable t)
                {
                    // pass it further
                    async.onFailure(t);
                }

                public void onSuccess(LinkConfig result)
                {
                    String link = createLinkHTML(getWikiPageReference(result), "wikilink", result.getUrl(), label);
                    async.onSuccess(link);
                }
            });
        return "";
    }

    /**
     * Builds the reference of a wiki link, from the link data as returned by the server.
     * 
     * @param config the link data, as returned by the server
     * @return the wiki page reference, created from the returned data
     */
    private String getWikiPageReference(LinkConfig config)
    {
        String url = config.getUrl();
        int paramsIndex = url.indexOf('?');
        int hashIndex = url.indexOf('#');
        // If the hash index is to the left of the qm index or the qm index is negative, copy from hash
        if ((hashIndex < paramsIndex && hashIndex >= 0) || (paramsIndex < 0)) {
            paramsIndex = hashIndex;
        }
        String params = "";
        if (paramsIndex > 0) {
            params = url.substring(paramsIndex);
        }

        return (!StringUtils.isEmpty(config.getWiki()) ? config.getWiki() + ":" : "") + config.getSpace() + "."
            + config.getPage() + params;
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
