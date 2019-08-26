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
import org.xwiki.rest.model.jaxb.PropertyValue;
import org.xwiki.rest.model.jaxb.PropertyValues;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;

import com.xpn.xwiki.objects.classes.StaticListClass;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Test for {@link StaticListClassPropertyValuesProvider}.
 *
 * @since 11.5RC1
 * @version $Id$
 */
@ComponentTest
public class StaticListClassPropertyValuesProviderTest
{
    @InjectMockComponents
    private StaticListClassPropertyValuesProvider staticListClassPropertyValuesProvider;

    @Test
    public void getAllowedValues() throws Exception
    {
        StaticListClass staticListClass = new StaticListClass();
        staticListClass.setValues("Foo|Bar|Toto|Tata|Foobar");
        PropertyValues allowedValues = staticListClassPropertyValuesProvider.getAllowedValues(staticListClass, 1, "f");
        assertEquals(Collections.singletonList(new PropertyValue("Foo")), allowedValues.getPropertyValues());

        allowedValues = staticListClassPropertyValuesProvider.getAllowedValues(staticListClass, 2, "f");
        assertEquals(Arrays.asList(new PropertyValue("Foo"), new PropertyValue("Foobar")),
            allowedValues.getPropertyValues());

        allowedValues = staticListClassPropertyValuesProvider.getAllowedValues(staticListClass, 1, "");
        assertEquals(Collections.singletonList(new PropertyValue("Foo")),
            allowedValues.getPropertyValues());

        allowedValues = staticListClassPropertyValuesProvider.getAllowedValues(staticListClass, 5, "");
        assertEquals(Arrays.asList(new PropertyValue("Foo"), new PropertyValue("Bar"), new PropertyValue("Toto"),
            new PropertyValue("Tata"), new PropertyValue("Foobar")),
            allowedValues.getPropertyValues());

        allowedValues = staticListClassPropertyValuesProvider.getAllowedValues(staticListClass, 2, "foobar");
        assertEquals(Collections.singletonList(new PropertyValue("Foobar")),
            allowedValues.getPropertyValues());

        allowedValues = staticListClassPropertyValuesProvider.getAllowedValues(staticListClass, 1, "baz");
        assertEquals(Collections.emptyList(),
            allowedValues.getPropertyValues());

        allowedValues = staticListClassPropertyValuesProvider.getAllowedValues(staticListClass, 3, "o");
        assertEquals(Arrays.asList(new PropertyValue("Foo"), new PropertyValue("Toto"), new PropertyValue("Foobar")),
            allowedValues.getPropertyValues());
    }
}
