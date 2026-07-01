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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Named;
import javax.inject.Provider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.stubbing.Answer;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link org.xwiki.configuration.internal.DocumentsConfigurationSource}.
 *
 * @version $Id$
 */
@ComponentTest
class SpacesConfigurationSourceTest
{
    private static final WikiReference WIKI_REFERENCE = new WikiReference("wiki");

    private static final SpaceReference SPACE_REFERENCE = new SpaceReference("space1", WIKI_REFERENCE);

    private static final SpaceReference SUBSPACE_REFERENCE = new SpaceReference("space2", SPACE_REFERENCE);

    @InjectMockComponents
    private SpacesConfigurationSource spacesSource;

    @MockComponent
    private Provider<XWikiContext> xcontextProvider;

    @MockComponent
    @Named("space")
    private ConfigurationSource spaceConfiguration;

    private final MapConfigurationSource spaceMap = new MapConfigurationSource();

    private final MapConfigurationSource subSpaceMap = new MapConfigurationSource();

    private final MapConfigurationSource noMap = new MapConfigurationSource();

    private final Map<SpaceReference, MapConfigurationSource> spaceConfigurations =
        Map.of(SPACE_REFERENCE, this.spaceMap, SUBSPACE_REFERENCE, this.subSpaceMap);

    private XWikiContext xcontext;

    @BeforeEach
    void before()
    {
        this.xcontext = new XWikiContext();
        when(this.xcontextProvider.get()).thenReturn(this.xcontext);

        when(this.spaceConfiguration.getProperty(any())).then((Answer<?>) invocation -> {
            return getSpaceConfiguration().getProperty(invocation.getArgument(0));
        });
        when(this.spaceConfiguration.getProperty(any(), any(Object.class))).then((Answer<?>) invocation -> {
            return getSpaceConfiguration().getProperty(invocation.getArgument(0), invocation.<Object>getArgument(1));
        });
        when(this.spaceConfiguration.getProperty(any(), any(Class.class))).then((Answer<?>) invocation -> {
            return getSpaceConfiguration().getProperty(invocation.getArgument(0), invocation.<Class<?>>getArgument(1));
        });
        when(this.spaceConfiguration.getProperty(any(), any(), any())).then((Answer<?>) invocation -> {
            return getSpaceConfiguration().getProperty(invocation.getArgument(0), invocation.getArgument(1),
                invocation.getArgument(2));
        });
        when(this.spaceConfiguration.containsKey(any())).then((Answer<Boolean>) invocation -> {
            return getSpaceConfiguration().containsKey(invocation.getArgument(0));
        });
        when(this.spaceConfiguration.getKeys()).then((Answer<List<String>>) invocation -> {
            return getSpaceConfiguration().getKeys();
        });
        when(this.spaceConfiguration.getKeys(any())).then((Answer<List<String>>) invocation -> {
            return getSpaceConfiguration().getKeys(invocation.getArgument(0));
        });
        when(this.spaceConfiguration.isEmpty()).then((Answer<Boolean>) invocation -> {
            return getSpaceConfiguration().isEmpty();
        });
        when(this.spaceConfiguration.isEmpty(any())).then((Answer<Boolean>) invocation -> {
            return getSpaceConfiguration().isEmpty(invocation.getArgument(0));
        });

        this.spaceMap.setProperty("pref", "prefvalue1");
        this.spaceMap.setProperty("pref1", "pref1value1");
        this.subSpaceMap.setProperty("pref", "prefvalue2");
        this.subSpaceMap.setProperty("pref2", "pref2value2");
    }

    private ConfigurationSource getSpaceConfiguration()
    {
        if (this.xcontext.getDoc() != null) {
            MapConfigurationSource map =
                this.spaceConfigurations.get(this.xcontext.getDoc().getDocumentReference().getParent());
            if (map != null) {
                return map;
            }
        }

        return this.noMap;
    }

    private void switchTo(SpaceReference reference)
    {
        this.xcontext.setDoc(new XWikiDocument(new DocumentReference("document", reference)));
    }

    private void assertKeys(Set<String> expected, List<String> actual)
    {
        assertEquals(expected, new HashSet<>(actual));        
    }
    
    @Test
    void containsKey()
    {
        assertFalse(this.spacesSource.containsKey("nopref"));
        assertNull(this.spacesSource.getProperty("nopref"));
        assertNull(this.spacesSource.getProperty("nopref", String.class));
        assertEquals("defaultvalue", this.spacesSource.getProperty("nopref", "defaultvalue"));

        switchTo(SPACE_REFERENCE);
        assertFalse(this.spacesSource.containsKey("nopref"));
        assertNull(this.spacesSource.getProperty("nopref"));
        assertNull(this.spacesSource.getProperty("nopref", String.class));
        assertEquals("defaultvalue", this.spacesSource.getProperty("nopref", "defaultvalue"));
        assertTrue(this.spacesSource.containsKey("pref"));
        assertEquals("prefvalue1", this.spacesSource.getProperty("pref"));
        assertEquals("prefvalue1", this.spacesSource.getProperty("pref", String.class));
        assertEquals("prefvalue1", this.spacesSource.getProperty("pref", "defaultvalue"));
        assertTrue(this.spacesSource.containsKey("pref1"));
        assertEquals("pref1value1", this.spacesSource.getProperty("pref1"));
        assertEquals("pref1value1", this.spacesSource.getProperty("pref1", String.class));
        assertFalse(this.spacesSource.containsKey("pref2"));
        assertNull(this.spacesSource.getProperty("pref2"));

        switchTo(SUBSPACE_REFERENCE);
        assertFalse(this.spacesSource.containsKey("nopref"));
        assertNull(this.spacesSource.getProperty("nopref"));
        assertNull(this.spacesSource.getProperty("nopref", String.class));
        assertEquals("defaultvalue", this.spacesSource.getProperty("nopref", "defaultvalue"));
        assertTrue(this.spacesSource.containsKey("pref"));
        assertNull(this.spacesSource.getProperty("nopref"));
        assertEquals("defaultvalue", this.spacesSource.getProperty("nopref", "defaultvalue"));
        assertTrue(this.spacesSource.containsKey("pref1"));
        assertEquals("prefvalue2", this.spacesSource.getProperty("pref"));
        assertEquals("prefvalue2", this.spacesSource.getProperty("pref", "defaultvalue"));
        assertEquals("pref1value1", this.spacesSource.getProperty("pref1"));
        assertTrue(this.spacesSource.containsKey("pref2"));
        assertEquals("pref2value2", this.spacesSource.getProperty("pref2"));
    }

    @Test
    void isEmpty()
    {
        assertTrue(this.spacesSource.isEmpty());
        assertTrue(this.spacesSource.isEmpty("pref1"));
        assertTrue(this.spacesSource.isEmpty("other"));

        switchTo(SPACE_REFERENCE);
        assertFalse(this.spacesSource.isEmpty());
        assertFalse(this.spacesSource.isEmpty("pref1"));
        assertTrue(this.spacesSource.isEmpty("other"));

        switchTo(SUBSPACE_REFERENCE);
        assertFalse(this.spacesSource.isEmpty());
        assertFalse(this.spacesSource.isEmpty("pref2"));
        assertTrue(this.spacesSource.isEmpty("other"));
    }

    @Test
    void getKeys()
    {
        assertKeys(Set.of(), this.spacesSource.getKeys());
        assertEquals(Arrays.asList(), this.spacesSource.getKeys());
        assertEquals(Arrays.asList(), this.spacesSource.getKeys("pref"));
        assertEquals(Arrays.asList(), this.spacesSource.getKeys("other"));

        switchTo(SPACE_REFERENCE);
        assertKeys(Set.of("pref", "pref1"), this.spacesSource.getKeys());
        assertKeys(Set.of("pref1"), this.spacesSource.getKeys("pref1"));
        assertKeys(Set.of(), this.spacesSource.getKeys("other"));

        switchTo(SUBSPACE_REFERENCE);
        assertKeys(Set.of("pref", "pref2", "pref1"), this.spacesSource.getKeys());
        assertKeys(Set.of("pref2"), this.spacesSource.getKeys("pref2"));
        assertKeys(Set.of(), this.spacesSource.getKeys("other"));
    }
}
