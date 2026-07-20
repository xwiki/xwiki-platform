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
package org.xwiki.repository.internal.resources;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.StreamingOutput;

import org.apache.commons.io.IOUtils;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.util.Timeout;
import org.xwiki.component.annotation.Component;
import org.xwiki.extension.Extension;
import org.xwiki.extension.ExtensionFile;
import org.xwiki.extension.ExtensionId;
import org.xwiki.extension.ResolveException;
import org.xwiki.extension.repository.DefaultExtensionRepositoryDescriptor;
import org.xwiki.extension.repository.ExtensionRepository;
import org.xwiki.extension.repository.ExtensionRepositoryDescriptor;
import org.xwiki.extension.repository.ExtensionRepositoryFactory;
import org.xwiki.extension.repository.ExtensionRepositoryManager;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.model.reference.AttachmentReferenceResolver;
import org.xwiki.query.QueryException;
import org.xwiki.rendering.listener.reference.ResourceReference;
import org.xwiki.rendering.listener.reference.ResourceType;
import org.xwiki.repository.Resources;
import org.xwiki.repository.internal.RepositoryConfiguration;
import org.xwiki.repository.internal.XWikiRepositoryModel;
import org.xwiki.repository.internal.reference.ExtensionResourceReference;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

/**
 * @version $Id$
 * @since 3.2M3
 */
@Component
@Named("org.xwiki.repository.internal.resources.ExtensionVersionFileRESTResource")
@Path(Resources.EXTENSION_VERSION_FILE)
public class ExtensionVersionFileRESTResource extends AbstractExtensionRESTResource
{
    @Inject
    private AttachmentReferenceResolver<String> attachmentResolver;

    @Inject
    private ExtensionRepositoryManager extensionRepositoryManager;

    @Inject
    private RepositoryConfiguration repositoryConfiguration;

    @GET
    public Response downloadExtension(@PathParam(Resources.PPARAM_EXTENSIONID) String extensionId,
        @PathParam(Resources.PPARAM_EXTENSIONVERSION) String extensionVersion,
        @QueryParam(ExtensionResourceReference.PARAM_REPOSITORYID) String repositoryId,
        @QueryParam(ExtensionResourceReference.PARAM_REPOSITORYTYPE) String repositoryType,
        @QueryParam(ExtensionResourceReference.PARAM_REPOSITORYURI) String repositoryURI)
        throws XWikiException, QueryException, URISyntaxException, IOException, ResolveException
    {
        ResponseBuilder response;

        if (repositoryId != null) {
            response =
                downloadRemoteExtension(new ExtensionResourceReference(extensionId, extensionVersion, repositoryId));
        } else if (repositoryType != null && repositoryURI != null
            && this.repositoryConfiguration.isAllowedCustomRepository()) {
            response = downloadRemoteExtension(
                new ExtensionResourceReference(extensionId, extensionVersion, repositoryType, new URI(repositoryURI)));
        } else {
            response = downloadLocalExtension(extensionId, extensionVersion);
        }

        return response.build();
    }

    private ResponseBuilder downloadLocalExtension(String extensionId, String extensionVersion)
        throws ResolveException, IOException, QueryException, XWikiException
    {
        XWikiDocument extensionDocument = getExistingExtensionDocumentById(extensionId);

        checkRights(extensionDocument);

        ResourceReference resourceReference =
            this.repositoryManager.getDownloadReference(extensionDocument, extensionVersion);

        ResponseBuilder response = null;

        if (ResourceType.ATTACHMENT.equals(resourceReference.getType())) {
            // It's an attachment
            AttachmentReference attachmentReference = this.attachmentResolver.resolve(resourceReference.getReference(),
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

            RequestConfig requestConfig = RequestConfig.custom()
                .setResponseTimeout(Timeout.ofSeconds(60))
                .setConnectTimeout(Timeout.ofSeconds(10))
                .build();

            // useSystemProperties() makes the client honor the JVM proxy settings (http.proxyHost, etc.),
            // which is why no explicit route planner is needed. A custom User-Agent is still required because
            // some upstream servers (e.g. nexus.xwiki.org) reject requests that don't set one.
            final CloseableHttpClient httpClient = HttpClients.custom()
                .useSystemProperties()
                .setUserAgent("XWikiExtensionRepository")
                .setDefaultRequestConfig(requestConfig)
                .build();

            // Execute eagerly so the upstream status and content type are available when building the JAX-RS
            // response, then stream the body back through a StreamingOutput without buffering it in memory. The
            // client and response are handed over to the StreamingOutput, which closes both once the body has been
            // fully written. If anything fails before that handoff, the finally block releases them.
            boolean handoffSucceeded = false;
            ClassicHttpResponse subResponse = null;
            try {
                HttpGet getMethod = new HttpGet(url.toString());

                try {
                    subResponse = httpClient.executeOpen(null, getMethod, null);
                } catch (Exception e) {
                    throw new IOException("Failed to request [" + url + "]", e);
                }

                response = Response.status(subResponse.getCode());

                HttpEntity entity = subResponse.getEntity();
                String contentTypeStr = entity != null ? entity.getContentType() : null;
                MediaType type = contentTypeStr != null ? MediaType.valueOf(contentTypeStr)
                    : MediaType.APPLICATION_OCTET_STREAM_TYPE;
                response.type(type);

                BaseObject extensionObject = getExtensionObject(extensionDocument);
                String extensionType =
                    this.extensionStore.getValue(extensionObject, XWikiRepositoryModel.PROP_EXTENSION_TYPE);

                final ClassicHttpResponse responseToStream = subResponse;
                StreamingOutput content = output -> {
                    try (httpClient; responseToStream) {
                        if (entity != null) {
                            entity.writeTo(output);
                        }
                    }
                };

                response.entity(content);
                response.header("Content-Disposition",
                    "attachment; filename=\"" + extensionId + '-' + extensionVersion + '.' + extensionType + "\"");

                handoffSucceeded = true;
            } finally {
                if (!handoffSucceeded) {
                    IOUtils.closeQuietly(subResponse, httpClient);
                }
            }
        } else if (ExtensionResourceReference.TYPE.equals(resourceReference.getType())) {
            ExtensionResourceReference extensionResource;
            if (resourceReference instanceof ExtensionResourceReference) {
                extensionResource = (ExtensionResourceReference) resourceReference;
            } else {
                extensionResource = new ExtensionResourceReference(resourceReference.getReference());
            }

            response = downloadRemoteExtension(extensionResource);
        } else {
            throw new WebApplicationException(Status.NOT_FOUND);
        }

        return response;
    }

    private ResponseBuilder downloadRemoteExtension(ExtensionResourceReference extensionResource)
        throws ResolveException, IOException
    {
        ExtensionRepository repository = null;
        if (extensionResource.getRepositoryId() != null) {
            repository = this.extensionRepositoryManager.getRepository(extensionResource.getRepositoryId());
        }

        if (repository == null && extensionResource.getRepositoryType() != null
            && extensionResource.getRepositoryURI() != null) {
            ExtensionRepositoryDescriptor repositoryDescriptor = new DefaultExtensionRepositoryDescriptor("tmp",
                extensionResource.getRepositoryType(), extensionResource.getRepositoryURI());
            try {
                ExtensionRepositoryFactory repositoryFactory =
                    this.componentManager.getInstance(ExtensionRepositoryFactory.class, repositoryDescriptor.getType());

                repository = repositoryFactory.createRepository(repositoryDescriptor);
            } catch (Exception e) {
                // Ignore invalid repository
                getLogger().warn("Invalid repository in download link [{}]", extensionResource);
            }

        }

        // Resolve extension
        Extension downloadExtension;
        if (repository == null) {
            downloadExtension = this.extensionRepositoryManager
                .resolve(new ExtensionId(extensionResource.getExtensionId(), extensionResource.getExtensionVersion()));
        } else {
            downloadExtension = repository
                .resolve(new ExtensionId(extensionResource.getExtensionId(), extensionResource.getExtensionVersion()));
        }

        // Get file
        ExtensionFile extensionFile = downloadExtension.getFile();

        // TODO: indicate a more accurate media type (probably need to add the concept in ExtensionFile)
        ResponseBuilder response = Response.ok(extensionFile.openStream(), MediaType.WILDCARD_TYPE);
        response.header("Content-Disposition", "attachment; filename=\"" + downloadExtension.getId().toString() + '.'
            + downloadExtension.getType() + "\"");

        return response;
    }
}
