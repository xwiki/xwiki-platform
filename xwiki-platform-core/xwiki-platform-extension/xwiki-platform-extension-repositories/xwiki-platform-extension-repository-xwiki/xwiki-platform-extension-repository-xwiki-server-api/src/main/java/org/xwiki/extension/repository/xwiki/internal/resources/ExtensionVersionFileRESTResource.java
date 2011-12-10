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
package org.xwiki.extension.repository.xwiki.internal.resources;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.lang3.StringUtils;
import org.restlet.data.MediaType;
import org.restlet.resource.InputRepresentation;
import org.xwiki.component.annotation.Component;
import org.xwiki.extension.repository.xwiki.Resources;
import org.xwiki.extension.repository.xwiki.internal.XWikiRepositoryModel;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.model.reference.AttachmentReferenceResolver;
import org.xwiki.query.QueryException;
import org.xwiki.rendering.listener.reference.ResourceReference;
import org.xwiki.rendering.listener.reference.ResourceType;
import org.xwiki.rendering.parser.ResourceReferenceParser;
import org.xwiki.rest.Utils;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.api.Attachment;
import com.xpn.xwiki.api.Document;

/**
 * @version $Id$
 * @since 3.2M3
 */
@Component("org.xwiki.extension.repository.xwiki.internal.resources.ExtensionVersionFileRESTResource")
@Path(Resources.EXTENSION_VERSION_FILE)
public class ExtensionVersionFileRESTResource extends AbstractExtensionRESTResource
{
    @Inject
    @Named("link")
    private ResourceReferenceParser resourceReferenceParser;

    @Inject
    private AttachmentReferenceResolver<String> attachmentResolver;

    @GET
    public Response downloadExtension(@PathParam(Resources.PPARAM_EXTENSIONID) String extensionId,
        @PathParam(Resources.PPARAM_EXTENSIONVERSION) String extensionVersion) throws XWikiException, QueryException,
        URISyntaxException, IOException
    {
        Document extensionDocument = getExtensionDocument(extensionId);

        if (extensionDocument.isNew()) {
            throw new WebApplicationException(Status.NOT_FOUND);
        }

        com.xpn.xwiki.api.Object extensionObject = getExtensionObject(extensionDocument);
        com.xpn.xwiki.api.Object extensionVersionObject =
            getExtensionVersionObject(extensionDocument, extensionVersion);

        ResponseBuilder response = null;

        String download = (String) getValue(extensionVersionObject, XWikiRepositoryModel.PROP_VERSION_DOWNLOAD);

        if (StringUtils.isNotEmpty(download)) {
            // User explicitly indicated a download location
            ResourceReference resourceReference = this.resourceReferenceParser.parse(download);

            if (ResourceType.ATTACHMENT.equals(resourceReference.getType())) {
                // It's an attachment
                AttachmentReference attachmentReference =
                    this.attachmentResolver.resolve(resourceReference.getReference(),
                        extensionDocument.getDocumentReference());

                Document document =
                    Utils.getXWikiApi(this.componentManager).getDocument(attachmentReference.getDocumentReference());

                Attachment xwikiAttachment = document.getAttachment(attachmentReference.getName());

                response = getAttachmentResponse(xwikiAttachment);
            } else if (ResourceType.URL.equals(resourceReference.getType())) {
                // It's an URL
                URL url = new URL(resourceReference.getReference());

                // TODO: find a proper way to do a perfect proxy of the URL without directly using Restlet classes.
                // It's a real pain since Restlet does not properly support some standard headers (like the charset in
                // the content-type for example) when given as it is in the Response

                URLConnection connection = url.openConnection();

                if (connection instanceof HttpURLConnection) {
                    HttpURLConnection httpConnection = (HttpURLConnection) connection;
                    response = Response.status(httpConnection.getResponseCode());
                } else {
                    response = Response.ok();
                }

                InputRepresentation content =
                    new InputRepresentation(connection.getInputStream(), new MediaType(connection.getContentType()),
                        connection.getContentLength());
                response.entity(content);
            } else {
                throw new WebApplicationException(Status.NOT_FOUND);
            }
        } else {
            // Fallback on standard named attachment
            Attachment xwikiAttachment =
                extensionDocument.getAttachment(extensionId + "-" + extensionVersion + "."
                    + getValue(extensionObject, XWikiRepositoryModel.PROP_EXTENSION_TYPE));

            response = getAttachmentResponse(xwikiAttachment);
        }

        return response.build();
    }
}
