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
import org.apache.commons.lang3.StringUtils;
import org.restlet.data.MediaType;
import org.xwiki.extension.Extension;
import org.xwiki.extension.ExtensionId;
import org.xwiki.extension.ExtensionLicenseManager;
import org.xwiki.extension.ResolveException;
import org.xwiki.extension.repository.AbstractExtensionRepository;
import org.xwiki.extension.repository.ExtensionRepositoryId;
import org.xwiki.extension.repository.SearchException;
import org.xwiki.extension.repository.Searchable;
import org.xwiki.extension.repository.xwiki.Resources;
import org.xwiki.extension.repository.xwiki.model.jaxb.ExtensionVersion;
import org.xwiki.extension.repository.xwiki.model.jaxb.ExtensionsSearchResult;

/**
 * @version $Id$
 */
public class XWikiExtensionRepository extends AbstractExtensionRepository implements Searchable
{
    private final XWikiExtensionRepositoryFactory repositoryFactory;

    private final ExtensionLicenseManager licenseManager;

    private final UriBuilder extensionVersionUriBuider;

    private final UriBuilder extensionVersionFileUriBuider;

    private final UriBuilder searchUriBuider;

    public XWikiExtensionRepository(ExtensionRepositoryId repositoryId,
        XWikiExtensionRepositoryFactory repositoryFactory, ExtensionLicenseManager licenseManager) throws Exception
    {
        super(repositoryId.getURI().getPath().endsWith("/") ? new ExtensionRepositoryId(repositoryId.getId(),
            repositoryId.getType(), new URI(StringUtils.chop(repositoryId.getURI().toString()))) : repositoryId);

        this.repositoryFactory = repositoryFactory;
        this.licenseManager = licenseManager;

        // Uri builders
        this.extensionVersionUriBuider = createUriBuilder(Resources.EXTENSION_VERSION);
        this.extensionVersionFileUriBuider = createUriBuilder(Resources.EXTENSION_VERSION_FILE);
        this.searchUriBuider = createUriBuilder(Resources.SEARCH);
    }

    public UriBuilder getExtensionFileUriBuider()
    {
        return this.extensionVersionFileUriBuider;
    }

    public GetMethod getRESTResource(UriBuilder builder, Object... values) throws IOException, IOException
    {
        String url;
        try {
            url = builder.build(values).toString();
        } catch (Exception e) {
            throw new IOException("Failed to build REST URL", e);
        }

        HttpClient httpClient = createClient();

        GetMethod getMethod = new GetMethod(url.toString());
        getMethod.addRequestHeader("Accept", MediaType.APPLICATION_XML.toString());
        try {
            httpClient.executeMethod(getMethod);
        } catch (Exception e) {
            throw new IOException("Failed to request [" + getMethod.getURI() + "]", e);
        }

        if (getMethod.getStatusCode() != HttpStatus.SC_OK) {
            throw new IOException("Invalid answer (" + getMethod.getStatusCode() + ") fo the server when requesting");
        }

        return getMethod;
    }

    public InputStream getRESTResourceAsStream(UriBuilder builder, Object... values) throws IOException, IOException
    {
        return getRESTResource(builder, values).getResponseBodyAsStream();
    }

    // ExtensionRepository

    @Override
    public Extension resolve(ExtensionId extensionId) throws ResolveException
    {
        try {
            return new XWikiExtension(this,
                (ExtensionVersion) this.repositoryFactory.getUnmarshaller().unmarshal(
                    getRESTResourceAsStream(this.extensionVersionUriBuider, extensionId.getId(),
                        extensionId.getVersion())), this.licenseManager);
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

    // Searchable

    @Override
    public List<Extension> search(String pattern, int offset, int nb) throws SearchException
    {
        UriBuilder builder = this.searchUriBuider.clone();

        builder.queryParam(Resources.QPARAM_LIST_START, offset);
        builder.queryParam(Resources.QPARAM_LIST_NUMBER, nb);

        ExtensionsSearchResult restExtensions;
        try {
            restExtensions =
                (ExtensionsSearchResult) this.repositoryFactory.getUnmarshaller().unmarshal(
                    getRESTResourceAsStream(builder, pattern));
        } catch (Exception e) {
            throw new SearchException("Failed to search extensions based on pattern [" + pattern + "]", e);
        }

        List<Extension> extensions = new ArrayList<Extension>(restExtensions.getExtensions().size());
        for (ExtensionVersion restExtension : restExtensions.getExtensions()) {
            extensions.add(new XWikiExtension(this, restExtension, this.licenseManager));
        }

        return extensions;
    }
}
