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

package org.xwiki.velocity.tools;

import org.junit.Assert;
import org.junit.Test;

/**
 * Unit tests for {@link EscapeTool}.
 * 
 * @version $Id$
 * @since 2.7RC1
 */
public class EscapeToolTest
{
    @Test
    public void testEscapeSimpleXML()
    {
        EscapeTool tool = new EscapeTool();
        String escapedText = tool.xml("a < a' && a' < a\" => a < a\"");

        Assert.assertFalse("Failed to escape <", escapedText.contains("<"));
        Assert.assertFalse("Failed to escape >", escapedText.contains(">"));
        Assert.assertFalse("Failed to escape '", escapedText.contains("'"));
        Assert.assertFalse("Failed to escape \"", escapedText.contains("\""));
        Assert.assertFalse("Failed to escape &", escapedText.contains("&&"));
    }

    @Test
    public void testEscapeXMLApos()
    {
        EscapeTool tool = new EscapeTool();

        Assert.assertFalse("' wrongly escaped to non-HTML &apos;", tool.xml("'").equals("&apos;"));
    }

    @Test
    public void testEscapeXMLWithNull()
    {
        EscapeTool tool = new EscapeTool();

        Assert.assertNull("null should be null", tool.xml(null));
    }

    @Test
    public void testEscapeXMLNonAscii()
    {
        EscapeTool tool = new EscapeTool();

        Assert.assertTrue("Non-ASCII characters shouldn't be escaped", tool.xml("\u0123").equals("\u0123"));
    }
}
