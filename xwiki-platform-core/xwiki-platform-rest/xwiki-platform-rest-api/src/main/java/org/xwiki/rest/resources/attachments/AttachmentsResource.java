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

import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.Encoded;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.xwiki.attachment.validation.AttachmentValidationException;
import org.xwiki.rest.XWikiRestException;
import org.xwiki.rest.model.jaxb.Attachments;

/**
 * @version $Id$
 */
@Path("/wikis/{wikiName}/spaces/{spaceName: .+}/pages/{pageName}/attachments")
public interface AttachmentsResource
{
    /**
     * Returns the attachments of a page, with optional pagination and filtering.
     *
     * @param wikiName the identifier of the wiki containing the page, for example {@code xwiki} for the main wiki
     * @param spaceName the reference of the space(s) containing the page; nested spaces are separated by
     *  {@code /spaces/} (for example {@code A/spaces/B/spaces/C} for the space {@code A.B.C})
     * @param pageName the name of the page whose attachments are listed, for example {@code WebHome}
     * @param start the 0-based index of the first attachment to return, used together with {@code number} for
     *  pagination; defaults to {@code 0}
     * @param number the maximum number of attachments to return; when {@code null} the wiki's configured REST query
     *  limit is used, and a value that is negative or larger than that configured limit is rejected with a
     *  {@code 400} response
     * @param withPrettyNames when {@code true}, also computes human-readable display names (for example the author's
     *  display name and the document title) in addition to the technical references, at some extra cost; defaults to
     *  {@code false}
     * @param name keeps only attachments whose file name contains this value (case-insensitive), for example
     *  {@code logo}; empty by default (no filtering)
     * @param author keeps only attachments whose author reference contains this value (case-insensitive), for example
     *  {@code XWiki.Admin}; empty by default (no filtering)
     * @param types a comma-separated list of media types and/or file-name extensions to keep, for example
     *  {@code image/png,pdf}; empty by default (no filtering)
     * @return the matching attachments the current user is allowed to view, within the requested pagination window
     * @throws XWikiRestException if the attachments cannot be retrieved
     */
    // Needs a lot of parameters to bind path and query parameters
    @SuppressWarnings("checkstyle:ParameterNumber")
    @GET Attachments getAttachments(
            @PathParam("wikiName") String wikiName,
            @PathParam("spaceName") @Encoded String spaceName,
            @PathParam("pageName") String pageName,
            @QueryParam("start") @DefaultValue("0") Integer start,
            @QueryParam("number") Integer number,
            @QueryParam("prettyNames") @DefaultValue("false") Boolean withPrettyNames,
            @QueryParam("name") @DefaultValue("") String name,
            @QueryParam("author") @DefaultValue("") String author,
            @QueryParam("types") @DefaultValue("") String types
    ) throws XWikiRestException;

    /**
     * Adds an attachment to a page, its content being sent as {@code multipart/form-data}. The attachment file name is
     * taken from the {@code Content-Disposition} of the uploaded part, or from a {@code filename} form field when
     * present (the form field wins).
     *
     * @param wikiName the identifier of the wiki containing the page, for example {@code xwiki} for the main wiki
     * @param spaceName the reference of the space(s) containing the page; nested spaces are separated by
     *  {@code /spaces/} (for example {@code A/spaces/B/spaces/C} for the space {@code A.B.C})
     * @param pageName the name of the page to attach the file to, for example {@code WebHome}
     * @param withPrettyNames when {@code true}, also computes human-readable display names (for example the author's
     *  display name and the document title) in the returned metadata, at some extra cost; defaults to {@code false}
     * @param createPage when {@code true}, creates the page if it does not exist yet; when {@code false} (the default),
     *  attaching to a missing page fails
     * @return a response holding the attachment metadata, with status {@code 201} when the attachment was created and
     *  {@code 202} when an existing attachment was updated
     * @throws XWikiRestException if the attachment cannot be stored, for example the user is not allowed to edit the
     *  page or no file part was supplied
     * @throws AttachmentValidationException if the attachment content is rejected by the configured attachment
     *  validation (for example a forbidden media type or an excessive size)
     */
    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA) Response addAttachment(
            @PathParam("wikiName") String wikiName,
            @PathParam("spaceName") @Encoded String spaceName,
            @PathParam("pageName") String pageName,
            @QueryParam("prettyNames") @DefaultValue("false") Boolean withPrettyNames,
            @QueryParam("createPage") @DefaultValue("false") Boolean createPage
    ) throws XWikiRestException, AttachmentValidationException;
}
