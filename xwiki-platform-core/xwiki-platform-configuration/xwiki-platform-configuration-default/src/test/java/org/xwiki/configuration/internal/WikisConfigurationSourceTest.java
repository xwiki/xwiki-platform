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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.configuration.internal.test.AbstractTestDocumentConfigurationSource;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.test.junit5.mockito.InjectMockComponents;

import com.xpn.xwiki.test.junit5.mockito.OldcoreTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link AbstractWikisConfigurationSource}.
 * 
 * @version $Id$
 */
@OldcoreTest
class WikisConfigurationSourceTest extends AbstractTestDocumentConfigurationSource
{
    public static class TestWikisConfigurationSource extends AbstractWikisConfigurationSource
    {
        @Override
        protected LocalDocumentReference getLocalDocumentReference()
        {
            return CONFIG_DOCREF;
        }

        @Override
        protected LocalDocumentReference getClassReference()
        {
            return CONFIG_CLASSREF;
        }

        @Override
        protected String getCacheId()
        {
            return "configuration.document.myApp";
        }
    }

    @InjectMockComponents
    private TestWikisConfigurationSource wikisConfigSource;

    private static final LocalDocumentReference CONFIG_CLASSREF = new LocalDocumentReference("MyApp", "ConfigClass");

    private static final LocalDocumentReference CONFIG_DOCREF = new LocalDocumentReference("MyApp", "Config");

    private static final DocumentReference SUBWIKI_CONFIG_DOCREF =
        new DocumentReference(CONFIG_DOCREF, new WikiReference(CURRENT_WIKI));

    private static final DocumentReference MAINWIKI_CONFIG_DOCREF =
        new DocumentReference(CONFIG_DOCREF, new WikiReference("xwiki"));

    @Override
    protected ConfigurationSource getConfigurationSource()
    {
        return this.wikisConfigSource;
    }

    @Override
    protected LocalDocumentReference getClassReference()
    {
        return CONFIG_CLASSREF;
    }

    @Override
    @BeforeEach
    public void before() throws Exception
    {
        super.before();

        setStringProperty(SUBWIKI_CONFIG_DOCREF, "color", "blue");
        setStringProperty(MAINWIKI_CONFIG_DOCREF, "color", "red");
        setStringProperty(MAINWIKI_CONFIG_DOCREF, "enabled", "true");

        when(this.mockConverter.convert(Boolean.class, "true")).thenReturn(true);
    }

    @Test
    void containsKey()
    {
        assertFalse(this.wikisConfigSource.containsKey("age"));
        assertTrue(this.wikisConfigSource.containsKey("color"));
        assertTrue(this.wikisConfigSource.containsKey("enabled"));
    }

    @Test
    void getKeys()
    {
        assertEquals(Arrays.asList("color", "enabled"), this.wikisConfigSource.getKeys());
    }

    @Test
    void getProperty()
    {
        assertEquals("blue", this.wikisConfigSource.getProperty("color"));
        assertEquals("true", this.wikisConfigSource.getProperty("enabled"));
        assertNull(this.wikisConfigSource.getProperty("age"));

        assertEquals("blue", this.wikisConfigSource.getProperty("color", "green"));
        assertEquals("18", this.wikisConfigSource.getProperty("age", "18"));

        assertEquals(true, this.wikisConfigSource.getProperty("enabled", Boolean.class));
        assertEquals(Collections.emptyList(), this.wikisConfigSource.getProperty("age", List.class));
    }

    @Test
    void isEmpty() throws Exception
    {
        assertFalse(this.wikisConfigSource.isEmpty());

        removeConfigObject(SUBWIKI_CONFIG_DOCREF);
        assertFalse(this.wikisConfigSource.isEmpty());

        removeConfigObject(MAINWIKI_CONFIG_DOCREF);
        assertTrue(this.wikisConfigSource.isEmpty());
    }
}
