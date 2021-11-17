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
package org.xwiki.rest.internal.resources.classes;

import java.util.Arrays;
import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.xwiki.localization.ContextualLocalizationManager;
import org.xwiki.rest.model.jaxb.PropertyValue;
import org.xwiki.rest.model.jaxb.PropertyValues;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.objects.classes.StaticListClass;
import com.xpn.xwiki.test.junit5.mockito.OldcoreTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.xwiki.rest.internal.resources.classes.AbstractClassPropertyValuesProvider.META_DATA_LABEL;

/**
 * Test for {@link StaticListClassPropertyValuesProvider}.
 *
 * @since 11.5RC1
 * @version $Id$
 */
@OldcoreTest
public class StaticListClassPropertyValuesProviderTest
{
    @InjectMockComponents
    private StaticListClassPropertyValuesProvider staticListClassPropertyValuesProvider;

    @MockComponent
    private ContextualLocalizationManager localization;

    @MockComponent
    private XWikiContext context;

    @Mock
    private BaseClass baseClass;

    @Test
    void getAllowedValues()
    {
        StaticListClass staticListClass = new StaticListClass();
        staticListClass.setValues("Foo|Bar|Toto|Tata|Foobar");
        PropertyValues allowedValues = this.staticListClassPropertyValuesProvider.getAllowedValues(staticListClass, 1,
            "f");
        assertEquals(Collections.singletonList(new PropertyValue("Foo")), allowedValues.getPropertyValues());

        allowedValues = this.staticListClassPropertyValuesProvider.getAllowedValues(staticListClass, 2, "f");
        assertEquals(Arrays.asList(new PropertyValue("Foo"), new PropertyValue("Foobar")),
            allowedValues.getPropertyValues());

        allowedValues = this.staticListClassPropertyValuesProvider.getAllowedValues(staticListClass, 1, "");
        assertEquals(Collections.singletonList(new PropertyValue("Foo")),
            allowedValues.getPropertyValues());

        allowedValues = this.staticListClassPropertyValuesProvider.getAllowedValues(staticListClass, 5, "");
        assertEquals(Arrays.asList(new PropertyValue("Foo"), new PropertyValue("Bar"), new PropertyValue("Toto"),
                new PropertyValue("Tata"), new PropertyValue("Foobar")),
            allowedValues.getPropertyValues());

        allowedValues = this.staticListClassPropertyValuesProvider.getAllowedValues(staticListClass, 2, "foobar");
        assertEquals(Collections.singletonList(new PropertyValue("Foobar")),
            allowedValues.getPropertyValues());

        allowedValues = this.staticListClassPropertyValuesProvider.getAllowedValues(staticListClass, 1, "baz");
        assertEquals(Collections.emptyList(),
            allowedValues.getPropertyValues());

        allowedValues = this.staticListClassPropertyValuesProvider.getAllowedValues(staticListClass, 3, "o");
        assertEquals(Arrays.asList(new PropertyValue("Foo"), new PropertyValue("Toto"), new PropertyValue("Foobar")),
            allowedValues.getPropertyValues());
    }

    @Test
    void getAllowedValuesWithLabel()
    {
        // Needed to build localization string.
        when(this.baseClass.getName()).thenReturn("XWiki.TestClass");
        // Needed to actually consider the localization in the display code.
        when(this.context.getWiki()).thenReturn(mock(XWiki.class));

        StaticListClass staticListClass = new StaticListClass();
        staticListClass.setName("Test");
        staticListClass.setObject(this.baseClass);

        staticListClass.setValues("Foo|Bar|Toto|Tata|Foobar|Id=Display Value");

        PropertyValues allowedValues =
            this.staticListClassPropertyValuesProvider.getAllowedValues(staticListClass, 3, "iD");
        PropertyValue expected = new PropertyValue("Id");
        expected.getMetaData().put(META_DATA_LABEL, "Display Value");
        assertEquals(Collections.singletonList(expected), allowedValues.getPropertyValues());

        // Test filter by label.
        allowedValues = this.staticListClassPropertyValuesProvider.getAllowedValues(staticListClass, 3, "Display");
        assertEquals(Collections.singletonList(expected), allowedValues.getPropertyValues());

        // Test filter by translated label.
        String translatedLabel = "Translated";
        when(this.localization.getTranslationPlain("XWiki.TestClass_Test_Id")).thenReturn(translatedLabel);
        expected.getMetaData().put(META_DATA_LABEL, translatedLabel);
        allowedValues = this.staticListClassPropertyValuesProvider.getAllowedValues(staticListClass, 3, "ansl");
        assertEquals(Collections.singletonList(expected), allowedValues.getPropertyValues());
    }

    @Test
    void getValueFromQueryResult()
    {
        // Needed to build localization string.
        when(this.baseClass.getName()).thenReturn("XWiki.TestQueryResult");
        // Needed to actually consider the localization in the display code.
        when(this.context.getWiki()).thenReturn(mock(XWiki.class));

        StaticListClass staticListClass = new StaticListClass();
        staticListClass.setName("Query");
        staticListClass.setObject(this.baseClass);
        staticListClass.setValues("Foo|Bar|Toto|Tata|Foobar|Id=Display Value");

        PropertyValue actual = this.staticListClassPropertyValuesProvider.getValueFromQueryResult("Foo",
            staticListClass);
        assertEquals(new PropertyValue("Foo"), actual);

        // For values without definition, the given id should be returned.
        actual = this.staticListClassPropertyValuesProvider.getValueFromQueryResult("Baz", staticListClass);
        assertEquals(new PropertyValue("Baz"), actual);

        actual = this.staticListClassPropertyValuesProvider.getValueFromQueryResult("Id", staticListClass);
        PropertyValue expected = new PropertyValue("Id");
        expected.getMetaData().put(META_DATA_LABEL, "Display Value");
        assertEquals(expected, actual);

        String translatedLabel = "Translated";
        expected.getMetaData().put(META_DATA_LABEL, translatedLabel);
        when(this.localization.getTranslationPlain("XWiki.TestQueryResult_Query_Id")).thenReturn(translatedLabel);

        // Test filter by translated label.
        actual = this.staticListClassPropertyValuesProvider.getValueFromQueryResult("Id", staticListClass);
        assertEquals(expected, actual);
    }

    @Test
    void getValueFromQueryResultWithSpecialCharacter()
    {
        // Needed to build localization string.
        when(this.baseClass.getName()).thenReturn("XWiki.TestQueryResultSpecial");
        // Needed to actually consider the localization in the display code.
        when(this.context.getWiki()).thenReturn(mock(XWiki.class));

        StaticListClass staticListClass = new StaticListClass();
        staticListClass.setName("Query");
        staticListClass.setObject(this.baseClass);

        staticListClass.setValues("Foo&bar|Id=Display & Value");

        PropertyValue actual = this.staticListClassPropertyValuesProvider.getValueFromQueryResult("Foo",
            staticListClass);
        assertEquals(new PropertyValue("Foo"), actual);

        actual = this.staticListClassPropertyValuesProvider.getValueFromQueryResult("Foo&bar", staticListClass);
        assertEquals(new PropertyValue("Foo&bar"), actual);

        actual = this.staticListClassPropertyValuesProvider.getValueFromQueryResult("Id", staticListClass);
        PropertyValue expected = new PropertyValue("Id");
        expected.getMetaData().put(META_DATA_LABEL, "Display & Value");
        assertEquals(expected, actual);

        String translatedLabel = "<Translated & &amp; Label>";
        expected.getMetaData().put(META_DATA_LABEL, translatedLabel);
        when(this.localization.getTranslationPlain("XWiki.TestQueryResultSpecial_Query_Id")).thenReturn(translatedLabel);

        // Test filter by translated label.
        actual = this.staticListClassPropertyValuesProvider.getValueFromQueryResult("Id", staticListClass);
        assertEquals(expected, actual);
    }
}
