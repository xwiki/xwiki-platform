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
package org.xwiki.extension.repository.xwiki.internal;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.core.UriBuilder;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.lang.StringUtils;
import org.restlet.data.MediaType;
import org.xwiki.extension.Extension;
import org.xwiki.extension.ExtensionId;
import org.xwiki.extension.ResolveException;
import org.xwiki.extension.repository.AbstractExtensionRepository;
import org.xwiki.extension.repository.ExtensionRepositoryId;
import org.xwiki.extension.repository.SearchException;
import org.xwiki.extension.repository.Searchable;
import org.xwiki.extension.repository.xwiki.model.jaxb.Extensions;

/**
 * @version $Id$
 */
public class XWikiExtensionRepository extends AbstractExtensionRepository implements Searchable
{
    private XWikiExtensionRepositoryFactory repositoryFactory;

    private final UriBuilder extensionUriBuider;

    private final UriBuilder extensionFileUriBuider;

    private final UriBuilder simplesearchUriBuider;

    public XWikiExtensionRepository(ExtensionRepositoryId repositoryId,
        XWikiExtensionRepositoryFactory repositoryFactory) throws Exception
    {
        super(repositoryId.getURI().getPath().endsWith("/") ? new ExtensionRepositoryId(repositoryId.getId(),
            repositoryId.getType(), new URI(StringUtils.chop(repositoryId.getURI().toString()))) : repositoryId);

        this.repositoryFactory = repositoryFactory;

        // Uri builders
        this.extensionUriBuider = createUriBuilder("/extension/{extensionId}/{extensionVersion}");
        this.extensionFileUriBuider = createUriBuilder("/extension/{extensionId}/{extensionVersion}/file");
        this.simplesearchUriBuider = createUriBuilder("/extensions/search/simple/{pattern}");
    }

    public UriBuilder getExtensionFileUriBuider()
    {
        return this.extensionFileUriBuider;
    }

    public InputStream getRESTResourceAsStream(UriBuilder builder, Object... values) throws ResolveException,
        IOException
    {
        String url;
        try {
            url = builder.build(values).toString();
        } catch (Exception e) {
            throw new ResolveException("Failed to build REST URL", e);
        }

        HttpClient httpClient = createClient();

        GetMethod getMethod = new GetMethod(url.toString());
        getMethod.addRequestHeader("Accept", MediaType.APPLICATION_XML.toString());
        try {
            httpClient.executeMethod(getMethod);
        } catch (Exception e) {
            throw new ResolveException("Failed to request [" + getMethod.getURI() + "]", e);
        }

        if (getMethod.getStatusCode() != HttpStatus.SC_OK) {
            throw new ResolveException("Invalid answer (" + getMethod.getStatusCode()
                + ") fo the server when requesting");
        }

        return getMethod.getResponseBodyAsStream();
    }

    // ExtensionRepository

    public Extension resolve(ExtensionId extensionId) throws ResolveException
    {
        try {
            return new XWikiExtension(
                this,
                (org.xwiki.extension.repository.xwiki.model.jaxb.Extension) this.repositoryFactory
                    .getUnmarshaller()
                    .unmarshal(
                        getRESTResourceAsStream(this.extensionUriBuider, extensionId.getId(), extensionId.getVersion())));
        } catch (Exception e) {
            throw new ResolveException("Failed to create extension object for extension [" + extensionId + "]", e);
        }
    }

    private HttpClient createClient()
    {
        HttpClient httpClient = new HttpClient();

        return httpClient;
    }

    private UriBuilder createUriBuilder(String path)
    {
        return UriBuilder.fromUri(getId().getURI()).path(path);
    }

    public boolean exists(ExtensionId extensionId)
    {
        // TODO
        return false;
    }

    // Searchable

    public List<Extension> search(String pattern, int offset, int nb) throws SearchException
    {
        Extensions restExtensions;
        try {
            restExtensions =
                (Extensions) this.repositoryFactory.getUnmarshaller().unmarshal(
                    getRESTResourceAsStream(this.simplesearchUriBuider, pattern));
        } catch (Exception e) {
            throw new SearchException("Failed to search extensions based on pattern [" + pattern + "]", e);
        }

        List<Extension> extensions = new ArrayList<Extension>(restExtensions.getExtensions().size());
        for (org.xwiki.extension.repository.xwiki.model.jaxb.Extension restExtension : restExtensions.getExtensions()) {
            extensions.add(new XWikiExtension(this, restExtension));
        }

        return extensions;
    }
}
