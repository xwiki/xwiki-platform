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

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.test.mockito.MockitoComponentMockingRule;
import org.xwiki.url.ExtendedURL;
import org.xwiki.wiki.descriptor.WikiDescriptor;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link PathWikiReferenceExtractor}.
 *
 * @version $Id$
 * @since 6.3M1
 */
public class PathWikiReferenceExtractorTest
{
    @Rule
    public MockitoComponentMockingRule<PathWikiReferenceExtractor> mocker =
        new MockitoComponentMockingRule<>(PathWikiReferenceExtractor.class);

    private WikiDescriptorManager wikiDescriptorManager;

    @Before
    public void setUp() throws Exception
    {
        this.wikiDescriptorManager = mocker.getInstance(WikiDescriptorManager.class);
        when(wikiDescriptorManager.getMainWikiId()).thenReturn("xwiki");
    }

    @Test
    public void extractWhenWikiDescriptor() throws Exception
    {
        setUpConfiguration(WikiNotFoundBehavior.REDIRECT_TO_MAIN_WIKI);

        WikiDescriptorManager wikiDescriptorManager = mocker.getInstance(WikiDescriptorManager.class);
        when(wikiDescriptorManager.getByAlias("someWiki")).thenReturn(new WikiDescriptor("wikiid", "someWiki"));

        testAndAssert("http://localhost/xwiki/wiki/someWiki/view/Main/WebHome", "wikiid");
    }

    @Test
    public void extractWhenNoWikiDescriptor() throws Exception
    {
        setUpConfiguration(WikiNotFoundBehavior.REDIRECT_TO_MAIN_WIKI);
        testAndAssert("http://localhost/xwiki/wiki/someWiki/view/Main/WebHome", "xwiki");
    }

    @Test
    public void extractWhenWikiDescriptorButEmptyServerName() throws Exception
    {
        setUpConfiguration(WikiNotFoundBehavior.REDIRECT_TO_MAIN_WIKI);

        WikiDescriptorManager wikiDescriptorManager = mocker.getInstance(WikiDescriptorManager.class);
        when(wikiDescriptorManager.getByAlias("someWiki")).thenReturn(new WikiDescriptor("", "someWiki"));

        testAndAssert("http://localhost/xwiki/wiki/someWiki/view/Main/WebHome", "xwiki");
    }

    @Test
    public void extractWhenNoDescriptorMatchingAliasButDescriptorMatchingId() throws Exception
    {
        setUpConfiguration(WikiNotFoundBehavior.REDIRECT_TO_MAIN_WIKI);

        WikiDescriptorManager wikiDescriptorManager = mocker.getInstance(WikiDescriptorManager.class);
        when(wikiDescriptorManager.getByAlias("someWiki")).thenReturn(null);
        when(wikiDescriptorManager.getById("someWiki")).thenReturn(new WikiDescriptor("dummy", "dummy"));

        testAndAssert("http://localhost/xwiki/wiki/someWiki/view/Main/WebHome", "somewiki");
    }

    @Test
    public void extractWhenNoWikiDescriptorButWithDomainBasedURL() throws Exception
    {
        setUpConfiguration(WikiNotFoundBehavior.REDIRECT_TO_MAIN_WIKI);
        testAndAssert("http://wiki.server.com/xwiki/bin/view/Main/WebHome", "xwiki");
    }

    @Test
    public void extractWhenNoWikiDescriptorAndDisplayErrorWhenWikiNotFound() throws Exception
    {
        setUpConfiguration(WikiNotFoundBehavior.DISPLAY_ERROR);
        testAndAssert("http://localhost/xwiki/wiki/someWiki/view/Main/WebHome", "somewiki");
    }

    @Test
    public void extractWhenNoExecutionContext() throws Exception
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
        WikiReference wikiReference = this.mocker.getComponentUnderTest().extract(url);
        assertEquals(new WikiReference(expectedWikiId), wikiReference);
    }

    private void setUpConfiguration(WikiNotFoundBehavior wikiNotFoundBehavior) throws Exception
    {
        // Simulate a configured Execution Context
        Execution execution = mocker.getInstance(Execution.class);
        ExecutionContext executionContext = new ExecutionContext();
        executionContext.setProperty("xwikicontext", true);
        when(execution.getContext()).thenReturn(executionContext);

        StandardURLConfiguration urlConfiguration = mocker.getInstance(StandardURLConfiguration.class);
        when(urlConfiguration.getWikiNotFoundBehavior()).thenReturn(wikiNotFoundBehavior);
    }
}
