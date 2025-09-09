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

import org.junit.Rule;
import org.junit.Test;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.objects.BaseProperty;
import com.xpn.xwiki.test.MockitoOldcoreRule;
import com.xpn.xwiki.test.reference.ReferenceComponentList;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;

/**
 * Unit tests for the {@link NumberClass} class.
 * 
 * @version $Id$
 */
@ReferenceComponentList
public class NumberClassTest
{
    @Rule
    public MockitoOldcoreRule oldcore = new MockitoOldcoreRule();

    /** Test the fromString method. */
    @Test
    public void testFromString() throws XWikiException
    {
        // Create a default Number property
        NumberClass nc = new NumberClass();
        BaseClass bc = new BaseClass();
        bc.setName("Some.Class");
        nc.setObject(bc);

        // A String value containing non-numeric caracters can not be respresented as a numeric value, so this sould
        // throw an exception
        XWikiException xWikiException = assertThrows(XWikiException.class, () -> nc.fromString("asd"));
        assertEquals("Error number 0 in 0: Error when parsing [asd] to type [long]",  xWikiException.getMessage());

        // A much too long number cannot be represented as a long value, so this should throw an exception
        xWikiException = assertThrows(XWikiException.class, () -> nc.fromString("1111111111111111111111111111111111"));
        assertEquals("Error number 0 in 0: Error when parsing [1111111111111111111111111111111111] to type [long]",
            xWikiException.getMessage());

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
        assertEquals(4, Integer.parseInt(p.getValue().toString()));
    }
}
