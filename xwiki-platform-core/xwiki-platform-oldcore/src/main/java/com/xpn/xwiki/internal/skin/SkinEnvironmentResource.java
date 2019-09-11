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

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.inject.Provider;

import org.xwiki.environment.Environment;
import org.xwiki.skin.ResourceRepository;
import org.xwiki.url.URLConfiguration;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.web.Utils;
import com.xpn.xwiki.web.XWikiURLFactory;

/**
 * @version $Id$
 * @since 6.4M1
 */
public class SkinEnvironmentResource extends AbstractEnvironmentResource
{
    protected Provider<XWikiContext> xcontextProvider;

    private URLConfiguration urlConfiguration;

    public SkinEnvironmentResource(String path, String resourceName, ResourceRepository repository,
        Environment environment, Provider<XWikiContext> xcontextProvider, URLConfiguration urlConfiguration)
    {
        super(path, resourceName, repository, environment);

        this.xcontextProvider = xcontextProvider;
        this.urlConfiguration = urlConfiguration;
    }

    private URLConfiguration getURLConfiguration() {
        if (this.urlConfiguration == null) {
            this.urlConfiguration = Utils.getComponent(URLConfiguration.class);
        }

        return this.urlConfiguration;
    }

    @Override
    public String getURL(boolean forceSkinAction) throws Exception
    {
        XWikiContext xcontext = this.xcontextProvider.get();

        Map<String, Object> parameters = new LinkedHashMap<>();
        if (getURLConfiguration().useResourceLastModificationDate()) {
            try {
                URL resourceUrl = this.xcontextProvider.get().getEngineContext().getResource(this.getPath());
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
}
