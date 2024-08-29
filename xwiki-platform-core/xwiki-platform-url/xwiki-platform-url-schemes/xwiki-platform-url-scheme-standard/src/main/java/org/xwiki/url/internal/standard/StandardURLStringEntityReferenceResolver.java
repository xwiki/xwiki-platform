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
package org.xwiki.url.internal.standard;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.EntityType;
import org.xwiki.model.internal.reference.DefaultStringEntityReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceResolver;
import org.xwiki.resource.CreateResourceReferenceException;
import org.xwiki.resource.CreateResourceTypeException;
import org.xwiki.resource.ResourceReference;
import org.xwiki.resource.ResourceReferenceResolver;
import org.xwiki.resource.ResourceType;
import org.xwiki.resource.ResourceTypeResolver;
import org.xwiki.resource.UnsupportedResourceReferenceException;
import org.xwiki.resource.entity.EntityResourceReference;
import org.xwiki.url.ExtendedURL;

import com.xpn.xwiki.XWikiContext;

/**
 * Attempts to extract the {@link EntityReference} from a {@link String} URL, using the
 * {@link StandardExtendedURLResourceTypeResolver}.
 * 
 * @version $Id$
 * @since 14.1RC1
 */
@Component
@Named("resource/standardURL")
@Singleton
public class StandardURLStringEntityReferenceResolver extends DefaultStringEntityReferenceResolver
{
    @Inject
    private Logger logger;

    @Inject
    private Provider<XWikiContext> xcontextProvider;

    @Inject
    @Named("standard")
    private ResourceReferenceResolver<ExtendedURL> resourceResolver;

    @Inject
    @Named("standard")
    private ResourceTypeResolver<ExtendedURL> typeResolver;

    @Inject
    private EntityReferenceResolver<String> defaultStringResolver;

    @Override
    public EntityReference resolve(String urlStringRepresentation, EntityType type, Object... parameters)
    {
        XWikiContext xcontext = this.xcontextProvider.get();
        try {
            ExtendedURL extendedURL =
                new ExtendedURL(new URL(urlStringRepresentation), xcontext.getRequest().getContextPath());
            ResourceType resourceType = this.typeResolver.resolve(extendedURL, Collections.emptyMap());
            ResourceReference reference =
                this.resourceResolver.resolve(extendedURL, resourceType, Collections.emptyMap());
            if (reference instanceof EntityResourceReference) {
                EntityReference entityReference = ((EntityResourceReference) reference).getEntityReference();
                return entityReference;
            }
        } catch (CreateResourceReferenceException | UnsupportedResourceReferenceException | CreateResourceTypeException
            | MalformedURLException e) {
            logger.warn("Failed to extract an EntityReference from [{}]. Root cause is [{}].", urlStringRepresentation,
                ExceptionUtils.getRootCauseMessage(e));
        }

        return defaultStringResolver.resolve(urlStringRepresentation, type, parameters);
    }
}
