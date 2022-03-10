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
package org.xwiki.refactoring.internal;

import javax.inject.Named;

import org.junit.jupiter.api.Test;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test of {@link DefaultRefactoringConfiguration}.
 *
 * @version $Id$
 * @since 12.8RC1
 */
@ComponentTest
class DefaultRefactoringConfigurationTest
{
    @InjectMockComponents
    private DefaultRefactoringConfiguration configuration;

    @MockComponent
    @Named("refactoring")
    private ConfigurationSource configurationSource;

    @MockComponent
    @Named("refactoringmainwiki")
    private ConfigurationSource mainWikiConfigurationSource;

    @MockComponent
    @Named("xwikiproperties")
    private ConfigurationSource xwikiPropertiesSource;

    @MockComponent
    private WikiDescriptorManager wikiDescriptorManager;

    @Test
    void isRecycleBinSkippingActivatedOnMainWikiConfigurationDefault()
    {
        when(this.configurationSource.getProperty("isRecycleBinSkippingActivated", Boolean.class)).thenReturn(null);
        when(this.wikiDescriptorManager.isMainWiki(any())).thenReturn(true);
        when(this.xwikiPropertiesSource.getProperty("refactoring.isRecycleBinSkippingActivated", false))
            .thenReturn(false);

        assertFalse(this.configuration.isRecycleBinSkippingActivated());

        verify(this.mainWikiConfigurationSource, never()).getProperty(any(), any());
    }

    @Test
    void isRecycleBinSkippingActivatedOnSubWikiConfigurationDefault()
    {
        when(this.configurationSource.getProperty("isRecycleBinSkippingActivated", Boolean.class)).thenReturn(null);
        when(this.wikiDescriptorManager.isMainWiki(any())).thenReturn(false);
        when(this.mainWikiConfigurationSource.getProperty("isRecycleBinSkippingActivated", Boolean.class))
            .thenReturn(null);
        when(this.xwikiPropertiesSource.getProperty("refactoring.isRecycleBinSkippingActivated", false))
            .thenReturn(false);

        assertFalse(this.configuration.isRecycleBinSkippingActivated());
    }

    @Test
    void isRecycleBinSkippingActivatedOnSubWikiConfigurationSetOnMainWiki()
    {
        when(this.configurationSource.getProperty("isRecycleBinSkippingActivated", Boolean.class)).thenReturn(null);
        when(this.wikiDescriptorManager.isMainWiki(any())).thenReturn(false);
        when(this.mainWikiConfigurationSource.getProperty("isRecycleBinSkippingActivated", Boolean.class))
            .thenReturn(true);

        assertTrue(this.configuration.isRecycleBinSkippingActivated());

        verify(this.xwikiPropertiesSource, never()).getProperty("refactoring.isRecycleBinSkippingActivated", false);
    }

    @Test
    void isRecycleBinSkippingActivatedOnSubWikiConfigurationSetOnSubWiki()
    {
        when(this.configurationSource.getProperty("isRecycleBinSkippingActivated", Boolean.class)).thenReturn(false);

        assertFalse(this.configuration.isRecycleBinSkippingActivated());

        verify(this.wikiDescriptorManager, never()).isMainWiki(any());
        verify(this.mainWikiConfigurationSource, never()).getProperty("isRecycleBinSkippingActivated", Boolean.class);
        verify(this.xwikiPropertiesSource, never()).getProperty("refactoring.isRecycleBinSkippingActivated", false);
    }
}
