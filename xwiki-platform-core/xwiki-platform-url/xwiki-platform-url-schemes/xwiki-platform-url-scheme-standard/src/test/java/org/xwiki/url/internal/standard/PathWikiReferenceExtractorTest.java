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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.url.ExtendedURL;
import org.xwiki.wiki.descriptor.WikiDescriptor;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link PathWikiReferenceExtractor}.
 *
 * @version $Id$
 * @since 6.3M1
 */
@ComponentTest
class PathWikiReferenceExtractorTest
{
    @InjectMockComponents
    private PathWikiReferenceExtractor extractor;

    @MockComponent
    private WikiDescriptorManager wikiDescriptorManager;

    @MockComponent
    private Execution execution;

    @MockComponent
    private StandardURLConfiguration urlConfiguration;

    @BeforeEach
    void setUp() throws Exception
    {
        when(this.wikiDescriptorManager.getMainWikiId()).thenReturn("xwiki");
    }

    @Test
    void extractWhenWikiDescriptor() throws Exception
    {
        setUpConfiguration(WikiNotFoundBehavior.REDIRECT_TO_MAIN_WIKI);

        when(this.wikiDescriptorManager.getByAlias("someWiki")).thenReturn(new WikiDescriptor("wikiid", "someWiki"));

        testAndAssert("http://localhost/xwiki/wiki/someWiki/view/Main/WebHome", "wikiid");
    }

    @Test
    void extractWhenNoWikiDescriptor() throws Exception
    {
        setUpConfiguration(WikiNotFoundBehavior.REDIRECT_TO_MAIN_WIKI);
        testAndAssert("http://localhost/xwiki/wiki/someWiki/view/Main/WebHome", "xwiki");
    }

    @Test
    void extractWhenWikiDescriptorButEmptyServerName() throws Exception
    {
        setUpConfiguration(WikiNotFoundBehavior.REDIRECT_TO_MAIN_WIKI);

        when(this.wikiDescriptorManager.getByAlias("someWiki")).thenReturn(new WikiDescriptor("", "someWiki"));

        testAndAssert("http://localhost/xwiki/wiki/someWiki/view/Main/WebHome", "xwiki");
    }

    @Test
    void extractWhenNoDescriptorMatchingAliasButDescriptorMatchingId() throws Exception
    {
        setUpConfiguration(WikiNotFoundBehavior.REDIRECT_TO_MAIN_WIKI);

        when(this.wikiDescriptorManager.getByAlias("someWiki")).thenReturn(null);
        when(this.wikiDescriptorManager.getById("someWiki")).thenReturn(new WikiDescriptor("dummy", "dummy"));

        testAndAssert("http://localhost/xwiki/wiki/someWiki/view/Main/WebHome", "somewiki");
    }

    @Test
    void extractWhenNoWikiDescriptorButWithDomainBasedURL() throws Exception
    {
        setUpConfiguration(WikiNotFoundBehavior.REDIRECT_TO_MAIN_WIKI);
        testAndAssert("http://wiki.server.com/xwiki/bin/view/Main/WebHome", "xwiki");
    }

    @Test
    void extractWhenNoWikiDescriptorAndDisplayErrorWhenWikiNotFound() throws Exception
    {
        setUpConfiguration(WikiNotFoundBehavior.DISPLAY_ERROR);
        testAndAssert("http://localhost/xwiki/wiki/someWiki/view/Main/WebHome", "somewiki");
    }

    @Test
    void extractWhenNoExecutionContext() throws Exception
    {
        testAndAssert("http://localhost/xwiki/wiki/someWiki/view/Main/WebHome", "somewiki");

        verify(this.wikiDescriptorManager, never()).getByAlias(any());
        verify(this.wikiDescriptorManager, never()).getById(any());
    }

    private void testAndAssert(String urlToTest, String expectedWikiId) throws Exception
    {
        ExtendedURL url = new ExtendedURL(new URL(urlToTest), "xwiki");
        // Remove the resource type (i.e. the first segment) since this is what is expected by the extractor
        url.getSegments().remove(0);
        WikiReference wikiReference = this.extractor.extract(url);
        assertEquals(new WikiReference(expectedWikiId), wikiReference);
    }

    private void setUpConfiguration(WikiNotFoundBehavior wikiNotFoundBehavior)
    {
        // Simulate a configured Execution Context
        ExecutionContext executionContext = new ExecutionContext();
        executionContext.setProperty("xwikicontext", true);
        when(this.execution.getContext()).thenReturn(executionContext);
        when(this.urlConfiguration.getWikiNotFoundBehavior()).thenReturn(wikiNotFoundBehavior);
    }
}
