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
package org.xwiki.rest.resources.attachments;

import javax.ws.rs.DELETE;
import javax.ws.rs.Encoded;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

import org.xwiki.attachment.validation.AttachmentValidationException;
import org.xwiki.rest.XWikiRestException;

/**
 * @version $Id$
 */
@Path("/wikis/{wikiName}/spaces/{spaceName: .+}/pages/{pageName}/attachments/{attachmentName}")
public interface AttachmentResource
{
    /**
     * Returns the content of an attachment.
     *
     * @param wikiName the identifier of the wiki containing the page, for example {@code xwiki} for the main wiki
     * @param spaceName the reference of the space(s) containing the page; nested spaces are separated by
     *  {@code /spaces/} (for example {@code A/spaces/B/spaces/C} for the space {@code A.B.C})
     * @param pageName the name of the page holding the attachment, for example {@code WebHome}
     * @param attachmentName the file name of the attachment, for example {@code photo.png}
     * @return a response streaming the attachment's binary content with its media type
     * @throws XWikiRestException if the attachment cannot be retrieved, for example the page or attachment does not
     *  exist or the user is not allowed to view it
     */
    @GET Response getAttachment(
            @PathParam("wikiName") String wikiName,
            @PathParam("spaceName") @Encoded String spaceName,
            @PathParam("pageName") String pageName,
            @PathParam("attachmentName") String attachmentName
    ) throws XWikiRestException;

    /**
     * Creates the attachment if it does not exist yet, or updates its content otherwise.
     *
     * @param wikiName the identifier of the wiki containing the page, for example {@code xwiki} for the main wiki
     * @param spaceName the reference of the space(s) containing the page; nested spaces are separated by
     *  {@code /spaces/} (for example {@code A/spaces/B/spaces/C} for the space {@code A.B.C})
     * @param pageName the name of the page holding the attachment, for example {@code WebHome}
     * @param attachmentName the file name to give the attachment, for example {@code photo.png}
     * @param content the raw binary content of the attachment
     * @return a response holding the attachment metadata, with status {@code 201} when the attachment was created and
     *  {@code 202} when an existing attachment was updated
     * @throws XWikiRestException if the attachment cannot be stored, for example the user is not allowed to edit the
     *  page
     * @throws AttachmentValidationException if the attachment content is rejected by the configured attachment
     *  validation (for example a forbidden media type or an excessive size)
     */
    @PUT Response putAttachment(
            @PathParam("wikiName") String wikiName,
            @PathParam("spaceName") @Encoded String spaceName,
            @PathParam("pageName") String pageName,
            @PathParam("attachmentName") String attachmentName,
            byte[] content
    ) throws XWikiRestException, AttachmentValidationException;

    /**
     * Deletes an attachment.
     *
     * @param wikiName the identifier of the wiki containing the page, for example {@code xwiki} for the main wiki
     * @param spaceName the reference of the space(s) containing the page; nested spaces are separated by
     *  {@code /spaces/} (for example {@code A/spaces/B/spaces/C} for the space {@code A.B.C})
     * @param pageName the name of the page holding the attachment, for example {@code WebHome}
     * @param attachmentName the file name of the attachment to delete, for example {@code photo.png}
     * @throws XWikiRestException if the user is not allowed to edit the page or if the deletion fails
     */
    @DELETE void deleteAttachment(
            @PathParam("wikiName") String wikiName,
            @PathParam("spaceName") @Encoded String spaceName,
            @PathParam("pageName") String pageName,
            @PathParam("attachmentName") String attachmentName
    ) throws XWikiRestException;
}
