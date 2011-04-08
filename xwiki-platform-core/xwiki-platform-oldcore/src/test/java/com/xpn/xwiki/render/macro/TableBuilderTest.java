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
package com.xpn.xwiki.render.macro;

import java.io.IOException;
import java.io.StringWriter;

import junit.framework.TestCase;

import org.radeox.macro.table.Table;

/**
 * Tests for the {@link TableBuilder} functionality.
 * 
 * @version $Id$
 */
public class TableBuilderTest extends TestCase
{
    /** Test for the basic functionality. */
    public void testSimpleTable() throws IOException
    {
        Table t = TableBuilder.build("1|2|3\na|b|c\n");
        StringWriter s = new StringWriter();
        t.appendTo(s);
        assertEquals("1", t.getXY(0, 0));
        assertEquals("2", t.getXY(1, 0));
        assertEquals("3", t.getXY(2, 0));
        assertEquals("a", t.getXY(0, 1));
        assertEquals("b", t.getXY(1, 1));
        assertEquals("c", t.getXY(2, 1));
        boolean thrown = false;
        try {
            t.getXY(3, 0);
        } catch (IndexOutOfBoundsException ex) {
            thrown = true;
        }
        assertTrue(thrown);
        thrown = false;
        try {
            t.getXY(3, 1);
        } catch (IndexOutOfBoundsException ex) {
            thrown = true;
        }
        assertTrue(thrown);
        thrown = false;
        try {
            t.getXY(0, 2);
        } catch (IndexOutOfBoundsException ex) {
            thrown = true;
        }
        assertTrue(thrown);
    }

    /** Test what happens with missing and extra cells. */
    public void testMissingExtraAndEmptyCells() throws IOException
    {
        Table t = TableBuilder.build("1|2|3\na|b\na|b|c|d\na||c\na| |c\n");
        StringWriter s = new StringWriter();
        t.appendTo(s);
        assertEquals("1", t.getXY(0, 0));
        assertEquals("2", t.getXY(1, 0));
        assertEquals("3", t.getXY(2, 0));
        assertEquals("a", t.getXY(0, 1));
        assertEquals("b", t.getXY(1, 1));
        boolean thrown = false;
        try {
            t.getXY(2, 1);
        } catch (IndexOutOfBoundsException ex) {
            thrown = true;
        }
        assertTrue(thrown);
        assertEquals("a", t.getXY(0, 2));
        assertEquals("b", t.getXY(1, 2));
        assertEquals("c", t.getXY(2, 2));
        assertEquals("d", t.getXY(3, 2));
        assertEquals("a", t.getXY(0, 3));
        assertEquals("", t.getXY(1, 3));
        assertEquals("c", t.getXY(2, 3));
        assertEquals("a", t.getXY(0, 4));
        assertEquals("", t.getXY(1, 4));
        assertEquals("c", t.getXY(2, 4));
    }

    /** Test the correct detection of empty headers (|||). */
    public void testEmptyHeader() throws IOException
    {
        Table t = TableBuilder.build("||\r\na|b|c\n");
        StringWriter s = new StringWriter();
        t.appendTo(s);
        assertEquals("", t.getXY(0, 0));
        assertEquals("", t.getXY(1, 0));
        assertEquals("", t.getXY(2, 0));
        assertEquals("a", t.getXY(0, 1));
        assertEquals("b", t.getXY(1, 1));
        assertEquals("c", t.getXY(2, 1));
        boolean thrown = false;
        try {
            t.getXY(3, 0);
        } catch (IndexOutOfBoundsException ex) {
            thrown = true;
        }
        assertTrue(thrown);
    }

    /** Test the correct detection of blank headers ( | | | ). */
    public void testBlankHeader() throws IOException
    {
        Table t = TableBuilder.build(" | | \na|b|c\n");
        StringWriter s = new StringWriter();
        t.appendTo(s);
        assertEquals("", t.getXY(0, 0));
        assertEquals("", t.getXY(1, 0));
        assertEquals("", t.getXY(2, 0));
        assertEquals("a", t.getXY(0, 1));
        assertEquals("b", t.getXY(1, 1));
        assertEquals("c", t.getXY(2, 1));
        boolean thrown = false;
        try {
            t.getXY(3, 0);
        } catch (IndexOutOfBoundsException ex) {
            thrown = true;
        }
        assertTrue(thrown);
    }
}
