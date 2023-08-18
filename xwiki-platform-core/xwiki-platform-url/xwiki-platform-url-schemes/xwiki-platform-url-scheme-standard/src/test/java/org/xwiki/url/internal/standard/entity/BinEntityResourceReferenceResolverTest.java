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

import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.component.util.ReflectionUtils;
import org.xwiki.model.EntityType;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link BinEntityResourceReferenceResolver}.
 *
 * @version $Id$
 * @since 6.1M2
 */
class BinEntityResourceReferenceResolverTest
{
    private BinEntityResourceReferenceResolver resolver;

    private WikiReference wikiReference = new WikiReference("wiki");

    private WikiReferenceExtractor wikiReferenceExtractor;

    private EntityReferenceResolver<EntityReference> entityReferenceResolver;

    private EntityResourceActionLister entityResourceActionLister;

    private StandardURLConfiguration configuration;

    @BeforeEach
    public void beforeEach() throws Exception
    {
        this.resolver = new BinEntityResourceReferenceResolver();

        this.wikiReferenceExtractor = mock(WikiReferenceExtractor.class);
        when(this.wikiReferenceExtractor.extract(any(ExtendedURL.class))).thenReturn(this.wikiReference);
        ReflectionUtils.setFieldValue(this.resolver, "wikiExtractor", this.wikiReferenceExtractor);

        this.entityReferenceResolver = mock(EntityReferenceResolver.class);
        ReflectionUtils.setFieldValue(this.resolver, "defaultReferenceEntityReferenceResolver",
            this.entityReferenceResolver);

        this.configuration = mock(StandardURLConfiguration.class);
        ReflectionUtils.setFieldValue(this.resolver, "configuration", this.configuration);

        this.entityResourceActionLister = mock(EntityResourceActionLister.class);
        when(this.entityResourceActionLister.listActions()).thenReturn(Arrays.asList("view", "download"));
        ReflectionUtils.setFieldValue(this.resolver, "entityResourceActionLister", this.entityResourceActionLister);
    }

    @Test
    void createResourceWhenViewActionHidden() throws Exception
    {
        when(this.configuration.isViewActionHidden()).thenReturn(true);

        EntityReference fullSingleSpaceReference = buildEntityReference("wiki", Arrays.asList("space"), "page");
        EntityReference fullTwoSpacesReference =
            buildEntityReference("wiki", Arrays.asList("space1", "space2"), "page");

        // Test when no segment
        testCreateResource("http://localhost/bin", "view", this.wikiReference, fullSingleSpaceReference,
            EntityType.DOCUMENT);

        // Test when no segment but trailing slash
        testCreateResource("http://localhost/bin/", "view", this.wikiReference, fullSingleSpaceReference,
            EntityType.DOCUMENT);

        // Test when single space segment, to be Nested Document friendly.
        // Normally the last segment is always the page name but we want to handle a special case when we
        // have "/view/something" and we wish in this case to consider that "something" is the space. This
        // is to handle Nested Documents, so that the user can have a top level Nested Document
        // (something.WebHome) and access it from /view/something. If we didn't handle this special case
        // the user would get Main.something and thus wouldn't be able to access something.WebHome. He'd
        // need to use /view/something/ which is not natural in the Nested Document mode.
        testCreateResource("http://localhost/bin/space", "view",
            buildEntityReference("wiki", Arrays.asList("space"), null), fullSingleSpaceReference, EntityType.DOCUMENT);

        // Test when 1 space segment and trailing slash
        testCreateResource("http://localhost/bin/space/", "view",
            buildEntityReference("wiki", Arrays.asList("space"), null), fullSingleSpaceReference, EntityType.DOCUMENT);

        // Test when 2 space segments and trailing slash
        testCreateResource("http://localhost/bin/space1/space2/", "view",
            buildEntityReference("wiki", Arrays.asList("space1", "space2"), null), fullTwoSpacesReference,
            EntityType.DOCUMENT);

        // Test when 1 space and page segments
        testCreateResource("http://localhost/bin/space/page", "view", fullSingleSpaceReference,
            fullSingleSpaceReference, EntityType.DOCUMENT);

        // Test when 2 spaces and page segments
        testCreateResource("http://localhost/bin/space1/space2/page", "view", fullTwoSpacesReference,
            fullTwoSpacesReference, EntityType.DOCUMENT);

        // Test when space segment is called "view"
        testCreateResource("http://localhost/bin/view/space/page", "view", fullSingleSpaceReference,
            fullSingleSpaceReference, EntityType.DOCUMENT);
        EntityReference viewTwoSpacesReference =
            buildEntityReference("wiki", Arrays.asList("view", "space2"), "page");
        testCreateResource("http://localhost/bin/view/view/space2/page", "view", viewTwoSpacesReference,
            viewTwoSpacesReference, EntityType.DOCUMENT);

        // Test when space segment is called "download"
        EntityReference downloadTwoSpacesReference =
            buildEntityReference("wiki", Arrays.asList("download", "space2"), "page");
        testCreateResource("http://localhost/bin/view/download/space2/page", "view", downloadTwoSpacesReference,
            downloadTwoSpacesReference, EntityType.DOCUMENT);

        // Test when download action
        testCreateResource("http://localhost/bin/download/space/page", "download", fullSingleSpaceReference,
            fullSingleSpaceReference, EntityType.DOCUMENT);
    }

    @Test
    void createResourceWhenViewActionShown() throws Exception
    {
        when(this.configuration.isViewActionHidden()).thenReturn(false);

        EntityReference fullSingleSpaceReference = buildEntityReference("wiki", Arrays.asList("space"), "page");
        EntityReference fullTwoSpacesReference =
            buildEntityReference("wiki", Arrays.asList("space1", "space2"), "page");

        // Test when 1 space segment to be Nested Document friendly (see the test above for more explanations)
        testCreateResource("http://localhost/bin/view/space", "view",
            buildEntityReference("wiki", Arrays.asList("space"), null), fullSingleSpaceReference, EntityType.DOCUMENT);

        // Test when 1 space segment and trailing slash
        testCreateResource("http://localhost/bin/view/space/", "view",
            buildEntityReference("wiki", Arrays.asList("space"), null), fullSingleSpaceReference, EntityType.DOCUMENT);

        // Test when 2 space segments and trailing slash
        testCreateResource("http://localhost/bin/view/space1/space2/", "view",
            buildEntityReference("wiki", Arrays.asList("space1", "space2"), null), fullTwoSpacesReference,
            EntityType.DOCUMENT);

        // Test when 1 space and page segments
        testCreateResource("http://localhost/bin/view/space/page", "view", fullSingleSpaceReference,
            fullSingleSpaceReference, EntityType.DOCUMENT);

        // Test when 2 spaces and page segments
        testCreateResource("http://localhost/bin/view/space1/space2/page", "view", fullTwoSpacesReference,
            fullTwoSpacesReference, EntityType.DOCUMENT);

        // Test when no "view" specified and space that is not an actio name
        testCreateResource("http://localhost/bin/space/page", "view", fullSingleSpaceReference,
            fullSingleSpaceReference, EntityType.DOCUMENT);

    }

    @Test
    void createResourceWhenSpaceAndPageNamesContainDots() throws Exception
    {
        EntityReference reference = buildEntityReference("wiki", Arrays.asList("space.with.dots"), "page.with.dots");
        testCreateResource("http://localhost/bin/view/space.with.dots/page.with.dots", "view",
            reference, reference, EntityType.DOCUMENT);
    }

    @Test
    void createResourceWhenFileActionAction() throws Exception
    {
        EntityReference singleSpaceReference =
            buildEntityReference("wiki", Arrays.asList("space"), "page", "attachment.ext");
        EntityReference twoSpaceReference =
            buildEntityReference("wiki", Arrays.asList("space1", "space2"), "page", "attachment.ext");

        testCreateResource("http://localhost/bin/download/space/page/attachment.ext", "download", singleSpaceReference,
            singleSpaceReference, EntityType.ATTACHMENT);
        testCreateResource("http://localhost/bin/download/space1/space2/page/attachment.ext", "download",
            twoSpaceReference, twoSpaceReference, EntityType.ATTACHMENT);

        testCreateResource("http://localhost/bin/delattachment/space/page/attachment.ext", "delattachment",
            singleSpaceReference, singleSpaceReference, EntityType.ATTACHMENT);
        testCreateResource("http://localhost/bin/delattachment/space1/space2/page/attachment.ext", "delattachment",
            twoSpaceReference, twoSpaceReference, EntityType.ATTACHMENT);

        testCreateResource("http://localhost/bin/viewattachrev/space/page/attachment.ext", "viewattachrev",
            singleSpaceReference, singleSpaceReference, EntityType.ATTACHMENT);
        testCreateResource("http://localhost/bin/viewattachrev/space1/space2/page/attachment.ext", "viewattachrev",
            twoSpaceReference, twoSpaceReference, EntityType.ATTACHMENT);

        testCreateResource("http://localhost/bin/downloadrev/space/page/attachment.ext", "downloadrev",
            singleSpaceReference, singleSpaceReference, EntityType.ATTACHMENT);
        testCreateResource("http://localhost/bin/downloadrev/space1/space2/page/attachment.ext", "downloadrev",
            twoSpaceReference, twoSpaceReference, EntityType.ATTACHMENT);
    }

    @Test
    void createResourceWhenURLHasParameters() throws Exception
    {
        EntityReference fullReference = buildEntityReference("wiki", Arrays.asList("space"), "page");
        ResourceReference resource =
            testCreateResource("http://localhost/bin/view/space/page?param1=value1&param2=value2", "view",
                fullReference, fullReference, EntityType.DOCUMENT);

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
    void createEntityResourceWhenURLHasAnchor() throws Exception
    {
        EntityReference fullReference = buildEntityReference("wiki", Arrays.asList("space"), "page");
        EntityResourceReference resource =
            testCreateResource("http://localhost/bin/view/space/page#anchor", "view",
                fullReference, fullReference, EntityType.DOCUMENT);

        assertEquals("anchor", resource.getAnchor());
    }

    private EntityResourceReference testCreateResource(String testURL, String expectedActionName,
        EntityReference expectedReference, EntityReference returnedReference, EntityType expectedEntityType)
        throws Exception
    {
        when(this.entityReferenceResolver.resolve(expectedReference, expectedEntityType)).thenReturn(returnedReference);
        ExtendedURL extendedURL = new ExtendedURL(new URL(testURL), null);
        // Remove the resource type segment since this is what gets passed to specific Reference Resolvers.
        extendedURL.getSegments().remove(0);
        EntityResourceReference entityResource = this.resolver.resolve(extendedURL,
            new ResourceType("bin"), Collections.<String, Object>emptyMap());

        assertEquals(expectedActionName, entityResource.getAction().getActionName());
        assertEquals(returnedReference, entityResource.getEntityReference());

        return entityResource;
    }

    private EntityReference buildEntityReference(String wiki, List<String> spaces, String page)
    {
        return buildEntityReference(wiki, spaces, page, null);
    }

    private EntityReference buildEntityReference(String wiki, List<String> spaces, String page, String attachment)
    {
        EntityReference entityReference = new WikiReference(wiki);
        if (spaces != null) {
            EntityReference parent = entityReference;
            for (String space : spaces) {
                entityReference = new EntityReference(space, EntityType.SPACE, parent);
                parent = entityReference;
            }
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
