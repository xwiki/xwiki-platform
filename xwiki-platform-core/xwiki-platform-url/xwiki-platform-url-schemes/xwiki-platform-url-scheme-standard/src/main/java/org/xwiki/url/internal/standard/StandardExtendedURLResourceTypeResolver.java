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
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.manager.ComponentRepositoryException;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.component.util.DefaultParameterizedType;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceResolver;
import org.xwiki.resource.CreateResourceTypeException;
import org.xwiki.resource.ResourceReferenceResolver;
import org.xwiki.resource.ResourceType;
import org.xwiki.resource.entity.EntityResourceReference;
import org.xwiki.resource.internal.entity.EntityResourceActionLister;
import org.xwiki.url.ExtendedURL;
import org.xwiki.url.internal.AbstractExtendedURLResourceTypeResolver;
import org.xwiki.url.internal.standard.entity.BinEntityResourceReferenceResolver;
import org.xwiki.url.internal.standard.entity.WikiEntityResourceReferenceResolver;

/**
 * Extracts the {@link ResourceType} from a passed {@link ExtendedURL}, using the {@code standard} URL scheme format.
 * In that format the Resource Type is the path segment in the URL just after the Context Path one (e.g.
 * {@code bin} in {@code http://<server>/xwiki/bin/view/Space/Page}.
 *
 * @version $Id$
 * @since 7.1M1
 */
@Component
@Named("standard")
@Singleton
public class StandardExtendedURLResourceTypeResolver extends AbstractExtendedURLResourceTypeResolver implements
    Initializable
{
    private static final String HINT = "standard";

    @Inject
    private ComponentManager rootComponentManager;

    /**
     * Used to know if the wiki is in path-based configuration or not.
     */
    @Inject
    private StandardURLConfiguration configuration;

    @Override
    public void initialize() throws InitializationException
    {
        // Note that we initialize the 2 Resolver in this component and not in
        // StandardExtendedURLResourceReferenceResolver because this class is called before
        // StandardExtendedURLResourceReferenceResolver and it checks if the Resolvers are registered when performing
        // its resolve. Thus the 2 Resolvers need to be registered *before* this class's resolve() is called.

        // Step 1: Dynamically register a WikiEntityResourceReferenceResolver since the user can choose the Resource
        // type name when specifying an Entity Resource Reference in path-based multiwiki, see
        // StandardURLConfiguration#getWikiPathPrefix()
        registerEntityResourceReferenceResolver(this.configuration.getWikiPathPrefix(),
            WikiEntityResourceReferenceResolver.class, "path");

        // Step 2: Dynamically register a BinEntityResourceReferenceResolver. Note that we use the
        // {@link EntityResourceReference#TYPE} as the hint since the Standard ExtendedURL Resource Type Resolver will
        // have converted from {@code this.configuration.getEntityPathPrefix()} to {@link EntityResourceReference#TYPE}
        // already.
        registerEntityResourceReferenceResolver(EntityResourceReference.TYPE.getId(),
            BinEntityResourceReferenceResolver.class, "domain");
    }

    @Override
    public ResourceType resolve(ExtendedURL extendedURL, Map<String, Object> parameters)
        throws CreateResourceTypeException
    {
        return resolve(HINT, extendedURL, parameters);
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

        DefaultComponentDependency<StandardURLConfiguration> standardURLConfigurationDependency =
            new DefaultComponentDependency<>();
        standardURLConfigurationDependency.setRoleType(StandardURLConfiguration.class);
        standardURLConfigurationDependency.setName("configuration");
        resolverDescriptor.addComponentDependency(standardURLConfigurationDependency);

        DefaultComponentDependency<EntityResourceActionLister> entityResourceActionListerDependency =
            new DefaultComponentDependency<>();
        entityResourceActionListerDependency.setRoleType(EntityResourceActionLister.class);
        entityResourceActionListerDependency.setName("entityResourceActionLister");
        resolverDescriptor.addComponentDependency(entityResourceActionListerDependency);

        try {
            this.rootComponentManager.registerComponent(resolverDescriptor);
        } catch (ComponentRepositoryException e) {
            throw new InitializationException(String.format(
                "Failed to dynamically register Resource Reference Resolver for hint [%s]", hint), e);
        }
    }

    private String computeHint(String type)
    {
        return String.format("%s/%s", HINT, type);
    }
}
