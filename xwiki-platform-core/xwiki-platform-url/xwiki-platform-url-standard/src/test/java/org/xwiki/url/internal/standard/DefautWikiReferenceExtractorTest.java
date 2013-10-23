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

import org.apache.commons.lang3.tuple.Pair;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.EntityReferenceValueProvider;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.test.mockito.MockitoComponentMockingRule;
import org.xwiki.url.internal.ExtendedURL;
import org.xwiki.wiki.descriptor.WikiDescriptor;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link DefaultWikiReferenceExtractor}.
 *
 * @version $Id$
 * @since 5.1M1
 */
public class DefautWikiReferenceExtractorTest
{
    @Rule
    public MockitoComponentMockingRule<DefaultWikiReferenceExtractor> mocker =
        new MockitoComponentMockingRule(DefaultWikiReferenceExtractor.class);

    @Before
    public void setUp() throws Exception
    {
        EntityReferenceValueProvider entityReferenceValueProvider =
            mocker.getInstance(EntityReferenceValueProvider.class);
        when(entityReferenceValueProvider.getDefaultValue(EntityType.WIKI)).thenReturn("xwiki");
    }

    @Test
    public void extractWhenDomainBasedAndNoWikiDescriptorForFullDomain() throws Exception
    {
        setUpConfiguration(false, WikiNotFoundBehavior.REDIRECT_TO_MAIN_WIKI);
        testAndAssert("http://wiki.server.com/xwiki/bin/view/Main/WebHome", "xwiki", false);
    }

    @Test
    public void extractWhenDomainBasedAndNoWikiDescriptorForFullDomainButDescriptorForSubdomain() throws Exception
    {
        setUpConfiguration(false, WikiNotFoundBehavior.REDIRECT_TO_MAIN_WIKI);

        WikiDescriptorManager wikiDescriptorManager = mocker.getInstance(WikiDescriptorManager.class);
        when(wikiDescriptorManager.getByAlias("wiki.server.com")).thenReturn(null);
        when(wikiDescriptorManager.getById("wiki")).thenReturn(new WikiDescriptor("dummy", "dummy"));

        testAndAssert("http://wiki.server.com/xwiki/bin/view/Main/WebHome", "wiki", false);
    }

    @Test
    public void extractWhenDomainBasedAndNoWikiDescriptorButStartsWithWWW() throws Exception
    {
        setUpConfiguration(false, WikiNotFoundBehavior.REDIRECT_TO_MAIN_WIKI);
        testAndAssert("http://www.wiki.com/xwiki/bin/view/Main/WebHome", "xwiki", false);
    }

    @Test
    public void extractWhenDomainBasedAndNoWikiDescriptorButStartsWithLocalhost() throws Exception
    {
        setUpConfiguration(false, WikiNotFoundBehavior.REDIRECT_TO_MAIN_WIKI);
        testAndAssert("http://localhost/xwiki/bin/view/Main/WebHome", "xwiki", false);
    }

    @Test
    public void extractWhenDomainBasedAndNoWikiDescriptorButStartsWithIP() throws Exception
    {
        setUpConfiguration(false, WikiNotFoundBehavior.REDIRECT_TO_MAIN_WIKI);
        testAndAssert("http://192.168.0.10/xwiki/bin/view/Main/WebHome", "xwiki", false);
    }

    @Test
    public void extractWhenDomainBasedAndWikiDescriptor() throws Exception
    {
        setUpConfiguration(false, WikiNotFoundBehavior.REDIRECT_TO_MAIN_WIKI);

        WikiDescriptorManager wikiDescriptorManager = mocker.getInstance(WikiDescriptorManager.class);
        when(wikiDescriptorManager.getByAlias("wiki.server.com")).thenReturn(new WikiDescriptor("wikiid", "wiki"));

        testAndAssert("http://wiki.server.com/xwiki/bin/view/Main/WebHome", "wikiid", false);
    }

    @Test
    public void extractWhenDomainBasedAndNoWikiDescriptorAndDisplayErrorWhenWikiNotFound() throws Exception
    {
        setUpConfiguration(false, WikiNotFoundBehavior.DISPLAY_ERROR);
        testAndAssert("http://wiki.server.com/xwiki/bin/view/Main/WebHome", "wiki", false);
    }

    @Test
    public void extractWhenPathBasedAndWikiDescriptor() throws Exception
    {
        setUpConfiguration(true, WikiNotFoundBehavior.REDIRECT_TO_MAIN_WIKI);

        WikiDescriptorManager wikiDescriptorManager = mocker.getInstance(WikiDescriptorManager.class);
        when(wikiDescriptorManager.getByAlias("someWiki")).thenReturn(new WikiDescriptor("wikiid", "someWiki"));

        testAndAssert("http://localhost/xwiki/wiki/someWiki/view/Main/WebHome", "wikiid", true);
    }

    @Test
    public void extractWhenPathBasedAndNoWikiDescriptor() throws Exception
    {
        setUpConfiguration(true, WikiNotFoundBehavior.REDIRECT_TO_MAIN_WIKI);
        testAndAssert("http://localhost/xwiki/wiki/someWiki/view/Main/WebHome", "xwiki", true);
    }

    @Test
    public void extractWhenPathBasedAndWikiDescriptorButEmptyServerName() throws Exception
    {
        setUpConfiguration(true, WikiNotFoundBehavior.REDIRECT_TO_MAIN_WIKI);

        WikiDescriptorManager wikiDescriptorManager = mocker.getInstance(WikiDescriptorManager.class);
        when(wikiDescriptorManager.getByAlias("someWiki")).thenReturn(new WikiDescriptor("", "someWiki"));

        testAndAssert("http://localhost/xwiki/wiki/someWiki/view/Main/WebHome", "xwiki", true);
    }
    @Test
    public void extractWhenPathBasedAndNoDescriptorMatchingAliasButDescriptorMatchingId() throws Exception
    {
        setUpConfiguration(true, WikiNotFoundBehavior.REDIRECT_TO_MAIN_WIKI);

        WikiDescriptorManager wikiDescriptorManager = mocker.getInstance(WikiDescriptorManager.class);
        when(wikiDescriptorManager.getByAlias("someWiki")).thenReturn(null);
        when(wikiDescriptorManager.getById("someWiki")).thenReturn(new WikiDescriptor("dummy", "dummy"));

        testAndAssert("http://localhost/xwiki/wiki/someWiki/view/Main/WebHome", "somewiki", true);
    }

    @Test
    public void extractWhenPathBasedAndNoWikiDescriptorButWithDomainBasedURL() throws Exception
    {
        setUpConfiguration(true, WikiNotFoundBehavior.REDIRECT_TO_MAIN_WIKI);
        testAndAssert("http://wiki.server.com/xwiki/bin/view/Main/WebHome", "xwiki", false);
    }

    @Test
    public void extractWhenPathBasedAndNoWikiDescriptorAndDisplayErrorWhenWikiNotFound() throws Exception
    {
        setUpConfiguration(true, WikiNotFoundBehavior.DISPLAY_ERROR);
        testAndAssert("http://localhost/xwiki/wiki/someWiki/view/Main/WebHome", "somewiki", true);
    }

    private void testAndAssert(String urlToTest, String expectedWikiId, boolean expectedIsActuallyPathBased)
        throws Exception
    {
        ExtendedURL url = new ExtendedURL(new URL(urlToTest), "xwiki");
        Pair<WikiReference, Boolean> extractionResult = this.mocker.getComponentUnderTest().extract(url);
        assertEquals(new WikiReference(expectedWikiId), extractionResult.getLeft());
        assertTrue(extractionResult.getRight() == expectedIsActuallyPathBased);
    }

    private void setUpConfiguration(boolean isPathBased, WikiNotFoundBehavior wikiNotFoundBehavior) throws Exception
    {
        StandardURLConfiguration urlConfiguration = mocker.getInstance(StandardURLConfiguration.class);
        when(urlConfiguration.isPathBasedMultiWiki()).thenReturn(isPathBased);
        when(urlConfiguration.getWikiPathPrefix()).thenReturn("wiki");
        when(urlConfiguration.getWikiNotFoundBehavior()).thenReturn(wikiNotFoundBehavior);
    }
}
