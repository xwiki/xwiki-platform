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
package org.xwiki.model.internal.reference;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.model.EntityType;
import org.xwiki.model.ModelConfiguration;
import org.xwiki.model.reference.test.TestConstants;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the deprecated {@link org.xwiki.model.internal.reference.DefaultEntityReferenceValueProvider}.
 *
 * @version $Id$
 * @since 2.3M1
 */
@ComponentTest
public class DefaultEntityReferenceValueProviderTest implements TestConstants
{
    @MockComponent
    private ModelConfiguration configuration;

    @InjectMockComponents
    private DefaultEntityReferenceValueProvider provider;

    @BeforeEach
    public void beforeEach()
    {
        when(this.configuration.getDefaultReferenceValue(EntityType.SPACE)).thenReturn(TestConstants.DEFAULT_SPACE);
        when(this.configuration.getDefaultReferenceValue(EntityType.WIKI)).thenReturn(TestConstants.DEFAULT_WIKI);
        when(this.configuration.getDefaultReferenceValue(EntityType.DOCUMENT)).thenReturn(TestConstants.DEFAULT_DOCUMENT);
        when(this.configuration.getDefaultReferenceValue(EntityType.ATTACHMENT)).thenReturn(
            TestConstants.DEFAULT_ATTACHMENT);
        when(this.configuration.getDefaultReferenceValue(EntityType.PAGE)).thenReturn(TestConstants.DEFAULT_PAGE);
        when(this.configuration.getDefaultReferenceValue(EntityType.PAGE_ATTACHMENT)).thenReturn(
            TestConstants.DEFAULT_ATTACHMENT);
    }

    @Test
    public void testGetDefaultValue()
    {
        assertEquals(TestConstants.DEFAULT_WIKI, this.provider.getDefaultValue(EntityType.WIKI));
        assertEquals(TestConstants.DEFAULT_SPACE, this.provider.getDefaultValue(EntityType.SPACE));
        assertEquals(TestConstants.DEFAULT_DOCUMENT, this.provider.getDefaultValue(EntityType.DOCUMENT));
        assertEquals(TestConstants.DEFAULT_ATTACHMENT, this.provider.getDefaultValue(EntityType.ATTACHMENT));
        assertEquals(TestConstants.DEFAULT_PAGE, this.provider.getDefaultValue(EntityType.PAGE));
        assertEquals(TestConstants.DEFAULT_ATTACHMENT, this.provider.getDefaultValue(EntityType.PAGE_ATTACHMENT));
    }
}
