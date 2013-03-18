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
     * Checks if the editor is running in a multiwiki environment.
     * 
     * @deprecated Multiwiki is on by default, starting with XWiki 5.0.
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
     * @param wikiName the name of the wiki to search for spaces. If this is {@code null} the current wiki will be used
     * @param async object used for asynchronous communication between server and client
     */
    void getSpaceNames(String wikiName, AsyncCallback<List<String>> async);

    /**
     * Returns the list of the page names from a given space and a given wiki.
     * 
     * @param wikiName the name of the wiki. Pass {@code null} if the current wiki should be used instead
     * @param spaceName the name of the space
     * @param async object used for asynchronous communication between server and client
     */
    void getPageNames(String wikiName, String spaceName, AsyncCallback<List<String>> async);

    /**
     * @param wikiName the name of the wiki where to look for modified pages
     * @param start the start index of the list of pages to return
     * @param count the number of pages to return
     * @param async object used for asynchronous communication between server and client, to return on success the
     *            recently {@code count} modified pages of the current user, starting from position {@code start}
     */
    void getRecentlyModifiedPages(String wikiName, int start, int count, AsyncCallback<List<WikiPage>> async);

    /**
     * @param wikiName the wiki where to run the search
     * @param start the start index of the list of pages to return
     * @param count the number of pages to return
     * @param keyword the keyword to search the pages for
     * @param async object used for asynchronous communication between server and client, to return on success the
     *            {@code count} pages whose full name or title match the keyword, starting from position {@code start}
     */
    void getMatchingPages(String wikiName, String keyword, int start, int count, AsyncCallback<List<WikiPage>> async);

    /**
     * Creates an entity link configuration object (URL, reference) for a link with the specified origin and
     * destination. The string serialization of the entity reference in the returned {@link EntityConfig} is relative to
     * the link origin.
     * 
     * @param origin the origin of the link
     * @param destination the destination of the link
     * @param async object used for asynchronous communication between server and client.
     */
    void getEntityConfig(EntityReference origin, ResourceReference destination, AsyncCallback<EntityConfig> async);

    /**
     * Returns information about the referenced attachment. Note that the {@code EntityReference#getFileName()} name
     * will be cleaned to match the attachment names cleaning rules, and the returned attachment serialized reference
     * and access URL will be generated with the cleaned name.
     * 
     * @param attachmentReference an attachment reference
     * @param async object used for asynchronous communication between server and client, to return, on success, an
     *            {@link Attachment} containing the serialized reference and the access URL of the specified attachment,
     *            or {@code null} in case the attachment was not found
     */
    void getAttachment(AttachmentReference attachmentReference, AsyncCallback<Attachment> async);

    /**
     * Returns all the image attachments from the referred page.
     * 
     * @param documentReference a reference to the document to get the images from
     * @param async object used for asynchronous communication between server and client
     */
    void getImageAttachments(WikiPageReference documentReference, AsyncCallback<List<Attachment>> async);

    /**
     * Returns all the attachments from the referred page.
     * 
     * @param documentReference a reference to the document to get the attachments from
     * @param async object used for asynchronous communication between server and client
     */
    void getAttachments(WikiPageReference documentReference, AsyncCallback<List<Attachment>> async);

    /**
     * Returns the URL to be used to upload an attachment to the specified document.
     * 
     * @param documentReference the document for which to retrieve the upload URL
     * @param async object used for asynchronous communication between server and client
     */
    void getUploadURL(WikiPageReference documentReference, AsyncCallback<String> async);

    /**
     * Parses the given link reference and extracts a reference to the linked resource. The returned resource reference
     * is resolved relative to the given base entity reference.
     * 
     * @param linkReferenceAsString a serialized link reference
     * @param baseReference the entity reference used to resolve the linked resource reference
     * @param async object used for asynchronous communication between server and client
     */
    void parseLinkReference(String linkReferenceAsString, EntityReference baseReference,
        AsyncCallback<ResourceReference> async);
}
