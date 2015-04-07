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
import org.xwiki.component.descriptor.ComponentInstantiationStrategy;
import org.xwiki.component.descriptor.DefaultComponentDependency;
import org.xwiki.component.descriptor.DefaultComponentDescriptor;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.manager.ComponentRepositoryException;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.component.util.DefaultParameterizedType;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceResolver;
import org.xwiki.resource.CreateResourceReferenceException;
import org.xwiki.resource.ResourceReference;
import org.xwiki.resource.ResourceReferenceResolver;
import org.xwiki.resource.UnsupportedResourceReferenceException;
import org.xwiki.url.ExtendedURL;
import org.xwiki.url.internal.standard.entity.BinEntityResourceReferenceResolver;
import org.xwiki.url.internal.standard.entity.WikiEntityResourceReferenceResolver;

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
public class StandardURLResourceReferenceResolver implements ResourceReferenceResolver<URL>, Initializable
{
    /**
     * @see #resolve(java.net.URL, java.util.Map)
     */
    private static final String IGNORE_PREFIX_KEY = "ignorePrefix";

    @Inject
    @Named("context")
    private ComponentManager componentManager;

    @Inject
    private ComponentManager rootComponentManager;

    /**
     * Used to know if the wiki is in path-based configuration or not.
     */
    @Inject
    private StandardURLConfiguration configuration;

    private ResourceReferenceResolver<ExtendedURL> binEntityResourceReferenceResolver;

    @Override
    public void initialize() throws InitializationException
    {
        // Step 1: Dynamically register a WikiEntityResourceReferenceResolver since the user can choose the Resource
        // type name when specifying an Entity Resource Reference in path-based multiwiki, see
        // StandardURLConfiguration#getWikiPathPrefix()
        registerEntityResourceReferenceResolver(this.configuration.getWikiPathPrefix(),
            WikiEntityResourceReferenceResolver.class, "path");

        // Step 2: Dynamically register a BinEntityResourceReferenceResolver since the user can choose the Resource
        // type name for Entities, see StandardURLConfiguration#getEntityPathPrefix()
        registerEntityResourceReferenceResolver(this.configuration.getEntityPathPrefix(),
            BinEntityResourceReferenceResolver.class, "domain");

        // Step 3: Cache the registered BinEntityResourceReferenceResolver for later use below
        try {
            this.binEntityResourceReferenceResolver = this.rootComponentManager.getInstance(
                new DefaultParameterizedType(null, ResourceReferenceResolver.class, ExtendedURL.class),
                computeHint(this.configuration.getEntityPathPrefix()));
        } catch (ComponentLookupException e) {
            throw new InitializationException("Failed to initialize Standard Resource Reference Resolver", e);
        }
    }

    private void registerEntityResourceReferenceResolver(String registrationHint,
        Class<? extends ResourceReferenceResolver<ExtendedURL>> registrationImplementation,
        String wikiExtractorHint) throws InitializationException
    {
        DefaultComponentDescriptor<ResourceReferenceResolver<ExtendedURL>> resolverDescriptor =
            new DefaultComponentDescriptor<>();
        resolverDescriptor.setImplementation(registrationImplementation);
        resolverDescriptor.setInstantiationStrategy(ComponentInstantiationStrategy.SINGLETON);
        String hint = computeHint(registrationHint);
        resolverDescriptor.setRoleHint(hint);
        resolverDescriptor.setRoleType(
            new DefaultParameterizedType(null, ResourceReferenceResolver.class, ExtendedURL.class));
        // Register dependencies
        DefaultComponentDependency<WikiReferenceExtractor> wikiReferenceExtractorDependency =
            new DefaultComponentDependency<>();
        wikiReferenceExtractorDependency.setRoleType(WikiReferenceExtractor.class);
        wikiReferenceExtractorDependency.setRoleHint(wikiExtractorHint);
        wikiReferenceExtractorDependency.setName("wikiExtractor");
        resolverDescriptor.addComponentDependency(wikiReferenceExtractorDependency);
        DefaultComponentDependency<EntityReferenceResolver<EntityReference>> entityReferenceResolverDependency =
            new DefaultComponentDependency<>();
        entityReferenceResolverDependency.setRoleType(new DefaultParameterizedType(null, EntityReferenceResolver.class,
            EntityReference.class));
        entityReferenceResolverDependency.setName("defaultReferenceEntityReferenceResolver");
        resolverDescriptor.addComponentDependency(entityReferenceResolverDependency);

        try {
            this.rootComponentManager.registerComponent(resolverDescriptor);
        } catch (ComponentRepositoryException e) {
            throw new InitializationException(String.format(
                "Failed to dynamically register Resource Reference Resolver for hint [%s]", hint), e);
        }
    }

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

        // Find the URL Factory for the type, which is the first segment in the ExtendedURL.
        //
        // Note that we need to remove it from the ExtendedURL instance since it's passed to the specific resolvers
        // and they shouldn't be aware of where it was located since they need to be able to resolve the rest of the
        // URL independently of the URL scheme, in case they wish to have a single URL syntax for all URL schemes.
        // Example:
        // - scheme 1: /<type>/something
        // - scheme 2: /something?type=<type>
        // The specific resolver for type <type> needs to be passed an ExtendedURL independent of the type, in this
        // case: /something
        //
        // However since we also want this code to work when short URLs are enabled, we only remove the segment part
        // if a Resource type has been identified (see below) and if not, we assume the URL is pointing to an Entity
        // Resource.
        String type = extendedURL.getSegments().get(0);

        // First, try to locate a URL Resolver registered only for this URL scheme
        try {
            resolver = this.componentManager.getInstance(new DefaultParameterizedType(null,
                ResourceReferenceResolver.class, ExtendedURL.class), computeHint(type));
            extendedURL.getSegments().remove(0);
        } catch (ComponentLookupException e) {
            // Second, if not found, try to locate a URL Resolver registered for all URL schemes
            try {
                resolver = this.componentManager.getInstance(
                    new DefaultParameterizedType(null, ResourceReferenceResolver.class, ExtendedURL.class), type);
                extendedURL.getSegments().remove(0);
            } catch (ComponentLookupException cle) {
                // No specific Resource Type Resolver had been found. In order to support short URLs (ie. without the
                // "bin" or "wiki" part specified), we assume the URL is pointing to an Entity Resource Reference.
                // Since the "wiki" type was not selected, we're assuming that the we'll use the "bin" entity resolver.
                resolver = this.binEntityResourceReferenceResolver;
            }
        }

        return resolver.resolve(extendedURL, parameters);
    }

    private String computeHint(String type)
    {
        return String.format("standard/%s", type);
    }
}
