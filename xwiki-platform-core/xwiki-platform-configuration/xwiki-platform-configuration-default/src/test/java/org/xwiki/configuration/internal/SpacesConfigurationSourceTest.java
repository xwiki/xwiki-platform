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

import javax.inject.Provider;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link org.xwiki.configuration.internal.DocumentsConfigurationSource}.
 *
 * @version $Id$
 */
public class SpacesConfigurationSourceTest
{
    @Rule
    public MockitoComponentMockingRule<SpacesConfigurationSource> mocker =
        new MockitoComponentMockingRule<>(SpacesConfigurationSource.class);

    private Map<String, Map<String, String>> spacesPreferences = new HashMap<>();

    private XWikiContext xcontext;

    @Before
    public void before() throws ComponentLookupException
    {
        this.xcontext = new XWikiContext();
        Provider<XWikiContext> xcontextProvider = this.mocker.getInstance(XWikiContext.TYPE_PROVIDER);
        when(xcontextProvider.get()).thenReturn(xcontext);

        ConfigurationSource spaceConfiguration = this.mocker.getInstance(ConfigurationSource.class, "space");

        when(spaceConfiguration.getProperty(anyString(), same(String.class))).then(new Answer<String>()
        {
            @Override
            public String answer(InvocationOnMock invocation) throws Throwable
            {
                Map<String, String> spacePreferences = getSpacePreferences();
                if (spacePreferences != null) {
                    return spacePreferences.get(invocation.getArgumentAt(0, String.class));
                }

                return null;
            }
        });
        when(spaceConfiguration.getProperty(anyString())).then(new Answer<Object>()
        {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable
            {
                Map<String, String> spacePreferences = getSpacePreferences();
                if (spacePreferences != null) {
                    return spacePreferences.get(invocation.getArgumentAt(0, String.class));
                }

                return null;
            }
        });
        when(spaceConfiguration.getProperty(anyString(), anyString())).then(new Answer<Object>()
        {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable
            {
                Map<String, String> spacePreferences = getSpacePreferences();
                if (spacePreferences != null) {
                    String key = invocation.getArgumentAt(0, String.class);
                    if (spacePreferences.containsKey(key)) {
                        return spacePreferences.get(key);   
                    }
                }

                return invocation.getArgumentAt(1, String.class);
            }
        });

    when(spaceConfiguration.containsKey(anyString())).then(new Answer<Boolean>()
        {
            @Override
            public Boolean answer(InvocationOnMock invocation) throws Throwable
            {
                Map<String, String> spacePreferences = getSpacePreferences();
                if (spacePreferences != null) {
                    return spacePreferences.containsKey(invocation.getArgumentAt(0, String.class));
                }

                return false;
            }
        });
        when(spaceConfiguration.getKeys()).then(new Answer<List<String>>()
        {
            @Override
            public List<String> answer(InvocationOnMock invocation) throws Throwable
            {
                Map<String, String> spacePreferences = getSpacePreferences();
                if (spacePreferences != null) {
                    return new ArrayList<>(spacePreferences.keySet());
                }

                return Collections.emptyList();
            }
        });
        when(spaceConfiguration.isEmpty()).then(new Answer<Boolean>()
        {
            @Override
            public Boolean answer(InvocationOnMock invocation) throws Throwable
            {
                Map<String, String> spacePreferences = getSpacePreferences();
                if (spacePreferences != null) {
                    return spacePreferences.isEmpty();
                }

                return true;
            }
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
    public void containsKey() throws Exception
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

        ConfigurationSource spaces = this.mocker.getComponentUnderTest();

        assertTrue(spaces.isEmpty());
        assertFalse(spaces.containsKey("nopref"));
        assertEquals(Arrays.asList(), spaces.getKeys());
        assertNull(spaces.getProperty("nopref"));
        assertNull(spaces.getProperty("nopref", String.class));
        assertEquals("defaultvalue", spaces.getProperty("nopref", "defaultvalue"));

        xcontext.setDoc(new XWikiDocument(new DocumentReference("document", space1Reference)));
        assertFalse(spaces.isEmpty());
        assertFalse(spaces.containsKey("nopref"));
        assertEquals(Arrays.asList("pref", "pref1"), spaces.getKeys());
        assertNull(spaces.getProperty("nopref"));
        assertNull(spaces.getProperty("nopref", String.class));
        assertEquals("defaultvalue", spaces.getProperty("nopref", "defaultvalue"));
        assertTrue(spaces.containsKey("pref"));
        assertEquals("prefvalue1", spaces.getProperty("pref"));
        assertEquals("prefvalue1", spaces.getProperty("pref", String.class));
        assertEquals("prefvalue1", spaces.getProperty("pref", "defaultvalue"));
        assertTrue(spaces.containsKey("pref1"));
        assertEquals("pref1value1", spaces.getProperty("pref1"));
        assertEquals("pref1value1", spaces.getProperty("pref1", String.class));
        assertFalse(spaces.containsKey("pref2"));
        assertNull(spaces.getProperty("pref2"));

        xcontext.setDoc(new XWikiDocument(new DocumentReference("document", space2Reference)));
        assertFalse(spaces.isEmpty());
        assertFalse(spaces.containsKey("nopref"));
        assertEquals(Arrays.asList("pref", "pref2", "pref1"), spaces.getKeys());
        assertNull(spaces.getProperty("nopref"));
        assertNull(spaces.getProperty("nopref", String.class));
        assertEquals("defaultvalue", spaces.getProperty("nopref", "defaultvalue"));
        assertTrue(spaces.containsKey("pref"));
        assertNull(spaces.getProperty("nopref"));
        assertEquals("defaultvalue", spaces.getProperty("nopref", "defaultvalue"));
        assertTrue(spaces.containsKey("pref1"));
        assertEquals("prefvalue2", spaces.getProperty("pref"));
        assertEquals("prefvalue2", spaces.getProperty("pref", "defaultvalue"));
        assertEquals("pref1value1", spaces.getProperty("pref1"));
        assertTrue(spaces.containsKey("pref2"));
        assertEquals("pref2value2", spaces.getProperty("pref2"));
    }
}
