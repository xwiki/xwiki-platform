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
package com.xpn.xwiki.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.AssertionsKt.assertNull;

/**
 * Unit tests for {@link com.xpn.xwiki.util.Util}.
 *
 * @version $Id$
 */
class UtilTest
{
    @Test
    void convertToAlphaNumeric()
    {
        assertEquals("Search", Util.convertToAlphaNumeric("Search"));
        assertEquals("Search", Util.convertToAlphaNumeric("S.earch"));
        assertEquals("e", Util.convertToAlphaNumeric("e$%\u00a3#^()"));
        assertEquals("e", Util.convertToAlphaNumeric(":\u0205!"));
    }

    @Test
    void isValidXMLElementName()
    {
        assertTrue(Util.isValidXMLElementName("myprop"));
        assertTrue(Util.isValidXMLElementName("my_prop"));
        assertTrue(Util.isValidXMLElementName("my_prop-1"));
        assertTrue(Util.isValidXMLElementName("my_prop-1.1"));
        assertFalse(Util.isValidXMLElementName("ns:my_prop-1"));
        assertFalse(Util.isValidXMLElementName("1prop"));
        assertFalse(Util.isValidXMLElementName("xmlprop"));
        assertFalse(Util.isValidXMLElementName("XMLprop"));
        assertFalse(Util.isValidXMLElementName("xMLprop"));
        assertFalse(Util.isValidXMLElementName("Xmlprop"));
        assertFalse(Util.isValidXMLElementName("ns my_prop"));
        assertFalse(Util.isValidXMLElementName("ns,my_prop"));
    }

    @Test
    void normalizeLanguage()
    {
        assertNull(Util.normalizeLanguage(null));
        assertEquals("", Util.normalizeLanguage(""));
        assertEquals("", Util.normalizeLanguage("  "));
        assertEquals("default", Util.normalizeLanguage("default"));
        assertEquals("default", Util.normalizeLanguage("DeFault"));
        assertEquals("default", Util.normalizeLanguage("invalid code"));
        assertEquals("en", Util.normalizeLanguage("en"));
        assertEquals("en", Util.normalizeLanguage("En_"));
        assertEquals("de_AT", Util.normalizeLanguage("DE_at"));
    }
}
