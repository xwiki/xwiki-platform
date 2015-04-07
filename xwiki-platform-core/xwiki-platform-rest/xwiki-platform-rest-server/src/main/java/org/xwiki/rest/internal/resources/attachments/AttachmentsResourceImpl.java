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

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Enumeration;

import javax.inject.Named;
import javax.mail.BodyPart;
import javax.mail.Header;
import javax.mail.Multipart;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.xwiki.component.annotation.Component;
import org.xwiki.rest.XWikiRestException;
import org.xwiki.rest.internal.Utils;
import org.xwiki.rest.internal.resources.BaseAttachmentsResource;
import org.xwiki.rest.model.jaxb.Attachments;
import org.xwiki.rest.resources.attachments.AttachmentResource;
import org.xwiki.rest.resources.attachments.AttachmentsResource;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.api.Document;

/**
 * @version $Id$
 */
@Component
@Named("org.xwiki.rest.internal.resources.attachments.AttachmentsResourceImpl")
public class AttachmentsResourceImpl extends BaseAttachmentsResource implements AttachmentsResource
{
    private static String FORM_FILENAME_FIELD = "filename";

    @Override
    public Attachments getAttachments(String wikiName, String spaceName, String pageName, Integer start, Integer number,
            Boolean withPrettyNames) throws XWikiRestException
    {
        try {
            DocumentInfo documentInfo = getDocumentInfo(wikiName, spaceName, pageName, null, null, true, false);
            Document doc = documentInfo.getDocument();

            return getAttachmentsForDocument(doc, start, number, withPrettyNames);
        } catch (XWikiException e) {
            throw new XWikiRestException(e);
        }
    }

    @Override
    public Response addAttachment(String wikiName, String spaceName, String pageName, Multipart multipart)
            throws XWikiRestException
    {
        try {
            DocumentInfo documentInfo = getDocumentInfo(wikiName, spaceName, pageName, null, null, true, true);

            Document doc = documentInfo.getDocument();

            if (!doc.hasAccessLevel("edit", Utils.getXWikiUser(componentManager))) {
                throw new WebApplicationException(Status.UNAUTHORIZED);
            }

            /* The name to be used */
            String attachmentName = null;

            /* The actual filename of the sent file */
            String actualFileName = null;

            /* The specified file name using a form field */
            String overriddenFileName = null;
            String contentType = null;
            InputStream inputStream = null;

            for (int i = 0; i < multipart.getCount(); i++) {
                BodyPart bodyPart = multipart.getBodyPart(i);

                /* Get the content disposition headers */
                Enumeration e = bodyPart.getMatchingHeaders(new String[]{ "Content-disposition" });
                while (e.hasMoreElements()) {
                    Header h = (Header) e.nextElement();

                    /* Parse header data. Normally headers are in the form form-data; key="value"; ... */
                    if (h.getValue().startsWith("form-data")) {
                        String[] fieldData = h.getValue().split(";");
                        for (String s : fieldData) {
                            String[] pair = s.split("=");
                            if (pair.length == 2) {
                                String key = pair[0].trim();
                                String value = pair[1].replace("\"", "").trim();

                                if ("name".equals(key)) {
                                    if (FORM_FILENAME_FIELD.equals(value)) {
                                        overriddenFileName = bodyPart.getContent().toString();
                                    }
                                } else if ("filename".equals(key)) {
                                    actualFileName = value;
                                    contentType = bodyPart.getContentType();
                                    inputStream = bodyPart.getInputStream();
                                }
                            }
                        }
                    }
                }
            }

            if (overriddenFileName != null) {
                attachmentName = overriddenFileName;
            } else {
                attachmentName = actualFileName;
            }

            if (attachmentName == null) {
                throw new WebApplicationException(Status.BAD_REQUEST);
            }

            byte[] buffer = new byte[4096];
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            while (true) {
                int read = inputStream.read(buffer);
                if (read != 4096) {
                    if (read != -1) {
                        baos.write(buffer, 0, read);
                    }

                    break;
                } else {
                    baos.write(buffer);
                }
            }
            baos.flush();

            /* Attach the file */
            AttachmentInfo attachmentInfo = storeAttachment(doc, attachmentName, baos.toByteArray());

            if (attachmentInfo.isAlreadyExisting()) {
                return Response.status(Status.ACCEPTED).entity(attachmentInfo.getAttachment()).build();
            } else {
                return Response.created(Utils.createURI(uriInfo.getBaseUri(), AttachmentResource.class, wikiName,
                    spaceName, pageName, attachmentName)).entity(attachmentInfo.getAttachment()).build();
            }
        } catch (Exception e) {
            throw new XWikiRestException(e);
        }
    }
}
