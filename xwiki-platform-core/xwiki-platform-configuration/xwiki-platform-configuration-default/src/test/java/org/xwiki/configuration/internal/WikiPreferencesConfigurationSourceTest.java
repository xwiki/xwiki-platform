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

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.when;

import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;

import org.junit.Assert;
import org.junit.Test;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.configuration.internal.test.AbstractTestDocumentConfigurationSource;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.properties.converter.ConversionException;

import com.xpn.xwiki.XWikiException;

/**
 * Unit tests for {@link WikiPreferencesConfigurationSource}.
 * 
 * @version $Id: 31e2e0d488d6f5dbc1fcec1211d30dc30000b5eb
 */
public class WikiPreferencesConfigurationSourceTest extends AbstractTestDocumentConfigurationSource
{
    public WikiPreferencesConfigurationSourceTest()
    {
        super(WikiPreferencesConfigurationSource.class);
    }

    @Override
    protected LocalDocumentReference getClassReference()
    {
        return WikiPreferencesConfigurationSource.CLASS_REFERENCE;
    }

    @Override
    public void before() throws Exception
    {
        super.before();

        this.oldcore.getXWikiContext().setLocale(Locale.ENGLISH);
    }

    // Tests

    @Test
    public void testGetProperty() throws Exception
    {
        Assert.assertEquals(null, this.componentManager.getComponentUnderTest().getProperty("key", String.class));
        Assert.assertEquals("default", this.componentManager.getComponentUnderTest().getProperty("key", "default"));
        Assert.assertEquals(null, this.componentManager.getComponentUnderTest().getProperty("key"));
        Assert.assertEquals(null, this.componentManager.getComponentUnderTest().getProperty("key", Integer.class));

        setStringProperty(new DocumentReference(CURRENT_WIKI, WikiPreferencesConfigurationSource.CLASS_SPACE_NAME,
            WikiPreferencesConfigurationSource.CLASS_PAGE_NAME), "key", "value");

        Assert.assertEquals("value", this.componentManager.getComponentUnderTest().getProperty("key", String.class));
        Assert.assertEquals("value", this.componentManager.getComponentUnderTest().getProperty("key", "default"));
        Assert.assertEquals("value", this.componentManager.getComponentUnderTest().getProperty("key"));

        Assert.assertEquals(null, this.componentManager.getComponentUnderTest().getProperty("wrongkey", String.class));
        Assert
            .assertEquals("default", this.componentManager.getComponentUnderTest().getProperty("wrongkey", "default"));
        Assert.assertEquals(null, this.componentManager.getComponentUnderTest().getProperty("wrongkey"));
        Assert.assertEquals(null, this.componentManager.getComponentUnderTest().getProperty("wrongkey", Integer.class));
    }

    @Test(expected = ConversionException.class)
    public void testGetPropertyWithWrongType() throws Exception
    {
        when(this.mockConverter.convert(Integer.class, "value")).thenThrow(ConversionException.class);

        setStringProperty(new DocumentReference(CURRENT_WIKI, WikiPreferencesConfigurationSource.CLASS_SPACE_NAME,
            WikiPreferencesConfigurationSource.CLASS_PAGE_NAME), "key", "value");

        Assert.assertEquals("values", this.componentManager.getComponentUnderTest().getProperty("key", Integer.class));
    }

    @Test
    public void testGetKeys() throws ComponentLookupException, XWikiException
    {
        setStringProperty(new DocumentReference(CURRENT_WIKI, WikiPreferencesConfigurationSource.CLASS_SPACE_NAME,
            WikiPreferencesConfigurationSource.CLASS_PAGE_NAME), "key1", "value");
        setStringProperty(new DocumentReference(CURRENT_WIKI, WikiPreferencesConfigurationSource.CLASS_SPACE_NAME,
            WikiPreferencesConfigurationSource.CLASS_PAGE_NAME), "key2", "value");

        List<String> result = this.componentManager.getComponentUnderTest().getKeys();

        Assert.assertEquals(new HashSet<String>(Arrays.asList("key1", "key2")), new HashSet<String>(result));
    }

    @Test
    public void testContainsKey() throws XWikiException, ComponentLookupException
    {
        setStringProperty(new DocumentReference(CURRENT_WIKI, WikiPreferencesConfigurationSource.CLASS_SPACE_NAME,
            WikiPreferencesConfigurationSource.CLASS_PAGE_NAME), "key", "value");

        Assert.assertTrue(this.componentManager.getComponentUnderTest().containsKey("key"));
        Assert.assertFalse(this.componentManager.getComponentUnderTest().containsKey("wrongkey"));
    }

    @Test
    public void testIsEmpty() throws ComponentLookupException, XWikiException
    {
        Assert.assertTrue(this.componentManager.getComponentUnderTest().isEmpty());

        setStringProperty(new DocumentReference(CURRENT_WIKI, WikiPreferencesConfigurationSource.CLASS_SPACE_NAME,
            WikiPreferencesConfigurationSource.CLASS_PAGE_NAME), "key", "value");

        Assert.assertFalse(this.componentManager.getComponentUnderTest().isEmpty());
    }
}
