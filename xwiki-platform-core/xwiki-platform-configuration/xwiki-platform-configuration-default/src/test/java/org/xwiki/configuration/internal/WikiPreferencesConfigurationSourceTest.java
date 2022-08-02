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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.configuration.ConfigurationSaveException;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.configuration.internal.test.AbstractTestDocumentConfigurationSource;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.properties.ConverterManager;
import org.xwiki.properties.converter.ConversionException;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.test.junit5.mockito.OldcoreTest;

/**
 * Unit tests for {@link WikiPreferencesConfigurationSource}.
 * 
 * @version $Id: 31e2e0d488d6f5dbc1fcec1211d30dc30000b5eb
 */
@OldcoreTest
class WikiPreferencesConfigurationSourceTest extends AbstractTestDocumentConfigurationSource
{
    @InjectMockComponents
    private WikiPreferencesConfigurationSource source;

    @MockComponent
    private ConverterManager converterManager;

    @Override
    protected ConfigurationSource getConfigurationSource()
    {
        return this.source;
    }

    @Override
    protected LocalDocumentReference getClassReference()
    {
        return WikiPreferencesConfigurationSource.CLASS_REFERENCE;
    }

    @Override
    @BeforeEach
    public void before() throws Exception
    {
        super.before();
        this.oldcore.getXWikiContext().setLocale(Locale.ENGLISH);
    }

    @Test
    void getProperty() throws Exception
    {
        assertNull(this.source.getProperty("key", String.class));
        assertEquals("default", this.source.getProperty("key", "default"));
        assertNull(this.source.getProperty("key"));
        assertNull(this.source.getProperty("key", Integer.class));

        // Validate result for simple String key

        setStringProperty(new DocumentReference(CURRENT_WIKI, WikiPreferencesConfigurationSource.CLASS_SPACE_NAME,
            WikiPreferencesConfigurationSource.CLASS_PAGE_NAME), "key", "value");

        assertEquals("value", this.source.getProperty("key", String.class));
        assertEquals("value", this.source.getProperty("key", "default"));
        assertEquals("value", this.source.getProperty("key"));

        // Validate result for non existing key

        assertNull(this.source.getProperty("wrongkey", String.class));
        assertEquals("default", this.source.getProperty("wrongkey", "default"));
        assertNull(this.source.getProperty("wrongkey"));
        assertNull(this.source.getProperty("wrongkey", Integer.class));

        // Check that the --- is empty hack "works"

        setStringProperty(new DocumentReference(CURRENT_WIKI, WikiPreferencesConfigurationSource.CLASS_SPACE_NAME,
            WikiPreferencesConfigurationSource.CLASS_PAGE_NAME), "key", WikiPreferencesConfigurationSource.NO_VALUE);

        assertNull(this.source.getProperty("key", String.class));
        assertEquals("default", this.source.getProperty("key", "default"));
        assertNull(this.source.getProperty("key"));
        assertNull(this.source.getProperty("key", Integer.class));
    }

    @Test
    void getPropertyTwiceWithDifferentType() throws Exception
    {
        when(this.mockConverter.convert(String.class, "10")).thenReturn("10");
        when(this.mockConverter.convert(Integer.class, "10")).thenReturn(10);

        setStringProperty(new DocumentReference(CURRENT_WIKI, WikiPreferencesConfigurationSource.CLASS_SPACE_NAME,
            WikiPreferencesConfigurationSource.CLASS_PAGE_NAME), "key", "10");

        assertEquals("10", this.source.getProperty("key", String.class));
        assertEquals((Integer) 10,
            this.source.<Integer>getProperty("key", Integer.class));
    }

    @Test
    void getPropertyWithWrongType() throws Exception
    {
        when(this.mockConverter.convert(Integer.class, "value")).thenThrow(new ConversionException("error"));

        setStringProperty(new DocumentReference(CURRENT_WIKI, WikiPreferencesConfigurationSource.CLASS_SPACE_NAME,
            WikiPreferencesConfigurationSource.CLASS_PAGE_NAME), "key", "value");

        Exception exception = assertThrows(ConversionException.class,
            () -> this.source.getProperty("key", Integer.class));
        assertEquals("error", exception.getMessage());
    }

    @Test
    void getKeys() throws XWikiException
    {
        setStringProperty(new DocumentReference(CURRENT_WIKI, WikiPreferencesConfigurationSource.CLASS_SPACE_NAME,
            WikiPreferencesConfigurationSource.CLASS_PAGE_NAME), "key1", "value");
        setStringProperty(new DocumentReference(CURRENT_WIKI, WikiPreferencesConfigurationSource.CLASS_SPACE_NAME,
            WikiPreferencesConfigurationSource.CLASS_PAGE_NAME), "key2", "value");
        setStringProperty(new DocumentReference(CURRENT_WIKI, WikiPreferencesConfigurationSource.CLASS_SPACE_NAME,
            WikiPreferencesConfigurationSource.CLASS_PAGE_NAME), "emptykey", "");
        setStringProperty(new DocumentReference(CURRENT_WIKI, WikiPreferencesConfigurationSource.CLASS_SPACE_NAME,
            WikiPreferencesConfigurationSource.CLASS_PAGE_NAME), "emptykey2", "---");

        List<String> result = this.source.getKeys();

        assertEquals(new HashSet<>(Arrays.asList("key1", "key2")), new HashSet<>(result));
    }

    @Test
    void containsKey() throws XWikiException
    {
        setStringProperty(new DocumentReference(CURRENT_WIKI, WikiPreferencesConfigurationSource.CLASS_SPACE_NAME,
            WikiPreferencesConfigurationSource.CLASS_PAGE_NAME), "key", "value");

        assertTrue(this.source.containsKey("key"));
        assertFalse(this.source.containsKey("wrongkey"));
    }

    @Test
    void isEmpty() throws XWikiException
    {
        assertTrue(this.source.isEmpty());

        setStringProperty(new DocumentReference(CURRENT_WIKI, WikiPreferencesConfigurationSource.CLASS_SPACE_NAME,
            WikiPreferencesConfigurationSource.CLASS_PAGE_NAME), "key", "value");

        assertFalse(this.source.isEmpty());
    }

    @Test
    void setProperty() throws Exception
    {
        DocumentReference reference = new DocumentReference(CURRENT_WIKI,
            WikiPreferencesConfigurationSource.CLASS_SPACE_NAME, WikiPreferencesConfigurationSource.CLASS_PAGE_NAME);
        this.componentManager.registerMockComponent(EntityReferenceSerializer.TYPE_STRING, "local");
        setupBaseObject(reference, (baseObject -> {}));

        // Since setProperties() will call baseObject.set(), without any indication of the type to set we need to
        // mock the base class too for the type to be found.
        XWikiDocument document = this.oldcore.getXWikiContext().getWiki().getDocument(reference,
            this.oldcore.getXWikiContext());
        BaseClass baseClass = document.getXClass();
        baseClass.addTextField("textKey", "Text Key", 30);
        baseClass.addBooleanField("booleanKey", "Boolean Key");

        Map<String, Object> properties =new HashMap<>();
        properties.put("textKey", "value");
        properties.put("booleanKey", true);
        this.source.setProperties(properties);

        assertEquals("value", this.source.getProperty("textKey"));

        // Simulate the conversion from Integer to Boolean
        when(this.converterManager.convert(Boolean.class, new Integer(1))).thenReturn(true);
        assertEquals(true, this.source.getProperty("booleanKey", Boolean.class));
    }

    @Test
    void setPropertyWhenException() throws Exception
    {
        DocumentReference reference = new DocumentReference(CURRENT_WIKI,
            WikiPreferencesConfigurationSource.CLASS_SPACE_NAME, WikiPreferencesConfigurationSource.CLASS_PAGE_NAME);
        XWikiContext xcontext = this.oldcore.getXWikiContext();
        when(this.oldcore.getXWikiContext().getWiki().getDocument(reference, xcontext)).thenThrow(
            new XWikiException(0, 0, "error"));

        Map<String, Object> properties = new HashMap<>();
        properties.put("key1", "value1");
        properties.put("key2", "value2");
        Exception exception = assertThrows(ConfigurationSaveException.class,
            () -> this.source.setProperties(properties));
        assertEquals("Failed to set properties [[key1],[key2]] in document [XWiki.XWikiPreferences]'s "
            + "[XWiki.XWikiPreferences] xobject", exception.getMessage());
    }
}