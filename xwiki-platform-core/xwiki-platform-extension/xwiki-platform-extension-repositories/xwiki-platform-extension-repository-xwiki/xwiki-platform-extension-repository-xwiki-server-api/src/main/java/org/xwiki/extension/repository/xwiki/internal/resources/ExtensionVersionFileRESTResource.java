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
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;

import org.restlet.data.MediaType;
import org.restlet.representation.InputRepresentation;
import org.xwiki.component.annotation.Component;
import org.xwiki.extension.Extension;
import org.xwiki.extension.ExtensionFile;
import org.xwiki.extension.ExtensionId;
import org.xwiki.extension.ResolveException;
import org.xwiki.extension.internal.reference.ExtensionResourceReference;
import org.xwiki.extension.repository.ExtensionRepository;
import org.xwiki.extension.repository.ExtensionRepositoryFactory;
import org.xwiki.extension.repository.ExtensionRepositoryId;
import org.xwiki.extension.repository.ExtensionRepositoryManager;
import org.xwiki.extension.repository.xwiki.Resources;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.model.reference.AttachmentReferenceResolver;
import org.xwiki.query.QueryException;
import org.xwiki.rendering.listener.reference.ResourceReference;
import org.xwiki.rendering.listener.reference.ResourceType;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

/**
 * @version $Id$
 * @since 3.2M3
 */
@Component("org.xwiki.extension.repository.xwiki.internal.resources.ExtensionVersionFileRESTResource")
@Path(Resources.EXTENSION_VERSION_FILE)
public class ExtensionVersionFileRESTResource extends AbstractExtensionRESTResource
{
    @Inject
    private AttachmentReferenceResolver<String> attachmentResolver;

    @Inject
    private ExtensionRepositoryManager extensionRepositoryManager;

    @GET
    public Response downloadExtension(@PathParam(Resources.PPARAM_EXTENSIONID) String extensionId,
        @PathParam(Resources.PPARAM_EXTENSIONVERSION) String extensionVersion) throws XWikiException, QueryException,
        URISyntaxException, IOException, ResolveException
    {
        XWikiDocument extensionDocument = getExistingExtensionDocumentById(extensionId);

        checkRights(extensionDocument);

        BaseObject extensionVersionObject = getExtensionVersionObject(extensionDocument, extensionVersion);

        ResponseBuilder response = null;

        ResourceReference resourceReference =
            this.repositoryManager.getDownloadReference(extensionDocument, extensionVersionObject);

        if (ResourceType.ATTACHMENT.equals(resourceReference.getType())) {
            // It's an attachment
            AttachmentReference attachmentReference =
                this.attachmentResolver.resolve(resourceReference.getReference(),
                    extensionDocument.getDocumentReference());

            XWikiContext xcontext = getXWikiContext();

            XWikiDocument document =
                xcontext.getWiki().getDocument(attachmentReference.getDocumentReference(), xcontext);

            checkRights(document);

            XWikiAttachment xwikiAttachment = document.getAttachment(attachmentReference.getName());

            response = getAttachmentResponse(xwikiAttachment);
        } else if (ResourceType.URL.equals(resourceReference.getType())) {
            // It's an URL
            URL url = new URL(resourceReference.getReference());

            // TODO: find a proper way to do a perfect proxy of the URL without directly using Restlet classes.
            // Should probably use javax.ws.rs.ext.MessageBodyWriter

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
        } else if (ExtensionResourceReference.TYPE.equals(resourceReference.getType())) {
            ExtensionResourceReference extensionResource;
            if (resourceReference instanceof ExtensionResourceReference) {
                extensionResource = (ExtensionResourceReference) resourceReference;
            } else {
                extensionResource = new ExtensionResourceReference(resourceReference.getReference());
            }

            ExtensionRepository repository = null;
            if (extensionResource.getRepositoryId() != null) {
                repository = this.extensionRepositoryManager.getRepository(extensionResource.getRepositoryId());
            }

            if (repository == null && extensionResource.getRepositoryType() != null
                && extensionResource.getRepositoryURI() != null) {
                ExtensionRepositoryId repositoryId =
                    new ExtensionRepositoryId("tmp", extensionResource.getRepositoryType(),
                        extensionResource.getRepositoryURI());
                try {
                    ExtensionRepositoryFactory repositoryFactory =
                        this.componentManager.lookup(ExtensionRepositoryFactory.class, repositoryId.getType());

                    repository = repositoryFactory.createRepository(repositoryId);
                } catch (Exception e) {
                    // Ignore invalid repository
                    this.logger.warning("Invalid repository in download link [" + resourceReference + "] in document ["
                        + extensionDocument + "]");
                }

            }

            // Resolve extension
            Extension downloadExtension;
            if (repository == null) {
                downloadExtension =
                    this.extensionRepositoryManager.resolve(new ExtensionId(extensionResource.getExtensionId(),
                        extensionResource.getExtensionVersion()));
            } else {
                downloadExtension =
                    repository.resolve(new ExtensionId(extensionResource.getExtensionId(), extensionResource
                        .getExtensionVersion()));
            }

            // Get file
            // TODO: find media type
            ExtensionFile extensionFile = downloadExtension.getFile();
            long length = extensionFile.getLength();
            InputRepresentation content = new InputRepresentation(extensionFile.openStream(), MediaType.ALL, length);

            response = Response.ok();
            response.entity(content);
        } else {
            throw new WebApplicationException(Status.NOT_FOUND);
        }

        return response.build();
    }
}
