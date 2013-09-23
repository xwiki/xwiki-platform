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

import org.junit.Assert;
import org.junit.Test;

/**
 * Unit tests for {@link com.xpn.xwiki.util.Util}.
 *
 * @version $Id$
 */
public class UtilTest
{
    @Test
    public void testConvertToAlphaNumeric()
    {
        Assert.assertEquals("Search", Util.convertToAlphaNumeric("Search"));
        Assert.assertEquals("Search", Util.convertToAlphaNumeric("S.earch"));
        Assert.assertEquals("e", Util.convertToAlphaNumeric("e$%\u00a3#^()"));
        Assert.assertEquals("e", Util.convertToAlphaNumeric(":\u0205!"));
    }

    @Test
    public void testIsValidXMLElementName()
    {
        Assert.assertTrue(Util.isValidXMLElementName("myprop"));
        Assert.assertTrue(Util.isValidXMLElementName("my_prop"));
        Assert.assertTrue(Util.isValidXMLElementName("my_prop-1"));
        Assert.assertTrue(Util.isValidXMLElementName("my_prop-1.1"));
        Assert.assertFalse(Util.isValidXMLElementName("ns:my_prop-1"));
        Assert.assertFalse(Util.isValidXMLElementName("1prop"));
        Assert.assertFalse(Util.isValidXMLElementName("xmlprop"));
        Assert.assertFalse(Util.isValidXMLElementName("XMLprop"));
        Assert.assertFalse(Util.isValidXMLElementName("xMLprop"));
        Assert.assertFalse(Util.isValidXMLElementName("Xmlprop"));
        Assert.assertFalse(Util.isValidXMLElementName("ns my_prop"));
        Assert.assertFalse(Util.isValidXMLElementName("ns,my_prop"));
    }

    @Test
    public void testNormalizeLanguage()
    {
        Assert.assertNull(Util.normalizeLanguage(null));
        Assert.assertEquals("", Util.normalizeLanguage(""));
        Assert.assertEquals("", Util.normalizeLanguage("  "));
        Assert.assertEquals("default", Util.normalizeLanguage("default"));
        Assert.assertEquals("default", Util.normalizeLanguage("DeFault"));
        Assert.assertEquals("default", Util.normalizeLanguage("invalid code"));
        Assert.assertEquals("en", Util.normalizeLanguage("en"));
        Assert.assertEquals("en", Util.normalizeLanguage("En_"));
        Assert.assertEquals("de_AT", Util.normalizeLanguage("DE_at"));
    }
}
