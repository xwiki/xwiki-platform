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
package org.xwiki.export.pdf.internal.chrome;

import javax.inject.Provider;

import org.junit.jupiter.api.Test;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link ChromeManagerProvider}.
 * 
 * @version $Id$
 */
@ComponentTest
class ChromeManagerProviderTest
{
    @InjectMockComponents
    private ChromeManagerProvider chromeManagerProvider;

    @MockComponent
    private Provider<ChromeManagerManager> chromeManagerManagerProvider;

    @MockComponent
    private WikiDescriptorManager wikiDescriptorManager;

    @Test
    void getAndDispose()
    {
        when(this.wikiDescriptorManager.getCurrentWikiId()).thenReturn("dev", "design", "dev");
        
        ChromeManagerManager devChromeManagerManager = mock(ChromeManagerManager.class, "dev");
        ChromeManager devChromeManager = mock(ChromeManager.class, "dev");
        when(devChromeManagerManager.get()).thenReturn(devChromeManager);
        
        ChromeManagerManager designChromeManagerManager = mock(ChromeManagerManager.class, "design");
        ChromeManager designChromeManager = mock(ChromeManager.class, "design");
        when(designChromeManagerManager.get()).thenReturn(designChromeManager);
        
        when(this.chromeManagerManagerProvider.get()).thenReturn(devChromeManagerManager, designChromeManagerManager);
        
        assertSame(devChromeManager, this.chromeManagerProvider.get());
        assertSame(designChromeManager, this.chromeManagerProvider.get());
        assertSame(devChromeManager, this.chromeManagerProvider.get());
        
        this.chromeManagerProvider.dispose();
        
        verify(devChromeManagerManager).dispose();
        verify(designChromeManagerManager).dispose();
    }
}
