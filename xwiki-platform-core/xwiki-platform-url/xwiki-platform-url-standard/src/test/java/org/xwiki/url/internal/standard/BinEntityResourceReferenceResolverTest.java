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
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.xwiki.component.util.ReflectionUtils;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceResolver;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.resource.entity.EntityResourceReference;
import org.xwiki.resource.ResourceReference;
import org.xwiki.url.ExtendedURL;
import org.xwiki.url.internal.standard.entity.BinEntityResourceReferenceResolver;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link BinEntityResourceReferenceResolver}.
 *
 * @version $Id$
 * @since 6.1M2
 */
public class BinEntityResourceReferenceResolverTest
{
    private BinEntityResourceReferenceResolver resolver;

    private WikiReference wikiReference = new WikiReference("wiki");

    private WikiReferenceExtractor wikiReferenceExtractor;

    private EntityReferenceResolver<EntityReference> entityReferenceResolver;

    @Before
    public void setUp() throws Exception
    {
        this.resolver = new BinEntityResourceReferenceResolver();

        this.wikiReferenceExtractor = mock(WikiReferenceExtractor.class);
        when(wikiReferenceExtractor.extract(any(ExtendedURL.class))).thenReturn(this.wikiReference);
        ReflectionUtils.setFieldValue(this.resolver, "wikiExtractor", this.wikiReferenceExtractor);

        this.entityReferenceResolver = mock(EntityReferenceResolver.class);
        ReflectionUtils.setFieldValue(this.resolver, "defaultReferenceEntityReferenceResolver",
            this.entityReferenceResolver);
    }

    @Test
    public void createResourceWhenNoViewAction() throws Exception
    {
        EntityReference fullReference = buildEntityReference("wiki", "space", "page");

        // Test when no segment
        testCreateResource("http://localhost/bin", "view", this.wikiReference, fullReference, EntityType.DOCUMENT);

        // Test when no segment but trailing slash
        testCreateResource("http://localhost/bin/", "view", this.wikiReference, fullReference, EntityType.DOCUMENT);

        // Test when page segment
        testCreateResource("http://localhost/bin/page", "view", buildEntityReference("wiki", null, "page"),
            fullReference, EntityType.DOCUMENT);

        // Test when space segment and trailing slash
        testCreateResource("http://localhost/bin/space/", "view", buildEntityReference("wiki", "space", null),
            fullReference, EntityType.DOCUMENT);

        // Test when space and page segments
        testCreateResource("http://localhost/bin/space/page", "view", fullReference, fullReference,
            EntityType.DOCUMENT);
    }

    @Test
    public void createResourceWhenViewAction() throws Exception
    {
        EntityReference fullReference = buildEntityReference("wiki", "space", "page");

        // Test when space segment and trailing slash
        testCreateResource("http://localhost/bin/view/space/", "view", buildEntityReference("wiki", "space", null),
            fullReference, EntityType.DOCUMENT);

        // Test when space and page segments
        testCreateResource("http://localhost/bin/view/space/page", "view", fullReference, fullReference,
            EntityType.DOCUMENT);

        // Test when space and page segments and trailing slash
        testCreateResource("http://localhost/bin/view/space/page/", "view", fullReference, fullReference,
            EntityType.DOCUMENT);

        // Test when space and page segments and ignored paths
        testCreateResource("http://localhost/bin/view/space/page/ignored/path", "view", fullReference, fullReference,
            EntityType.DOCUMENT);
    }

    @Test
    public void createResourceWhenSpaceAndPageNamesContainDots() throws Exception
    {
        EntityReference reference = buildEntityReference("wiki", "space.with.dots", "page.with.dots");
        testCreateResource("http://localhost/bin/view/space.with.dots/page.with.dots", "view",
            reference, reference, EntityType.DOCUMENT);
    }

    @Test
    public void createResourceWhenDownloadAction() throws Exception
    {
        EntityReference reference = buildEntityReference("wiki", "space", "page", "attachment.ext");
        testCreateResource("http://localhost/bin/download/space/page/attachment.ext", "download", reference, reference,
            EntityType.ATTACHMENT);
    }

    @Test
    public void createResourceWhenURLHasParameters() throws Exception
    {
        EntityReference fullReference = buildEntityReference("wiki", "space", "page");
        ResourceReference resource = testCreateResource("http://localhost/bin/view/space/page?param1=value1&param2=value2",
            "view", fullReference, fullReference, EntityType.DOCUMENT);

        // Assert parameters
        // Note: the parameters order are the same as the order specified in the URL.
        Map<String, List<String>> expectedMap = new LinkedHashMap<String, List<String>>();
        expectedMap.put("param1", Arrays.asList("value1"));
        expectedMap.put("param2", Arrays.asList("value2"));
        assertEquals(expectedMap, resource.getParameters());

        // Also verify it works when there's a param with no value.
        resource = testCreateResource("http://localhost/bin/view/space/page?param",
            "view", fullReference, fullReference, EntityType.DOCUMENT);
        expectedMap = new LinkedHashMap<>();
        expectedMap.put("param", Collections.<String>emptyList());
        assertEquals(expectedMap, resource.getParameters());
    }

    @Test
    public void createResourceWhenUsingBinEvenThoughPathConfiguredAsEntity() throws Exception
    {
        EntityReference fullReference = buildEntityReference("wiki", "space", "page");
        testCreateResource("http://localhost/bin/space/page", "view", fullReference, fullReference,
            EntityType.DOCUMENT);
    }

    private ResourceReference testCreateResource(String testURL, String expectedActionName,
        EntityReference expectedReference, EntityReference returnedReference, EntityType expectedEntityType)
        throws Exception
    {
        when(this.entityReferenceResolver.resolve(expectedReference, expectedEntityType)).thenReturn(returnedReference);
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
