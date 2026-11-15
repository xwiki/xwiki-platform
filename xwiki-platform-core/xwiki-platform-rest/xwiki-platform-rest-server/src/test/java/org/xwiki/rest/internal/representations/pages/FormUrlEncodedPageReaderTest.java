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
package org.xwiki.rest.internal.representations.pages;

import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;

import org.junit.jupiter.api.Test;
import org.xwiki.rest.JAXRSUtils;
import org.xwiki.rest.model.jaxb.Page;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Unit test for {@link FormUrlEncodedPageReader}.
 *
 * @version $Id$
 */
@ComponentTest
class FormUrlEncodedPageReaderTest
{
    @InjectMockComponents
    private FormUrlEncodedPageReader reader;

    @MockComponent
    private JAXRSUtils jaxrs;

    @Test
    void readsFieldsAndHiddenTrue() throws Exception
    {
        MultivaluedMap<String, String> form = new MultivaluedHashMap<>();
        form.putSingle("title", "My Title");
        form.putSingle("parent", "Space.Parent");
        form.putSingle("content", "My Content");
        form.putSingle("hidden", "true");
        when(this.jaxrs.readForm(any(), any(), any())).thenReturn(form);

        Page page = this.reader.readFrom(Page.class, null, null, null, null, null);

        assertEquals("My Title", page.getTitle());
        assertEquals("Space.Parent", page.getParent());
        assertEquals("My Content", page.getContent());
        assertTrue(page.isHidden());
    }

    @Test
    void hiddenDefaultsToFalseWhenAbsent() throws Exception
    {
        // Pin current behavior: Boolean.valueOf(null) is false, so a page whose form has no
        // "hidden" field is treated as not hidden.
        MultivaluedMap<String, String> form = new MultivaluedHashMap<>();
        when(this.jaxrs.readForm(any(), any(), any())).thenReturn(form);

        Page page = this.reader.readFrom(Page.class, null, null, null, null, null);

        assertFalse(page.isHidden());
    }
}
