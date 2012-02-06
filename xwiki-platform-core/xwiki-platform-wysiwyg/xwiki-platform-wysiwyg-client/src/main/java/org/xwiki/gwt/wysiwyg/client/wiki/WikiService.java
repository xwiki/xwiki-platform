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

import org.xwiki.component.annotation.ComponentRole;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;

/**
 * The service interface used on the server.
 * <p>
 * NOTE: This component interface should be split in multiple domain specific interfaces. Don't add any more methods!
 * 
 * @version $Id$
 */
@ComponentRole
@RemoteServiceRelativePath("WikiService.gwtrpc")
public interface WikiService extends RemoteService
{
    /**
     * Checks if the editor is running in a multiwiki environment.
     * 
     * @return {@code true} if we are in a multiwiki environment, {@code false} otherwise
     */
    Boolean isMultiWiki();

    /**
     * @return a list containing the names of all wikis
     */
    List<String> getVirtualWikiNames();

    /**
     * @param wikiName the name of the wiki to search for spaces. If this is {@code null} the current wiki will be used
     * @return a list of all spaces names in the specified wiki
     */
    List<String> getSpaceNames(String wikiName);

    /**
     * @param wikiName the name of the wiki. Pass {@code null} if the current wiki should be used instead
     * @param spaceName the name of the space
     * @return the list of the page names from a given space and a given wiki
     */
    List<String> getPageNames(String wikiName, String spaceName);

    /**
     * @param wikiName the name of the wiki where to look for modified pages
     * @param start the start index of the list of pages to return
     * @param count the number of pages to return
     * @return the recently {@code count} modified pages of the current user from the specified wiki, starting from
     *         position {@code start}
     */
    List<WikiPage> getRecentlyModifiedPages(String wikiName, int start, int count);

    /**
     * @param wikiName the wiki where to run the search
     * @param start the start index of the list of pages to return
     * @param count the number of pages to return
     * @param keyword the keyword to search the pages for
     * @return the {@code count} pages whose full name or title match the keyword, starting from position {@code start}
     */
    List<WikiPage> getMatchingPages(String wikiName, String keyword, int start, int count);

    /**
     * Creates an entity link configuration object (URL, link reference) for a link with the specified origin and
     * destination. The link reference in the returned {@link EntityConfig} is relative to the link origin.
     * 
     * @param origin the origin of the link
     * @param destination the destination of the link
     * @return the link configuration object that can be used to insert the link in the origin page
     */
    EntityConfig getEntityConfig(EntityReference origin, ResourceReference destination);

    /**
     * Returns information about the referenced attachment.
     * 
     * @param attachmentReference an attachment reference
     * @return an {@link Attachment} containing the serialized reference and the access URL of the specified attachment,
     *         or {@code null} in case the attachment was not found
     */
    Attachment getAttachment(AttachmentReference attachmentReference);

    /**
     * Returns all the image attachments from the referred page.
     * 
     * @param documentReference a reference to the document to get the images from
     * @return list of the image attachments
     */
    List<Attachment> getImageAttachments(WikiPageReference documentReference);

    /**
     * Returns all the attachments from the referred page.
     * 
     * @param documentReference a reference to the document to get the attachments from
     * @return list of the attachments
     */
    List<Attachment> getAttachments(WikiPageReference documentReference);

    /**
     * @param documentReference a document reference
     * @return the URL that can be used to upload an attachment to the specified document
     */
    String getUploadURL(WikiPageReference documentReference);

    /**
     * Parses the given link reference and extracts a reference to the linked resource. The returned resource reference
     * is resolved relative to the given base entity reference.
     * 
     * @param linkReference a serialized link reference
     * @param baseReference the entity reference used to resolve the linked resource reference
     * @return a reference to the linked resource
     */
    ResourceReference parseLinkReference(String linkReference, EntityReference baseReference);
}
