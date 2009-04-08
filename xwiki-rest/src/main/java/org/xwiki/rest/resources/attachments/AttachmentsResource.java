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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.List;

import javax.mail.BodyPart;
import javax.mail.Header;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.Response.Status;

import org.xwiki.rest.DomainObjectFactory;
import org.xwiki.rest.RangeIterable;
import org.xwiki.rest.XWikiResource;
import org.xwiki.rest.model.jaxb.Attachment;
import org.xwiki.rest.model.jaxb.Attachments;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.api.Document;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;

/**
 * @version $Id$
 */
@Path("/wikis/{wikiName}/spaces/{spaceName}/pages/{pageName}/attachments")
public class AttachmentsResource extends XWikiResource
{
    private static String FORM_FILENAME_FIELD = "filename";

    @GET
    public Attachments getAttachments(@PathParam("wikiName") String wikiName, @PathParam("spaceName") String spaceName,
        @PathParam("pageName") String pageName, @QueryParam("start") @DefaultValue("0") Integer start,
        @QueryParam("number") @DefaultValue("-1") Integer number) throws XWikiException
    {
        DocumentInfo documentInfo = getDocumentInfo(wikiName, spaceName, pageName, null, null, true, false);
        Document doc = documentInfo.getDocument();

        Attachments attachments = objectFactory.createAttachments();

        List<com.xpn.xwiki.api.Attachment> xwikiAttachments = doc.getAttachmentList();

        RangeIterable<com.xpn.xwiki.api.Attachment> ri =
            new RangeIterable<com.xpn.xwiki.api.Attachment>(xwikiAttachments, start, number);

        /*
         * We need to retrieve the base XWiki documents because Document doesn't have a method for retrieving the
         * external URL for an attachment
         */
        XWikiDocument xwikiDocument = xwiki.getDocument(doc.getPrefixedFullName(), xwikiContext);

        for (com.xpn.xwiki.api.Attachment xwikiAttachment : ri) {
            String attachmentXWikiAbsoluteUrl =
                xwikiDocument.getExternalAttachmentURL(xwikiAttachment.getFilename(), "download", xwikiContext)
                    .toString();

            String attachmentXWikiRelativeUrl =
                xwikiDocument.getAttachmentURL(xwikiAttachment.getFilename(), "download", xwikiContext).toString();

            attachments.getAttachments().add(
                DomainObjectFactory.createAttachment(objectFactory, uriInfo.getBaseUri(), xwikiAttachment,
                    attachmentXWikiRelativeUrl, attachmentXWikiAbsoluteUrl));
        }

        return attachments;
    }

    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response addAttachment(@PathParam("wikiName") String wikiName, @PathParam("spaceName") String spaceName,
        @PathParam("pageName") String pageName, Multipart multipart) throws MessagingException, IOException,
        XWikiException
    {
        DocumentInfo documentInfo = getDocumentInfo(wikiName, spaceName, pageName, null, null, true, true);

        Document doc = documentInfo.getDocument();

        if (!doc.hasAccessLevel("edit", xwikiUser)) {
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
            Enumeration e = bodyPart.getMatchingHeaders(new String[] {"Content-disposition"});
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

        /* Clear the fileName */
        attachmentName = xwikiContext.getWiki().clearName(attachmentName, false, true, xwikiContext);

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
        boolean existed = false;

        XWikiDocument xwikiDocument = xwiki.getDocument(doc.getPrefixedFullName(), xwikiContext);
        XWikiAttachment xwikiAttachment = xwikiDocument.getAttachment(attachmentName);
        if (xwikiAttachment == null) {
            xwikiAttachment = new XWikiAttachment();
            xwikiDocument.getAttachmentList().add(xwikiAttachment);
        } else {
            existed = true;
        }

        xwikiAttachment.setContent(baos.toByteArray());
        xwikiAttachment.setAuthor(xwikiUser);
        xwikiAttachment.setFilename(attachmentName);
        xwikiAttachment.setDoc(xwikiDocument);

        xwikiDocument.saveAttachmentContent(xwikiAttachment, xwikiContext);

        doc.save();

        /*
         * We need to retrieve the base XWiki documents because Document doesn't have a method for retrieving the
         * external URL for an attachment
         */
        xwikiDocument = xwiki.getDocument(doc.getPrefixedFullName(), xwikiContext);
        String attachmentXWikiAbsoluteUrl =
            xwikiDocument.getExternalAttachmentURL(attachmentName, "download", xwikiContext).toString();

        String attachmentXWikiRelativeUrl =
            xwikiDocument.getAttachmentURL(attachmentName, "download", xwikiContext).toString();

        Attachment attachment =
            DomainObjectFactory.createAttachment(objectFactory, uriInfo.getBaseUri(), new com.xpn.xwiki.api.Attachment(
                doc, xwikiAttachment, xwikiContext), attachmentXWikiRelativeUrl, attachmentXWikiAbsoluteUrl);

        if (existed) {
            return Response.status(Status.ACCEPTED).entity(attachment).build();
        } else {
            return Response.created(UriBuilder.fromUri(uriInfo.getBaseUri()).path(AttachmentResource.class).build(wikiName, spaceName, pageName, attachmentName)).entity(attachment).build();
        }
    }

}
