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
package org.xwiki.index.test.ui.docker;

import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.xwiki.index.test.po.DocumentsMacroPage;
import org.xwiki.livedata.test.po.TableLayoutElement;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.test.docker.junit5.TestReference;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for the DocumentsMacro page.
 *
 * @version $Id$
 * @since 4.2M3
 */
@UITest
class DocumentsMacroIT
{
    /**
     * Verify that the {@code {{documents/}}} macro works by going to the page defining this wiki macro since it
     * contains an example usage and we can verify it displays the expected result.
     */
    @Test
    @Order(1)
    void documentsMacro(TestUtils setup, TestReference testReference)
    {
        // Create a dummy page in the Main space and having Main.WebHome as its parent so that it appears in the
        // Documents Macro livetable (since the example lists pages in the Main space having Main.WebHome as their
        // parent).
        String testMethodName = testReference.getLastSpaceReference().getName();
        DocumentReference documentReference = new DocumentReference(testMethodName,
            new SpaceReference("Main", testReference.getWikiReference()));
        setup.createPage(documentReference, "", "Test Title", "xwiki/2.1", "Main.WebHome");

        DocumentsMacroPage dmp = DocumentsMacroPage.gotoPage();
        TableLayoutElement tableLayout = dmp.getDocumentsExampleLiveTable().getTableLayout();

        // Verify that we have a Page column
        assertTrue(tableLayout.hasColumn("Title"), "No Title column found");

        // Verify there are several rows displayed
        assertTrue(tableLayout.countRows() > 0);

        // Verify that page titles are displayed by filtering on one page for which we know the title
        tableLayout.filterColumn("Location", testMethodName);
        tableLayout.assertRow("Title", "Test Title");
    }
}
