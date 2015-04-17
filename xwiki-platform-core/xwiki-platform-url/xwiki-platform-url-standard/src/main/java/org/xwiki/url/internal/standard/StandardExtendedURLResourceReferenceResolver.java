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

import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

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
import org.xwiki.resource.ResourceType;
import org.xwiki.resource.UnsupportedResourceReferenceException;
import org.xwiki.url.ExtendedURL;
import org.xwiki.url.internal.standard.entity.BinEntityResourceReferenceResolver;
import org.xwiki.url.internal.standard.entity.WikiEntityResourceReferenceResolver;

/**
 * Parses an {@link ExtendedURL} written in the "standard" format and generate a
 * {@link org.xwiki.resource.ResourceReference} out of it.
 *
 * @version $Id$
 * @since 7.1M1
 */
@Component
@Named("standard")
@Singleton
public class StandardExtendedURLResourceReferenceResolver implements ResourceReferenceResolver<ExtendedURL>,
    Initializable
{
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
     * No specific parameter is supported.
     *
     * @see org.xwiki.resource.ResourceReferenceResolver#resolve(Object, ResourceType, Map)
     */
    @Override
    public ResourceReference resolve(ExtendedURL extendedURL, ResourceType type, Map<String, Object> parameters)
        throws CreateResourceReferenceException, UnsupportedResourceReferenceException
    {
        ResourceReferenceResolver<ExtendedURL> resolver = findResolver(type);
        return resolver.resolve(extendedURL, type, parameters);
    }

    /**
     *  Find the right Resolver for the passed Resource type and call it.
     */
    private ResourceReferenceResolver<ExtendedURL> findResolver(ResourceType type)
        throws UnsupportedResourceReferenceException
    {
        ResourceReferenceResolver<ExtendedURL> resolver;

        try {
            resolver = this.componentManager.getInstance(new DefaultParameterizedType(null,
                ResourceReferenceResolver.class, ExtendedURL.class), computeHint(type.getId()));
        } catch (ComponentLookupException e) {
            // Second, if not found, try to locate a URL Resolver registered for all URL schemes
            try {
                resolver = this.componentManager.getInstance(new DefaultParameterizedType(null,
                    ResourceReferenceResolver.class, ExtendedURL.class), type.getId());
            } catch (ComponentLookupException cle) {
                // Error, this shouldn't happen
                throw new UnsupportedResourceReferenceException(String.format(
                    "Couldn't find any Resource Reference Resolver for type [%s]", type), cle);
            }
        }

        return resolver;
    }

    private String computeHint(String type)
    {
        return String.format("standard/%s", type);
    }
}
