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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link org.xwiki.configuration.internal.DocumentsConfigurationSource}.
 *
 * @version $Id$
 */
@ComponentTest
class SpacesConfigurationSourceTest
{
    @InjectMockComponents
    private SpacesConfigurationSource spacesSource;

    @MockComponent
    private Provider<XWikiContext> xcontextProvider;
    
    @MockComponent
    @Named("space")
    private ConfigurationSource spaceConfiguration;
    
    private Map<String, Map<String, String>> spacesPreferences = new HashMap<>();

    private XWikiContext xcontext;

    @BeforeEach
    void before()
    {
        this.xcontext = new XWikiContext();
        when(this.xcontextProvider.get()).thenReturn(this.xcontext);
        when(this.spaceConfiguration.getProperty(any(), same(String.class))).then((Answer<String>) invocation -> {
            Map<String, String> spacePreferences = getSpacePreferences();
            if (spacePreferences != null) {
                return spacePreferences.get(invocation.getArgument(0));
            }
            return null;
        });
        when(spaceConfiguration.getProperty(any())).then(invocation -> {
            Map<String, String> spacePreferences = getSpacePreferences();
            if (spacePreferences != null) {
                return spacePreferences.get(invocation.getArgument(0));
            }
            return null;
        });
        when(spaceConfiguration.getProperty(any(), anyString())).then(invocation -> {
            Map<String, String> spacePreferences = getSpacePreferences();
            if (spacePreferences != null) {
                String key = invocation.getArgument(0);
                if (spacePreferences.containsKey(key)) {
                    return spacePreferences.get(key);
                }
            }
            return invocation.getArgument(1);
        });
        when(spaceConfiguration.containsKey(any())).then((Answer<Boolean>) invocation -> {
            Map<String, String> spacePreferences = getSpacePreferences();
            if (spacePreferences != null) {
                return spacePreferences.containsKey(invocation.getArgument(0));
            }
            return false;
        });
        when(spaceConfiguration.getKeys()).then((Answer<List<String>>) invocation -> {
            Map<String, String> spacePreferences = getSpacePreferences();
            if (spacePreferences != null) {
                return new ArrayList<>(spacePreferences.keySet());
            }
            return Collections.emptyList();
        });
        when(spaceConfiguration.isEmpty()).then((Answer<Boolean>) invocation -> {
            Map<String, String> spacePreferences = getSpacePreferences();
            if (spacePreferences != null) {
                return spacePreferences.isEmpty();
            }
            return true;
        });
    }

    private Map<String, String> getSpacePreferences()
    {
        if (xcontext.getDoc() != null) {
            return spacesPreferences.get(xcontext.getDoc().getDocumentReference().getParent().getName());
        }

        return null;
    }

    @Test
    void containsKey()
    {
        WikiReference wikiReference = new WikiReference("wiki");
        SpaceReference space1Reference = new SpaceReference("space1", wikiReference);
        SpaceReference space2Reference = new SpaceReference("space2", space1Reference);

        Map<String, String> space1Preferences = new LinkedHashMap<>();
        space1Preferences.put("pref", "prefvalue1");
        space1Preferences.put("pref1", "pref1value1");
        Map<String, String> space2Preferences = new LinkedHashMap<>();
        space2Preferences.put("pref", "prefvalue2");
        space2Preferences.put("pref2", "pref2value2");
        this.spacesPreferences.put(space1Reference.getName(), space1Preferences);
        this.spacesPreferences.put(space2Reference.getName(), space2Preferences);

        // Tests

        assertTrue(this.spacesSource.isEmpty());
        assertFalse(this.spacesSource.containsKey("nopref"));
        assertEquals(Arrays.asList(), this.spacesSource.getKeys());
        assertNull(this.spacesSource.getProperty("nopref"));
        assertNull(this.spacesSource.getProperty("nopref", String.class));
        assertEquals("defaultvalue", this.spacesSource.getProperty("nopref", "defaultvalue"));

        this.xcontext.setDoc(new XWikiDocument(new DocumentReference("document", space1Reference)));
        assertFalse(this.spacesSource.isEmpty());
        assertFalse(this.spacesSource.containsKey("nopref"));
        assertEquals(Arrays.asList("pref", "pref1"), this.spacesSource.getKeys());
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

        this.xcontext.setDoc(new XWikiDocument(new DocumentReference("document", space2Reference)));
        assertFalse(this.spacesSource.isEmpty());
        assertFalse(this.spacesSource.containsKey("nopref"));
        assertEquals(Arrays.asList("pref", "pref2", "pref1"), this.spacesSource.getKeys());
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
}
