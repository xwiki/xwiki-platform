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
package org.xwiki.mail.internal.configuration;

import javax.inject.Named;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link org.xwiki.mail.internal.configuration.DefaultGeneralMailConfiguration}.
 *
 * @version $Id$
 * @since 12.4RC1
 */
@ComponentTest
public class DefaultGeneralMailConfigurationTest
{
    @InjectMockComponents
    private DefaultGeneralMailConfiguration configuration;

    @MockComponent
    @Named("mailgeneral")
    private ConfigurationSource currentWikiMailConfigDocumentSource;

    @MockComponent
    @Named("mailgeneralmainwiki")
    private ConfigurationSource mainWikiMailConfigDocumentSource;

    @MockComponent
    @Named("xwikiproperties")
    private ConfigurationSource xwikiPropertiesSource;

    @MockComponent
    private WikiDescriptorManager wikiDescriptorManager;

    @BeforeEach
    public void setUp()
    {
        when(this.wikiDescriptorManager.getCurrentWikiId()).thenReturn("mainwiki");
        when(this.wikiDescriptorManager.isMainWiki("mainwiki")).thenReturn(true);
    }

    @Test
    public void obfuscateEmailAddressesWhenNotConfigured()
    {
        // Simulate the default value returned by the call to getProperty() and not the value of the
        // "mail.general.obfuscateEmailAddresses" property which is supposed to be null here...
        when(this.xwikiPropertiesSource.getProperty("mail.general.obfuscateEmailAddresses", false)).thenReturn(false);
        assertFalse(this.configuration.obfuscateEmailAddresses());
    }

    @Test
    public void obfuscateEmailAddressesWhenDefinedInXWikiProperties()
    {
        when(this.xwikiPropertiesSource.getProperty("mail.general.obfuscateEmailAddresses", false)).thenReturn(true);

        assertTrue(this.configuration.obfuscateEmailAddresses());
    }

    @Test
    public void obfuscateEmailAddressesFromMailConfigDocumentInMainWiki()
    {
        when(this.currentWikiMailConfigDocumentSource.getProperty("obfuscateEmailAddresses", Boolean.class))
            .thenReturn(true);

        assertTrue(this.configuration.obfuscateEmailAddresses());
    }

    @Test
    public void obfuscateEmailAddressesFromMailConfigDocumentInSubwikiAndConfigInMainWiki()
    {
        when(this.wikiDescriptorManager.getCurrentWikiId()).thenReturn("subwiki");
        when(this.wikiDescriptorManager.isMainWiki("subwiki")).thenReturn(false);

        when(this.currentWikiMailConfigDocumentSource.getProperty("obfuscateEmailAddresses", Boolean.class))
            .thenReturn(null);
        when(this.mainWikiMailConfigDocumentSource.getProperty("obfuscateEmailAddresses", Boolean.class))
            .thenReturn(true);

        assertTrue(this.configuration.obfuscateEmailAddresses());
    }
}