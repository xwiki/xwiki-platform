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
package org.xwiki.rest.internal.representations.objects;

import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;

import org.junit.jupiter.api.Test;
import org.xwiki.rest.JAXRSUtils;
import org.xwiki.rest.model.jaxb.Object;
import org.xwiki.rest.model.jaxb.Property;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Unit test for {@link FormUrlEncodedObjectReader}.
 *
 * @version $Id$
 */
@ComponentTest
class FormUrlEncodedObjectReaderTest
{
    @InjectMockComponents
    private FormUrlEncodedObjectReader reader;

    @MockComponent
    private JAXRSUtils jaxrs;

    @Test
    void readsClassNameAndProperties() throws Exception
    {
        MultivaluedMap<String, String> form = new MultivaluedHashMap<>();
        form.putSingle("className", "XWiki.MyClass");
        form.putSingle("property#title", "Hello");
        when(this.jaxrs.readForm(any(), any(), any())).thenReturn(form);

        Object object = this.reader.readFrom(Object.class, null, null, null, null, null);

        assertEquals("XWiki.MyClass", object.getClassName());
        assertEquals(1, object.getProperties().size());
        Property property = object.getProperties().get(0);
        assertEquals("title", property.getName());
        assertEquals("Hello", property.getValue());
    }

    @Test
    void readsMultipleProperties() throws Exception
    {
        MultivaluedMap<String, String> form = new MultivaluedHashMap<>();
        form.putSingle("className", "XWiki.MyClass");
        form.putSingle("property#title", "Hello");
        form.putSingle("property#author", "John");
        when(this.jaxrs.readForm(any(), any(), any())).thenReturn(form);

        Object object = this.reader.readFrom(Object.class, null, null, null, null, null);

        assertEquals(2, object.getProperties().size());
    }

    @Test
    void ignoresFieldsThatAreNotPropertyPrefixed() throws Exception
    {
        MultivaluedMap<String, String> form = new MultivaluedHashMap<>();
        form.putSingle("className", "XWiki.MyClass");
        form.putSingle("notAProperty", "ignored");
        when(this.jaxrs.readForm(any(), any(), any())).thenReturn(form);

        Object object = this.reader.readFrom(Object.class, null, null, null, null, null);

        assertEquals(0, object.getProperties().size());
    }
}
