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

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceResolver;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.test.mockito.MockitoComponentMockingRule;
import org.xwiki.url.XWikiEntityURL;
import org.xwiki.url.XWikiURL;
import org.xwiki.url.internal.ExtendedURL;

import static org.mockito.Mockito.*;
import static org.junit.Assert.*;

/**
 * Unit tests for {@link ExtendedURLXWikiEntityURLFactory}.
 *
 * @version $Id$
 * @since 5.1M1
 */
public class ExtendedURLXWikiEntityURLFactoryTest
{
    @Rule
    public MockitoComponentMockingRule<ExtendedURLXWikiEntityURLFactory> mocker =
        new MockitoComponentMockingRule(ExtendedURLXWikiEntityURLFactory.class);

    private WikiReference wikiReference = new WikiReference("wiki");

    @Before
    public void setUp() throws Exception
    {
        WikiReferenceExtractor wikiReferenceExtractor = this.mocker.getInstance(WikiReferenceExtractor.class);
        when(wikiReferenceExtractor.extract(any(ExtendedURL.class))).thenReturn(
            new ImmutablePair<WikiReference, Boolean>(this.wikiReference, false));
    }

    @Test
    public void createURLWhenNoViewAction() throws Exception
    {
        EntityReference fullReference = buildEntityReference("wiki", "space", "page");

        // Test when no segment
        testCreateURL("http://localhost/bin", "view", this.wikiReference, fullReference, EntityType.DOCUMENT);

        // Test when no segment but trailing slash
        testCreateURL("http://localhost/bin/", "view", this.wikiReference, fullReference, EntityType.DOCUMENT);

        // Test when page segment
        testCreateURL("http://localhost/bin/page", "view", buildEntityReference("wiki", null, "page"),
            fullReference, EntityType.DOCUMENT);

        // Test when space segment and trailing slash
        testCreateURL("http://localhost/bin/space/", "view", buildEntityReference("wiki", "space", null),
            fullReference, EntityType.DOCUMENT);

        // Test when space and page segments
        testCreateURL("http://localhost/bin/space/page", "view", fullReference, fullReference,
            EntityType.DOCUMENT);
    }

    @Test
    public void createURLWhenViewAction() throws Exception
    {
        EntityReference fullReference = buildEntityReference("wiki", "space", "page");

        // Test when space segment and trailing slash
        testCreateURL("http://localhost/bin/view/space/", "view", buildEntityReference("wiki", "space", null),
            fullReference, EntityType.DOCUMENT);

        // Test when space and page segments
        testCreateURL("http://localhost/bin/view/space/page", "view", fullReference, fullReference,
            EntityType.DOCUMENT);

        // Test when space and page segments and trailing slash
        testCreateURL("http://localhost/bin/view/space/page/", "view", fullReference, fullReference,
            EntityType.DOCUMENT);

        // Test when space and page segments and ignored paths
        testCreateURL("http://localhost/bin/view/space/page/ignored/path", "view", fullReference, fullReference,
            EntityType.DOCUMENT);
    }

    @Test
    public void createURLWhenSpaceAndPageNamesContainDots() throws Exception
    {
        EntityReference reference = buildEntityReference("wiki", "space.with.dots", "page.with.dots");
        testCreateURL("http://localhost/bin/view/space.with.dots/page.with.dots", "view",
            reference, reference, EntityType.DOCUMENT);
    }

    @Test
    public void createURLWhenDownloadAction() throws Exception
    {
        EntityReference reference = buildEntityReference("wiki", "space", "page", "attachment.ext");
        testCreateURL("http://localhost/bin/download/space/page/attachment.ext", "download", reference, reference,
            EntityType.ATTACHMENT);
    }

    @Test
    public void createURLWhenURLHasParameters() throws Exception
    {
        EntityReference fullReference = buildEntityReference("wiki", "space", "page");
        XWikiURL xwikiURL = testCreateURL("http://localhost/bin/view/space/page?param1=value1&param2=value2",
            "view", fullReference, fullReference, EntityType.DOCUMENT);

        // Assert parameters
        // Note: the parameters order are the same as the order specified in the URL.
        Map<String, List<String>> expectedMap = new LinkedHashMap<String, List<String>>();
        expectedMap.put("param1", Arrays.asList("value1"));
        expectedMap.put("param2", Arrays.asList("value2"));
        assertEquals(expectedMap, xwikiURL.getParameters());

        // Also verify it works when there's a param with no value.
        xwikiURL = testCreateURL("http://localhost/bin/view/space/page?param",
            "view", fullReference, fullReference, EntityType.DOCUMENT);
        expectedMap = new LinkedHashMap<String, List<String>>();
        expectedMap.put("param", Collections.<String>emptyList());
        assertEquals(expectedMap, xwikiURL.getParameters());
    }

    private XWikiURL testCreateURL(String testURL, String expectedAction, EntityReference expectedReference,
        EntityReference returnedReference, EntityType expectedEntityType) throws Exception
    {
        setUpEntityReferenceResolverMock(expectedReference, returnedReference, expectedEntityType);
        ExtendedURL url = new ExtendedURL(new URL(testURL));
        XWikiEntityURL entityURL = this.mocker.getComponentUnderTest().createURL(url, Collections.EMPTY_MAP);

        assertEquals(expectedAction, entityURL.getAction());
        assertEquals(returnedReference, entityURL.getEntityReference());

        return entityURL;
    }

    private void setUpEntityReferenceResolverMock(EntityReference expectedReference, EntityReference returnedReference,
        EntityType expectedEntityType) throws Exception
    {
        EntityReferenceResolver<EntityReference> resolver =
            this.mocker.getInstance(EntityReferenceResolver.TYPE_REFERENCE);
        when(resolver.resolve(expectedReference, expectedEntityType)).thenReturn(returnedReference);
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
