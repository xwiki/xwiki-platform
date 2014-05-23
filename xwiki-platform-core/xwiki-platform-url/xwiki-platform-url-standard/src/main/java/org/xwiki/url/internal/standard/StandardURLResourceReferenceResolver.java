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

import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.util.DefaultParameterizedType;
import org.xwiki.resource.CreateResourceReferenceException;
import org.xwiki.resource.ResourceReference;
import org.xwiki.resource.ResourceReferenceResolver;
import org.xwiki.resource.UnsupportedResourceReferenceException;
import org.xwiki.url.ExtendedURL;

import java.net.URL;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

/**
 * Parses a URL written in the "standard" format and generate a {@link org.xwiki.resource.ResourceReference} out of it.
 *
 * @version $Id$
 * @since 6.1M2
 */
@Component
@Named("standard")
@Singleton
public class StandardURLResourceReferenceResolver implements ResourceReferenceResolver<URL>
{
    /**
     * @see #resolve(java.net.URL, java.util.Map)
     */
    private static final String IGNORE_PREFIX_KEY = "ignorePrefix";

    @Inject
    @Named("context")
    private ComponentManager componentManager;

    /**
     * Used to know if the wiki is in path-based configuration or not.
     */
    @Inject
    private StandardURLConfiguration configuration;

    /**
     * {@inheritDoc}
     *
     * <p/>
     * Supported parameters:
     * <ul>
     *   <li>"ignorePrefix": the starting part of the URL Path (i.e. after the Authority part) to ignore. This is
     *       useful for example for passing the Web Application Context (for a web app) which should be ignored.
     *       Example: "/xwiki".</li> 
     * </ul>
     *
     * @see org.xwiki.resource.ResourceReferenceResolver#resolve(Object, java.util.Map)
     */
    @Override
    public ResourceReference resolve(URL url, Map<String, Object> parameters)
        throws CreateResourceReferenceException, UnsupportedResourceReferenceException
    {
        // Step 1: Use an Extended URL in to get access to the URL path segments.
        // Note that we also remove the passed ignore prefix from the segments if it has been specified.
        // The reason is because we need to ignore the Servlet Context if this code is called in a Servlet
        // environment and since the XWiki Application can be installed in the ROOT context, as well as in any Context
        // there's no way we can guess this, and thus it needs to be passed.
        String ignorePrefix = (String) parameters.get(IGNORE_PREFIX_KEY);
        ExtendedURL extendedURL = new ExtendedURL(url, ignorePrefix);

        // Step 2: Find the right Resolver for the passed Resource type and call it.
        ResourceReferenceResolver<ExtendedURL> resolver;

        // Find the URL Factory for the type, which is the first segment in the ExtendedURL
        String type = extendedURL.getSegments().get(0);

        // First, try to locate a URL Resolver registered only for this URL scheme
        try {
            resolver = this.componentManager.getInstance(
                new DefaultParameterizedType(null, ResourceReferenceResolver.class,
                    ExtendedURL.class), String.format("standard/%s", type));
        } catch (ComponentLookupException e) {
            // Second, if not found, try to locate a URL Resolver registered for all URL schemes
            try {
                resolver = this.componentManager.getInstance(
                    new DefaultParameterizedType(null, ResourceReferenceResolver.class, ExtendedURL.class), type);
            } catch (ComponentLookupException cle) {
                throw new UnsupportedResourceReferenceException(
                    String.format("Failed to find a Resolver for Resource Reference of type [%s] for URL [%s]", type,
                        url), e);
            }
        }

        return resolver.resolve(extendedURL, parameters);
    }
}
