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
package org.xwiki.gwt.wysiwyg.client.wiki;

import java.util.List;

import org.xwiki.gwt.wysiwyg.client.plugin.link.LinkConfig;

import com.google.gwt.user.client.rpc.AsyncCallback;

/**
 * Service interface used on the client. It should have all the methods from {@link WikiService} with an additional
 * {@link AsyncCallback} parameter. This is specific to GWT's architecture.
 * 
 * @version $Id$
 */
public interface WikiServiceAsync
{
    /**
     * Check if the current wiki is part of a multiwiki (i.e. this is a virtual wiki).
     * 
     * @param async object used for asynchronous communication between server and client
     */
    void isMultiWiki(AsyncCallback<Boolean> async);

    /**
     * Returns a list containing the names of all wikis.
     * 
     * @param async async object used for asynchronous communication between server and client
     */
    void getVirtualWikiNames(AsyncCallback<List<String>> async);

    /**
     * Returns a list of all spaces names in the specified wiki.
     * 
     * @param wikiName the name of the wiki to search for spaces. If this is <code>null</code>, the current wiki will be
     *            used.
     * @param async object used for asynchronous communication between server and client
     */
    void getSpaceNames(String wikiName, AsyncCallback<List<String>> async);

    /**
     * Returns the list of the page names from a given space and a given wiki.
     * 
     * @param wikiName the name of the wiki. Pass <code>null</code> if this should use the current wiki.
     * @param spaceName the name of the space
     * @param async object used for asynchronous communication between server and client
     */
    void getPageNames(String wikiName, String spaceName, AsyncCallback<List<String>> async);

    /**
     * @param start the start index of the list of pages to return
     * @param count the number of pages to return
     * @param async object used for asynchronous communication between server and client, to return on success the
     *            recently {@code count} modified pages of the current user, starting from position {@code start}
     */
    void getRecentlyModifiedPages(int start, int count, AsyncCallback<List<WikiPage>> async);

    /**
     * @param start the start index of the list of pages to return
     * @param count the number of pages to return
     * @param keyword the keyword to search the pages for
     * @param async object used for asynchronous communication between server and client, to return on success the
     *            {@code count} pages whose full name or title match the keyword, starting from position {@code start}
     */
    void getMatchingPages(String keyword, int start, int count, AsyncCallback<List<WikiPage>> async);

    /**
     * Creates a page link (URL, reference) from the given parameters. None of them are mandatory, if one misses, it is
     * replaced with a default value.
     * 
     * @param wikiName the name of the wiki to which to link
     * @param spaceName the name of the space of the page. If this parameter is missing, it is replaced with the space
     *            of the current document in the context.
     * @param pageName the name of the page to which to link to. If it's missing, it is replaced with "WebHome".
     * @param revision the value for the page revision to which to link to. If this is missing, the link is made to the
     *            latest revision, the default view action for the document.
     * @param anchor the name of the anchor type.
     * @param async object used for asynchronous communication between server and client.
     */
    void getPageLink(String wikiName, String spaceName, String pageName, String revision, String anchor,
        AsyncCallback<LinkConfig> async);

    /**
     * Returns attachment information from the passed parameters, testing if the passed attachment exists. Note that the
     * {@code attachmentName} name will be cleaned to match the attachment names cleaning rules, and the attachment
     * reference and URL will be generated with the cleaned name. This function will be used as a method to test the
     * correct upload of a file to a page.
     * 
     * @param wikiName the name of the wiki of the page the file is attached to
     * @param spaceName the name of the space of the page the file is attached to
     * @param pageName the name of the page the file is attached to
     * @param attachmentName the uncleaned name of the attachment, which is to be cleaned on the server
     * @param async object used for asynchronous communication between server and client, to return, on success, an
     *            {@link Attachment} containing the reference and the URL of the attachment, or {@code null} in case the
     *            attachment was not found
     */
    void getAttachment(String wikiName, String spaceName, String pageName, String attachmentName,
        AsyncCallback<Attachment> async);

    /**
     * Returns all the image attachments from the referred page.
     * 
     * @param wikiName the name of the wiki to get images from
     * @param spaceName the name of the space to get image attachments from
     * @param pageName the name of the page to get image attachments from
     * @param async object used for asynchronous communication between server and client.
     */
    void getImageAttachments(String wikiName, String spaceName, String pageName, AsyncCallback<List<Attachment>> async);

    /**
     * Returns all the attachments from the referred page.
     * 
     * @param wikiName the name of the wiki to get attachments from
     * @param spaceName the name of the space to get attachments from
     * @param pageName the name of the page to get attachments from
     * @param async object used for asynchronous communication between server and client.
     */
    void getAttachments(String wikiName, String spaceName, String pageName, AsyncCallback<List<Attachment>> async);
}
