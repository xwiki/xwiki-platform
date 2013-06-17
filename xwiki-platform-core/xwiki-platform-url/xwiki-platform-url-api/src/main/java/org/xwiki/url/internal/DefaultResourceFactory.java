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
import org.xwiki.resource.Resource;
import org.xwiki.resource.ResourceCreationException;
import org.xwiki.resource.ResourceFactory;
import org.xwiki.resource.UnsupportedResourceException;
import org.xwiki.url.ResourceConfiguration;

/**
 * Resource Factory that delegates the work to the Resource Factory specified in the XWiki Configuration
 * (see {@link org.xwiki.url.ResourceConfiguration#getURLFormatId()}.
 *
 * @version $Id$
 * @since 5.2M1
 */
@Component
@Singleton
public class DefaultResourceFactory implements ResourceFactory<URL, Resource>
{
    /**
     * Used to get the hint of the {@link ResourceFactory} to use.
     */
    @Inject
    private ResourceConfiguration configuration;

    /**
     * Used to lookup the correct {@link ResourceFactory} component.
     */
    @Inject
    private ComponentManager componentManager;

    @Override
    public Resource createResource(URL urlRepresentation, Map<String, Object> parameters)
        throws ResourceCreationException, UnsupportedResourceException
    {
        ResourceFactory factory;
        try {
            factory = this.componentManager.getInstance(ResourceFactory.TYPE_URL_RESOURCE,
                this.configuration.getURLFormatId());
        } catch (ComponentLookupException e) {
            throw new ResourceCreationException(
                String.format("Invalid configuration hint [%s]. Cannot create Resource for [%s].",
                    this.configuration.getURLFormatId(), urlRepresentation), e);
        }
        return factory.createResource(urlRepresentation, parameters);
    }
}
