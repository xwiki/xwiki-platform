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
package org.xwiki.notifications.internal.rss;

import java.util.Collections;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.xwiki.localization.ContextualLocalizationManager;
import org.xwiki.notifications.notifiers.internal.ModelBridge;
import org.xwiki.notifications.notifiers.internal.rss.DefaultNotificationRSSManager;
import org.xwiki.test.mockito.MockitoComponentMockingRule;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;

import com.rometools.rome.feed.synd.SyndFeed;

/**
 * Unit tests for {@link DefaultNotificationRSSManager}.
 *
 * @version $Id$
 * @since 9.6RC1
 */
public class DefaultNotificationRSSManagerTest
{
    @Rule
    public final MockitoComponentMockingRule<DefaultNotificationRSSManager> mocker =
            new MockitoComponentMockingRule<>(DefaultNotificationRSSManager.class);

    private ContextualLocalizationManager contextualLocalizationManager;

    private ModelBridge modelBridge;

    private WikiDescriptorManager wikiDescriptorManager;

    @Before
    public void setUp() throws Exception
    {
        this.contextualLocalizationManager = this.mocker.registerMockComponent(ContextualLocalizationManager.class);

        this.modelBridge = this.mocker.registerMockComponent(ModelBridge.class);

        this.wikiDescriptorManager = this.mocker.registerMockComponent(WikiDescriptorManager.class);
        Mockito.when(this.wikiDescriptorManager.getCurrentWikiId()).thenReturn("xwiki");
    }

    @Test
    public void renderFeed() throws Exception
    {
        Mockito.when(this.contextualLocalizationManager.getTranslationPlain("notifications.rss.feedTitle"))
                .thenReturn("FeedTitle");
        Mockito.when(this.modelBridge.getDocumentURL(
                ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn("url");
        Mockito.when(this.contextualLocalizationManager.getTranslationPlain("notifications.rss.feedDescription"))
                .thenReturn("FeedDescription");

        SyndFeed feed = this.mocker.getComponentUnderTest().renderFeed(Collections.EMPTY_LIST);

        Assert.assertEquals("FeedTitle", feed.getTitle());
        Assert.assertEquals("FeedDescription", feed.getDescription());
        Assert.assertEquals("url", feed.getLink());
        Assert.assertEquals(0, feed.getEntries().size());
    }
}
