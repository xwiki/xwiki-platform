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
package org.xwiki.officeimporter.internal.cleaner;

import org.junit.Test;

/**
 * Test case for cleaning html tables in {@link OpenOfficeHTMLCleaner}.
 * 
 * @version $Id$
 * @since 1.8
 */
public class TableOpenOfficeCleaningTest extends AbstractHTMLCleaningTest
{
    /**
     * An isolated paragraph inside a table cell item should be replaced with paragraph's content.
     */
    @Test
    public void testTableCellItemIsolatedParagraphCleaning()
    {
        assertCleanHTML("<table><tbody><tr><td><p>Test</p></td></tr></tbody></table>",
            "<table><tbody><tr><td>Test</td></tr></tbody></table>");
    }

    /**
     * An isolated paragraph inside a table header item should be replaced with paragraph's content.
     */
    @Test
    public void testTableHeaderItemIsolatedParagraphCleaning()
    {
        assertCleanHTML("<table><thead><tr><th><p>Test</p></th></tr></thead><tbody><tr><td></td></tr></tbody></table>",
            "<table><thead><tr><th>Test</th></tr></thead><tbody><tr><td></td></tr></tbody></table>");
    }

    /**
     * Tests that empty cells (e.g. those that contains just a line break) are preserved (i.e. are left unchanged).
     */
    @Test
    public void testEmptyCellsArePreserved()
    {
        String html =
            "<table><thead><tr><th><br/></th><th></th></tr></thead>"
                + "<tbody><tr><td></td><td><br/></td></tr></tbody></table>";
        assertCleanHTML(html, html);
    }

    /**
     * Empty rows should be removed.
     */
    @Test
    public void testEmptyRowRemoving()
    {
        assertCleanHTML("<table><tbody><tr><td>cell</td></tr><tr></tr></tbody></table>",
            "<table><tbody><tr><td>cell</td></tr></tbody></table>");
    }
}
