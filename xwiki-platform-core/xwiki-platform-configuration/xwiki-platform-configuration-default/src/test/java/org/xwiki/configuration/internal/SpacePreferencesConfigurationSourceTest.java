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
package org.xwiki.configuration.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.configuration.internal.test.AbstractTestDocumentConfigurationSource;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.test.junit5.mockito.OldcoreTest;

/**
 * Unit tests for {@link SpacePreferencesConfigurationSource}.
 *
 * @version $Id$
 */
@OldcoreTest
class SpacePreferencesConfigurationSourceTest extends AbstractTestDocumentConfigurationSource
{
    private static final DocumentReference SPACE_DOCUMENT =
        new DocumentReference(CURRENT_WIKI, "currentspace", SpacePreferencesConfigurationSource.DOCUMENT_NAME);

    @InjectMockComponents
    private SpacePreferencesConfigurationSource source;

    @MockComponent
    private DocumentAccessBridge dab;

    @Override
    @BeforeEach
    public void before() throws Exception
    {
        super.before();
        when(this.dab.getCurrentDocumentReference()).thenReturn(SPACE_DOCUMENT);
    }

    @Override
    protected ConfigurationSource getConfigurationSource()
    {
        return this.source;
    }

    @Override
    protected LocalDocumentReference getClassReference()
    {
        return SpacePreferencesConfigurationSource.CLASS_REFERENCE;
    }

    @Test
    void getPropertyForStringWhenExists() throws Exception
    {
        setStringProperty(SPACE_DOCUMENT, "key", "value");

        assertEquals("value", this.source.getProperty("key", String.class));
    }
}
