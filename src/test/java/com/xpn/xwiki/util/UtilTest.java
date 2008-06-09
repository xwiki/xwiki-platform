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
 *
 */
package com.xpn.xwiki.util;

import junit.framework.TestCase;

/**
 * Unit tests for {@link com.xpn.xwiki.util.Util}.
 *
 * @version $Id$
 */
public class UtilTest extends TestCase
{
    public void testConvertToAlphaNumeric()
    {
        assertEquals("Search", Util.convertToAlphaNumeric("Search"));
        assertEquals("Search", Util.convertToAlphaNumeric("S.earch"));
        assertEquals("e", Util.convertToAlphaNumeric("e$%£#^()"));
        assertEquals("e", Util.convertToAlphaNumeric(":\u0205!"));
    }

    public void testIsValidXMLElementName()
    {
        assertTrue(Util.isValidXMLElementName("myprop"));
        assertTrue(Util.isValidXMLElementName("my_prop"));
        assertTrue(Util.isValidXMLElementName("my_prop-1"));
        assertTrue(Util.isValidXMLElementName("ns:my_prop-1"));
        assertTrue(Util.isValidXMLElementName("ns:my_prop-1.1"));
        assertFalse(Util.isValidXMLElementName("1prop"));        
        assertFalse(Util.isValidXMLElementName("xmlprop"));
        assertFalse(Util.isValidXMLElementName("XMLprop"));
        assertFalse(Util.isValidXMLElementName("xMLprop"));
        assertFalse(Util.isValidXMLElementName("Xmlprop"));
        assertFalse(Util.isValidXMLElementName("ns my_prop"));
        assertFalse(Util.isValidXMLElementName("ns,my_prop"));
    }
}
