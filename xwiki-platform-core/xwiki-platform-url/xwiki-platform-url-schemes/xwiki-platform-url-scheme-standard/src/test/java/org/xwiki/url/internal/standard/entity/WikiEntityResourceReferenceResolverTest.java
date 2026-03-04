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
package org.xwiki.url.internal.standard.entity;

import java.net.URI;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.component.util.ReflectionUtils;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceResolver;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.resource.ResourceReference;
import org.xwiki.resource.ResourceType;
import org.xwiki.resource.entity.EntityResourceReference;
import org.xwiki.resource.internal.entity.EntityResourceActionLister;
import org.xwiki.url.ExtendedURL;
import org.xwiki.url.internal.standard.StandardURLConfiguration;
import org.xwiki.url.internal.standard.WikiReferenceExtractor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link org.xwiki.url.internal.standard.entity.WikiEntityResourceReferenceResolver}.
 *
 * @version $Id$
 * @since 6.3M1
 */
class WikiEntityResourceReferenceResolverTest
{
    private WikiEntityResourceReferenceResolver resolver;

    private WikiReference wikiReference = new WikiReference("somewiki");

    private WikiReferenceExtractor wikiReferenceExtractor;

    private EntityReferenceResolver<EntityReference> entityReferenceResolver;

    @BeforeEach
    void setUp()
    {
        this.resolver = new WikiEntityResourceReferenceResolver();

        this.wikiReferenceExtractor = mock(WikiReferenceExtractor.class);
        ReflectionUtils.setFieldValue(this.resolver, "wikiExtractor", this.wikiReferenceExtractor);

        this.entityReferenceResolver = mock(EntityReferenceResolver.class);
        ReflectionUtils.setFieldValue(this.resolver, "defaultReferenceEntityReferenceResolver",
            this.entityReferenceResolver);

        StandardURLConfiguration configuration = mock(StandardURLConfiguration.class);
        when(configuration.isViewActionHidden()).thenReturn(false);
        ReflectionUtils.setFieldValue(this.resolver, "configuration", configuration);

        EntityResourceActionLister entityResourceActionLister = mock(EntityResourceActionLister.class);
        when(entityResourceActionLister.listActions()).thenReturn(List.of("view", "download"));
        ReflectionUtils.setFieldValue(this.resolver, "entityResourceActionLister", entityResourceActionLister);
    }

    @Test
    void resolve() throws Exception
    {
        ExtendedURL url = new ExtendedURL(new URI("http://localhost/wiki/somewiki/view/space/page").toURL(), null);
        url.getSegments().removeFirst();
        when(wikiReferenceExtractor.extract(url)).thenReturn(this.wikiReference);

        EntityReference reference = buildEntityReference();
        EntityResourceReference result = (EntityResourceReference) testCreateResource(
            reference, reference);

        assertEquals(new DocumentReference("somewiki", "space", "page"), result.getEntityReference());
    }
    
    private ResourceReference testCreateResource(EntityReference expectedReference, EntityReference returnedReference)
        throws Exception
    {
        when(this.entityReferenceResolver.resolve(expectedReference, EntityType.DOCUMENT)).thenReturn(
            returnedReference);
        ExtendedURL extendedURL = new ExtendedURL(
            new URI("http://localhost/wiki/somewiki/view/space/page").toURL(), null);
        // Remove the resource type segment since this is what gets passed to specific Reference Resolvers.
        extendedURL.getSegments().removeFirst();
        EntityResourceReference entityResource =
            this.resolver.resolve(extendedURL, new ResourceType("wiki"), Collections.emptyMap());

        assertEquals("view", entityResource.getAction().getActionName());
        assertEquals(returnedReference, entityResource.getEntityReference());

        return entityResource;
    }

    private EntityReference buildEntityReference()
    {
        EntityReference entityReference = new WikiReference("somewiki");
        entityReference = new EntityReference("space", EntityType.SPACE, entityReference);
        entityReference = new EntityReference("page", EntityType.DOCUMENT, entityReference);
        return entityReference;
    }
}
