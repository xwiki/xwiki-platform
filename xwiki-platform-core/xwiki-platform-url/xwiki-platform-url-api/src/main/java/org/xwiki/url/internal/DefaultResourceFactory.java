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
package org.xwiki.url.internal;

import java.net.URL;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.url.ResourceFactory;
import org.xwiki.url.URLConfiguration;
import org.xwiki.url.URLCreationException;
import org.xwiki.url.UnsupportedURLException;
import org.xwiki.url.Resource;

/**
 * URL Factory that delegates the work to the URL Factory specified in the XWiki Configuration
 * (see {@link org.xwiki.url.URLConfiguration#getURLFormatId()}.
 *
 * @version $Id$
 * @since 3.0M3
 */
@Component
@Singleton
public class DefaultResourceFactory implements ResourceFactory<URL, Resource>
{
    /**
     * Used to get the hint of the {@link org.xwiki.url.ResourceFactory} to use.
     */
    @Inject
    private URLConfiguration configuration;

    /**
     * Used to lookup the correct {@link org.xwiki.url.ResourceFactory} component.
     */
    @Inject
    private ComponentManager componentManager;

    @Override
    public Resource createURL(URL urlRepresentation, Map<String, Object> parameters)
        throws URLCreationException, UnsupportedURLException
    {
        ResourceFactory factory;
        try {
            factory = this.componentManager.getInstance(ResourceFactory.TYPE_URL_XWIKIURL,
                this.configuration.getURLFormatId());
        } catch (ComponentLookupException e) {
            throw new URLCreationException(String.format("Invalid configuration hint [%s]. Cannot create XWiki URL.",
                this.configuration.getURLFormatId()), e);
        }
        return factory.createURL(urlRepresentation, parameters);
    }
}
