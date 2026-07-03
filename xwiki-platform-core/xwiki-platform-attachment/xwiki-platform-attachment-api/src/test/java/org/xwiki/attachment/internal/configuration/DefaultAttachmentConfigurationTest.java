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
package org.xwiki.attachment.internal.configuration;

import javax.inject.Named;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test of {@link DefaultAttachmentConfiguration}.
 *
 * @version $Id$
 * @since 16.3.0RC1
 */
@ComponentTest
class DefaultAttachmentConfigurationTest
{
    @InjectMockComponents
    private DefaultAttachmentConfiguration attachmentConfiguration;

    @MockComponent
    @Named("xwikiproperties")
    private ConfigurationSource xWikiPropertiesConfigurationSource;

    private static final String ATTACHMENT_MIMETYPE_ENABLE_COMMENTS_PROPERTY = "attachment.upload.enableComments";

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void areCommentsEnabledInPropertiesConfigurationSource(boolean enabled)
    {
        when(this.xWikiPropertiesConfigurationSource.getProperty(ATTACHMENT_MIMETYPE_ENABLE_COMMENTS_PROPERTY, false))
            .thenReturn(enabled);

        assertEquals(enabled, this.attachmentConfiguration.isCommentsEnabled());
        verify(this.xWikiPropertiesConfigurationSource).getProperty(ATTACHMENT_MIMETYPE_ENABLE_COMMENTS_PROPERTY,
            false);
    }
}
