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
package org.xwiki.platform.blog.internal;

import java.util.Arrays;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.extension.ExtensionId;
import org.xwiki.extension.InstalledExtension;
import org.xwiki.extension.event.ExtensionUpgradedEvent;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.platform.blog.BlogVisibilityMigration;
import org.xwiki.test.mockito.MockitoComponentMockingRule;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

/**
 * @version $Id$
 */
public class BlogUpgradeEventListenerTest
{
    @Rule
    public MockitoComponentMockingRule<BlogUpgradeEventListener> mocker =
            new MockitoComponentMockingRule<>(BlogUpgradeEventListener.class);

    private BlogVisibilityMigration blogVisibilityMigration;
    private WikiDescriptorManager wikiDescriptorManager;

    @Before
    public void setUp() throws Exception
    {
        blogVisibilityMigration = mocker.getInstance(BlogVisibilityMigration.class);
        wikiDescriptorManager = mocker.getInstance(WikiDescriptorManager.class);
    }

    private void testWithVersion(String version, boolean executedExpected) throws Exception
    {
        // Mocks
        InstalledExtension installedExtension1 = mock(InstalledExtension.class);
        InstalledExtension installedExtension2 = mock(InstalledExtension.class);
        InstalledExtension installedExtension3 = mock(InstalledExtension.class);

        ExtensionUpgradedEvent event = new ExtensionUpgradedEvent(
                new ExtensionId("org.xwiki.platform:xwiki-platform-blog-ui", "9.0"), "wiki:chocolate");

        when(installedExtension1.getId()).thenReturn(new ExtensionId("foo", "8"));
        when(installedExtension2.getId()).thenReturn(new ExtensionId("bar", "9.0"));
        when(installedExtension2.getId()).thenReturn(new ExtensionId("org.xwiki.platform:xwiki-platform-blog-ui",
                version));

        // Test
        mocker.getComponentUnderTest().onEvent(event, null,
                Arrays.asList(installedExtension1, installedExtension2, installedExtension3));

        // Verify
        if (executedExpected) {
            verify(blogVisibilityMigration).execute(eq(new WikiReference("chocolate")));
        } else {
            verifyZeroInteractions(blogVisibilityMigration);
        }
    }

    @Test
    public void onEventWithVersion82() throws Exception
    {
        testWithVersion("8.2", true);
    }

    @Test
    public void onEventWithVersion51() throws Exception
    {
        testWithVersion("5.1", true);
    }

    @Test
    public void onEventWithVersion746() throws Exception
    {
        testWithVersion("7.4.6", false);
    }

    @Test
    public void onEventWithVersion842() throws Exception
    {
        testWithVersion("8.4.2", false);
    }

    @Test
    public void onEventWithVersion90() throws Exception
    {
        testWithVersion("9.0", false);
    }

    @Test
    public void onEventWithNoBlogInstalled() throws Exception
    {
        // Mocks
        InstalledExtension installedExtension1 = mock(InstalledExtension.class);

        ExtensionUpgradedEvent event = new ExtensionUpgradedEvent(
                new ExtensionId("org.xwiki.platform:xwiki-platform-blog-ui", "9.0"), "wiki:chocolate");

        when(installedExtension1.getId()).thenReturn(new ExtensionId("foobar", "8"));

        // Test
        mocker.getComponentUnderTest().onEvent(event, null, Arrays.asList(installedExtension1));

        // Verify
        verifyZeroInteractions(blogVisibilityMigration);
    }

    @Test
    public void onEventWithNoNamespace() throws Exception
    {
        // Mocks
        InstalledExtension installedExtension1 = mock(InstalledExtension.class);

        ExtensionUpgradedEvent event = new ExtensionUpgradedEvent(
                new ExtensionId("org.xwiki.platform:xwiki-platform-blog-ui", "9.0"), null);

        when(installedExtension1.getId()).thenReturn(new ExtensionId("org.xwiki.platform:xwiki-platform-blog-ui",
                "2.3"));

        when(wikiDescriptorManager.getAllIds()).thenReturn(Arrays.asList("wiki1", "wiki2"));

        // Test
        mocker.getComponentUnderTest().onEvent(event, null, Arrays.asList(installedExtension1));

        // Verify
        verify(blogVisibilityMigration).execute(eq(new WikiReference("wiki1")));
        verify(blogVisibilityMigration).execute(eq(new WikiReference("wiki2")));
    }
}
