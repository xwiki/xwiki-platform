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
package org.xwiki.user.internal;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.user.Editor;
import org.xwiki.user.UserType;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link DefaultUserProperties}.
 *
 * @version $Id$
 */
@ComponentTest
public class DefaultUserPropertiesTest
{
    @MockComponent
    private ConfigurationSource configurationSource;

    private DefaultUserProperties user;

    @BeforeEach
    void setup()
    {
        this.user = new DefaultUserProperties(this.configurationSource);
    }

    @Test
    void isActive()
    {
        when(this.configurationSource.getProperty("active", Boolean.class, false)).thenReturn(true);
        assertTrue(this.user.isActive());

        when(this.configurationSource.getProperty("active", Boolean.class, false)).thenReturn(false);
        assertFalse(this.user.isActive());
    }

    @Test
    void displayHiddenDocuments()
    {
        when(this.configurationSource.getProperty("displayHiddenDocuments", Boolean.class, false))
            .thenReturn(true);
        assertTrue(user.displayHiddenDocuments());

        when(this.configurationSource.getProperty("displayHiddenDocuments", Boolean.class, false))
            .thenReturn(false);
        assertFalse(user.displayHiddenDocuments());
    }

    @Test
    void getFirstName()
    {
        when(this.configurationSource.getProperty("first_name")).thenReturn("John");
        assertEquals("John", this.user.getFirstName());
    }

    @Test
    void getLastName()
    {
        when(this.configurationSource.getProperty("last_name")).thenReturn("Doe");
        assertEquals("Doe", this.user.getLastName());
    }

    @Test
    void getEmail()
    {
        when(this.configurationSource.getProperty("email")).thenReturn("john@doe.com");
        assertEquals("john@doe.com", this.user.getEmail());
    }

    @Test
    void getEmailWhenNull()
    {
        when(this.configurationSource.getProperty("email")).thenReturn(null);
        assertNull(this.user.getEmail());
    }

    @Test
    void getUserType()
    {
        when(this.configurationSource.getProperty("usertype")).thenReturn("advanced");
        assertEquals(UserType.ADVANCED, this.user.getType());
    }

    @Test
    void getEditor()
    {
        when(this.configurationSource.getProperty("editor")).thenReturn("Wysiwyg");
        assertEquals(Editor.WYSIWYG, this.user.getEditor());
    }

    @Test
    void getProperty()
    {
        when(this.configurationSource.getProperty("property")).thenReturn("value");
        assertEquals("value", this.user.getProperty("property"));
    }

    @Test
    void isEmailChecked()
    {
        when(this.configurationSource.getProperty("email_checked", Boolean.class, false)).thenReturn(true);
        assertTrue(this.user.isEmailChecked());

        when(this.configurationSource.getProperty("email_checked", Boolean.class, false)).thenReturn(false);
        assertFalse(this.user.isEmailChecked());
    }

    @Test
    void setPropertiesUsingTypedMethods() throws Exception
    {
        this.user.setActive(true);
        this.user.setFirstName("firstname");
        this.user.setLastName("lastname");
        this.user.setEditor(Editor.WYSIWYG);
        this.user.setEmail("john@doe.com");
        this.user.setEmailChecked(true);
        this.user.setType(UserType.ADVANCED);
        this.user.setDisplayHiddenDocuments(true);

        doAnswer(invocation -> {
            Map<String, Object> map = (Map<String, Object>) invocation.getArguments()[0];
            assertEquals(8, map.size());
            assertTrue((boolean) map.get("active"));
            assertEquals("firstname",map.get("first_name"));
            assertEquals("lastname", map.get("last_name"));
            assertEquals(Editor.WYSIWYG, map.get("editor"));
            assertTrue((boolean) map.get("displayHiddenDocuments"));
            assertEquals("john@doe.com", map.get("email"));
            assertTrue((boolean) map.get("email_checked"));
            assertEquals(UserType.ADVANCED, map.get("usertype"));
            return null;
        }).when(this.configurationSource).setProperties(any(Map.class));

        this.user.save();

        // Call save again and verify no properties are passed this time since they have already been saved.

        doAnswer(invocation -> {
            Map<String, Object> map = (Map<String, Object>) invocation.getArguments()[0];
            assertEquals(0, map.size());
            return null;
        }).when(this.configurationSource).setProperties(any(Map.class));

        this.user.save();
    }

    @Test
    void setProperties() throws Exception
    {
        doAnswer(invocation -> {
            Map<String, Object> map = (Map<String, Object>) invocation.getArguments()[0];
            assertEquals(2, map.size());
            assertEquals("firstname",map.get("first_name"));
            assertEquals("lastname", map.get("last_name"));
            return null;
        }).when(this.configurationSource).setProperties(any(Map.class));

        Map<String, Object> map = new HashMap<>();
        map.put("first_name", "firstname");
        map.put("last_name", "lastname");
        this.user.setProperties(map);
    }
}
