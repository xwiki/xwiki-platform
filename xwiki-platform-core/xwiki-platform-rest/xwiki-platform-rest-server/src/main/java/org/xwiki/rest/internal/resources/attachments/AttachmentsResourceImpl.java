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
package org.xwiki.rest.internal.resources.attachments;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.Part;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.xwiki.attachment.validation.AttachmentValidationException;
import org.xwiki.component.annotation.Component;
import org.xwiki.container.Container;
import org.xwiki.container.servlet.ServletRequest;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.rest.XWikiRestException;
import org.xwiki.rest.internal.Utils;
import org.xwiki.rest.internal.resources.BaseAttachmentsResource;
import org.xwiki.rest.model.jaxb.Attachments;
import org.xwiki.rest.resources.attachments.AttachmentResource;
import org.xwiki.rest.resources.attachments.AttachmentsResource;
import org.xwiki.security.authorization.Right;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.api.Document;

/**
 * @version $Id$
 */
@Component
@Named("org.xwiki.rest.internal.resources.attachments.AttachmentsResourceImpl")
public class AttachmentsResourceImpl extends BaseAttachmentsResource implements AttachmentsResource
{
    private static final String NAME = "name";

    @Inject
    private Container container;

    @Override
    public Attachments getAttachments(String wiki, String spaces, String page, Integer offset, Integer limit,
        Boolean withPrettyNames, String name, String author, String fileTypes) throws XWikiRestException
    {
        Map<String, String> filters = new HashMap<>();
        filters.put(NAME, name);
        filters.put("author", author);
        filters.put("fileTypes", fileTypes);

        return super.getAttachments(new DocumentReference(wiki, parseSpaceSegments(spaces), page), filters, offset,
            limit, withPrettyNames);
    }

    @Override
    public Response addAttachment(String wikiName, String spaceName, String pageName, Boolean withPrettyNames,
        Boolean createPage) throws XWikiRestException, AttachmentValidationException
    {
        Document doc;
        ImmutablePair<String, InputStream> file;
        List<String> spaces;
        try {
            spaces = parseSpaceSegments(spaceName);
            DocumentInfo documentInfo = getDocumentInfo(wikiName, spaces, pageName, null, null, !createPage, true);
            doc = documentInfo.getDocument();

            if (!this.authorization.hasAccess(Right.EDIT, doc.getDocumentReference())) {
                throw new WebApplicationException(Status.UNAUTHORIZED);
            }

            file = getFile();
            if (file.getKey() == null || file.getValue() == null) {
                throw new WebApplicationException(Status.BAD_REQUEST);
            }
        } catch (Exception e) {
            throw new XWikiRestException(e);
        }

        // Attach the file and retrieve the attachment information.
        try {
            AttachmentInfo attachmentInfo =
                storeAndRetrieveAttachment(doc, file.getKey(), file.getValue(), withPrettyNames);

            if (attachmentInfo.isAlreadyExisting()) {
                return Response.status(Status.ACCEPTED).entity(attachmentInfo.getAttachment()).build();
            } else {
                return Response.created(Utils.createURI(this.uriInfo.getBaseUri(), AttachmentResource.class, wikiName,
                    spaces, pageName, file.getKey())).entity(attachmentInfo.getAttachment()).build();
            }
        } catch (XWikiException e) {
            throw new XWikiRestException(e);
        }
    }

    private ImmutablePair<String, InputStream> getFile() throws IOException, ServletException
    {
        // The original file name that was sent along with the file content. The value is read from the
        // Content-Disposition header.
        String originalFileName = null;
        InputStream inputStream = null;

        // The file name submitted as a separate form field. When present, it overwrites the original file name.
        String overwritingFileName = null;

        HttpServletRequest request = ((ServletRequest) this.container.getRequest()).getHttpServletRequest();
        Collection<Part> parts = request.getParts();

        for (Part part : parts) {
            String fileName = part.getSubmittedFileName();
            if (fileName != null) {
                // This body part represents the file itself.
                originalFileName = fileName;
                inputStream = part.getInputStream();
            } else {
                // This body part represents a plain form field.
                if ("filename".equals(part.getName())) {
                    // This body part represents the form field used to overwrite the original file name.
                    // We don't use bodyPart.getContent() because it doesn't use UTF-8 to read the input stream.
                    overwritingFileName = IOUtils.toString(part.getInputStream(), StandardCharsets.UTF_8);
                }
            }
        }

        String attachmentName = overwritingFileName != null ? overwritingFileName : originalFileName;
        return new ImmutablePair<>(attachmentName, inputStream);
    }
}
