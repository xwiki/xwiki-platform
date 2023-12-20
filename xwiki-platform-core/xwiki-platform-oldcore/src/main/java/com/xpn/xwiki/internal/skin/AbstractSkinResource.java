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
package com.xpn.xwiki.internal.skin;

import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.inject.Provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.environment.Environment;
import org.xwiki.filter.input.DefaultInputStreamInputSource;
import org.xwiki.filter.input.InputSource;
import org.xwiki.skin.ResourceRepository;
import org.xwiki.url.URLConfiguration;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.internal.template.InternalTemplateManager;
import com.xpn.xwiki.web.Utils;
import com.xpn.xwiki.web.XWikiURLFactory;

/**
 * An abstract skin resource class used by all the skin resources that needs to resolve resources by {@link URL}, and
 * get resources as stream.
 *
 * @version $Id$
 * @since 13.8RC1
 */
public abstract class AbstractSkinResource extends AbstractResource<InputSource>
{
    protected static final Logger LOGGER = LoggerFactory.getLogger(AbstractSkinResource.class);

    protected Environment environment;

    protected Provider<XWikiContext> xcontextProvider;

    protected URLConfiguration urlConfiguration;

    /**
     * Default constructor.
     * 
     * @param id the identifier of the resource
     * @param path the path of the resource in the skin (for instance, {@code "/templates/display.vm"})
     * @param resourceName the name of the resource (for instance, {@code "display.vm"})
     * @param repository the resource repository, used to access the respository's id
     * @param environment the environment used to resolve the resources
     * @param xcontextProvider the context provider, used to access the context when resolving the resources URLs
     * @param urlConfiguration the url configuration, used when resolving the resources URLs
     */
    protected AbstractSkinResource(String id, String path, String resourceName, ResourceRepository repository,
        Environment environment, Provider<XWikiContext> xcontextProvider, URLConfiguration urlConfiguration)
    {
        super(id, path, resourceName, repository);

        this.environment = environment;
        this.xcontextProvider = xcontextProvider;
        this.urlConfiguration = urlConfiguration;
    }

    @Override
    public InputSource getInputSource()
    {
        String path = getPath();
        InputStream inputStream = getResourceAsStream(path);
        if (inputStream != null) {
            return new DefaultInputStreamInputSource(inputStream, true);
        }

        return null;
    }

    @Override
    public Instant getInstant() throws Exception
    {
        return InternalTemplateManager.getResourceInstant(this.environment, getPath());
    }

    @Override
    public String getURL(boolean forceSkinAction) throws Exception
    {
        XWikiContext xcontext = this.xcontextProvider.get();

        Map<String, Object> parameters = new LinkedHashMap<>();
        if (getURLConfiguration().useResourceLastModificationDate()) {
            try {
                URL resourceUrl = this.environment.getResource(this.getPath());
                Path resourcePath = Paths.get(resourceUrl.toURI());
                FileTime lastModifiedTime = Files.getLastModifiedTime(resourcePath);
                parameters.put(XWiki.CACHE_VERSION, String.valueOf(lastModifiedTime.toMillis()));
            } catch (Exception e) {
                parameters.put(XWiki.CACHE_VERSION, xcontext.getWiki().getVersion());
            }
        } else {
            parameters.put(XWiki.CACHE_VERSION, xcontext.getWiki().getVersion());
        }

        XWikiURLFactory urlf = xcontext.getURLFactory();

        URL url;

        if (forceSkinAction) {
            url = urlf.createSkinURL(this.resourceName, "skins", getRepository().getId(), xcontext, parameters);
        } else {
            url = urlf.createSkinURL(this.resourceName, getRepository().getId(), xcontext, parameters);
        }

        return urlf.getURL(url, xcontext);
    }

    private URLConfiguration getURLConfiguration()
    {
        if (this.urlConfiguration == null) {
            this.urlConfiguration = Utils.getComponent(URLConfiguration.class);
        }

        return this.urlConfiguration;
    }

    /**
     * Resolve an {@link InputStream} from a resource path.
     *
     * @param path the resource path
     * @return the resolved input stream
     */
    public abstract InputStream getResourceAsStream(String path);
}
