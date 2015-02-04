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

import java.net.URL;
import java.util.Collections;

import org.junit.Before;
import org.junit.Test;
import org.xwiki.component.util.ReflectionUtils;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceResolver;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.resource.ResourceReference;
import org.xwiki.resource.entity.EntityResourceReference;
import org.xwiki.url.ExtendedURL;
import org.xwiki.url.internal.standard.entity.WikiEntityResourceReferenceResolver;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link org.xwiki.url.internal.standard.entity.WikiEntityResourceReferenceResolver}.
 *
 * @version $Id$
 * @since 6.3M1
 */
public class WikiEntityResourceReferenceResolverTest
{
    private WikiEntityResourceReferenceResolver resolver;

    private WikiReference wikiReference = new WikiReference("somewiki");

    private WikiReferenceExtractor wikiReferenceExtractor;

    private EntityReferenceResolver<EntityReference> entityReferenceResolver;

    @Before
    public void setUp() throws Exception
    {
        this.resolver = new WikiEntityResourceReferenceResolver();

        this.wikiReferenceExtractor = mock(WikiReferenceExtractor.class);
        ReflectionUtils.setFieldValue(this.resolver, "wikiExtractor", this.wikiReferenceExtractor);

        this.entityReferenceResolver = mock(EntityReferenceResolver.class);
        ReflectionUtils.setFieldValue(this.resolver, "defaultReferenceEntityReferenceResolver",
            this.entityReferenceResolver);
    }

    @Test
    public void resolve() throws Exception
    {
        ExtendedURL url = new ExtendedURL(new URL("http://localhost/wiki/somewiki/view/space/page"));
        url.getSegments().remove(0);
        when(wikiReferenceExtractor.extract(url)).thenReturn(this.wikiReference);

        EntityReference reference = buildEntityReference("somewiki", "space", "page");
        EntityResourceReference result = (EntityResourceReference) testCreateResource(
            "http://localhost/wiki/somewiki/view/space/page", "view", reference, reference, EntityType.DOCUMENT);

        assertEquals(new DocumentReference("somewiki", "space", "page"), result.getEntityReference());
    }
    
    private ResourceReference testCreateResource(String testURL, String expectedActionName,
        EntityReference expectedReference, EntityReference returnedReference, EntityType expectedEntityType)
        throws Exception
    {
        when(this.entityReferenceResolver.resolve(expectedReference, expectedEntityType)).thenReturn(
            returnedReference);
        ExtendedURL url = new ExtendedURL(new URL(testURL));
        // Remove the resource type segment since this is what gets passed to specific Reference Resolvers.
        url.getSegments().remove(0);
        EntityResourceReference entityResource =
            (EntityResourceReference) this.resolver.resolve(url, Collections.EMPTY_MAP);

        assertEquals(expectedActionName, entityResource.getAction().getActionName());
        assertEquals(returnedReference, entityResource.getEntityReference());

        return entityResource;
    }

    private EntityReference buildEntityReference(String wiki, String space, String page)
    {
        return buildEntityReference(wiki, space, page, null);
    }

    private EntityReference buildEntityReference(String wiki, String space, String page, String attachment)
    {
        EntityReference entityReference = new WikiReference(wiki);
        if (space != null) {
            entityReference = new EntityReference(space, EntityType.SPACE, entityReference);
        }
        if (page != null) {
            entityReference = new EntityReference(page, EntityType.DOCUMENT, entityReference);
        }
        if (attachment != null) {
            entityReference = new EntityReference(attachment, EntityType.ATTACHMENT, entityReference);
        }
        return entityReference;
    }
}
