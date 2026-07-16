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

import javax.ws.rs.DefaultValue;
import javax.ws.rs.Encoded;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;

import org.xwiki.rest.XWikiRestException;
import org.xwiki.rest.model.jaxb.Attachment;

/**
 * Provides access to the attachment metadata. Attachment data is provided by {@link AttachmentResource}.
 * 
 * @version $Id$
 * @since 11.5RC1
 */
@Path("/wikis/{wikiName}/spaces/{spaceName: .+}/pages/{pageName}/attachments/{attachmentName}/metadata")
public interface AttachmentMetadataResource
{
    /**
     * Returns the metadata of an attachment (such as its size, media type, version and author) without its binary
     * content.
     *
     * @param wikiName the identifier of the wiki containing the page, for example {@code xwiki} for the main wiki
     * @param spaceName the reference of the space(s) containing the page; nested spaces are separated by
     *  {@code /spaces/} (for example {@code A/spaces/B/spaces/C} for the space {@code A.B.C})
     * @param pageName the name of the page holding the attachment, for example {@code WebHome}
     * @param attachmentName the file name of the attachment, for example {@code photo.png}
     * @param withPrettyNames when {@code true}, also computes human-readable display names (for example the author's
     *  display name and the document title) in addition to the technical references, at some extra cost; defaults to
     *  {@code false}
     * @return the attachment metadata
     * @throws XWikiRestException if the attachment metadata cannot be retrieved, for example the page or attachment
     *  does not exist
     */
    @GET
    Attachment getAttachment(@PathParam("wikiName") String wikiName, @PathParam("spaceName") @Encoded String spaceName,
        @PathParam("pageName") String pageName, @PathParam("attachmentName") String attachmentName,
        @QueryParam("prettyNames") @DefaultValue("false") Boolean withPrettyNames) throws XWikiRestException;
}
