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
package com.xpn.xwiki.objects.classes;

import java.util.Locale;

import org.junit.jupiter.api.Test;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseProperty;
import com.xpn.xwiki.test.MockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.InjectMockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.OldcoreTest;
import com.xpn.xwiki.test.reference.ReferenceComponentList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the {@link NumberClass} class.
 * 
 * @version $Id$
 */
@OldcoreTest
@ReferenceComponentList
public class NumberClassTest
{
    @InjectMockitoOldcore
    public MockitoOldcore oldcore;

    @Test
    void fromStringWithLong()
    {
        // Create a default Number property
        NumberClass nc = new NumberClass();
        BaseClass bc = new BaseClass();
        bc.setName("Some.Class");
        nc.setObject(bc);
        XWikiDocument document = mock(XWikiDocument.class);
        nc.setOwnerDocument(document);
        when(document.getLocale()).thenReturn(Locale.FRENCH);

        // A String value containing non-numeric caracters can not be respresented as a numeric value, so this sould
        // return null
        assertNull(nc.fromString("asd"));

        // A much too long number cannot be represented as a long value, so this should also return null
        assertNull(nc.fromString("1111111111111111111111111111111111"));

        BaseProperty p;

        // A null value should lead to creating an object with an empty value
        p = nc.fromString(null);
        assertNotNull(p);
        assertNull(p.getValue());

        // An empty String should lead to creating an object with an empty value
        p = nc.fromString("");
        assertNotNull(p);
        assertNull(p.getValue());

        // An integer value should lead to creating an object containing that integer as value
        p = nc.fromString("4");
        assertNotNull(p);
        assertEquals(4L, p.getValue());
    }

    @Test
    void fromStringWithFloat()
    {
        // Create a default Number property
        NumberClass nc = new NumberClass();
        BaseClass bc = new BaseClass();
        bc.setName("Some.Class");
        nc.setObject(bc);
        nc.setNumberType(NumberClass.TYPE_FLOAT);
        XWikiDocument document = mock(XWikiDocument.class);
        nc.setOwnerDocument(document);
        when(document.getLocale()).thenReturn(Locale.FRENCH);

        // A String value containing non-numeric caracters can not be respresented as a numeric value, so this sould
        // return null
        assertNull(nc.fromString("asd"));

        BaseProperty p;

        // A null value should lead to creating an object with an empty value
        p = nc.fromString(null);
        assertNotNull(p);
        assertNull(p.getValue());

        // An empty String should lead to creating an object with an empty value
        p = nc.fromString("");
        assertNotNull(p);
        assertNull(p.getValue());

        // Very long values are handled by Float
        p = nc.fromString("1111111111111111111111111111111111");
        assertNotNull(p);
        assertEquals(1.1111111111111111E33f, p.getValue());

        // An integer value should lead to creating an object containing that integer as value
        p = nc.fromString("4");
        assertNotNull(p);
        assertEquals(4.0f, p.getValue());

        // We're using french locale
        p = nc.fromString("4,738");
        assertNotNull(p);
        assertEquals(4.738f, p.getValue());
    }
}
