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

import java.text.MessageFormat;

import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.xwiki.index.test.po.OrphanedPagesPage;
import org.xwiki.livedata.test.po.TableLayoutElement;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.test.docker.junit5.TestReference;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.xwiki.security.internal.XWikiConstants.GUEST_USER;
import static org.xwiki.security.internal.XWikiConstants.XWIKI_SPACE;

/**
 * Test of the orphaned pages.
 *
 * @version $Id$
 * @since 13.5RC1
 */
@UITest
class OrphanedPagesIT
{
    @Test
    @Order(1)
    void listOrphanedPages(TestUtils setup, TestReference testReference)
    {
        SpaceReference testSpaceReference = testReference.getLastSpaceReference();

        setup.loginAsSuperAdmin();
        setup.deletePage(testReference, true);
        setup.forceGuestUser();

        setup.createPage(testReference, "");
        TableLayoutElement tableLayout = OrphanedPagesPage.goToPage().getLiveData().getTableLayout();
        assertEquals(1, tableLayout.countRows());
        tableLayout.assertCellWithLink("Page", "WebHome", setup.getURL(testSpaceReference));
        tableLayout.assertCellWithLink("Space",
            MessageFormat.format("{0}.{1}", testSpaceReference.getParent().getName(), testSpaceReference.getName()),
            setup.getURL(testSpaceReference));
        tableLayout.assertRow("Date", CoreMatchers.hasItem(tableLayout.getDatePatternMatcher()));
        tableLayout.assertCellWithLink("Last Author", "Unknown User",
            setup.getURL(new DocumentReference("xwiki", XWIKI_SPACE, GUEST_USER)));
        tableLayout.assertCellWithCopyAction("Actions", testSpaceReference);
        setup.loginAsSuperAdmin();
        tableLayout = OrphanedPagesPage.goToPage().getLiveData().getTableLayout();
        assertEquals(1, tableLayout.countRows());
        tableLayout.assertCellWithDeleteAction("Actions", testReference);
        tableLayout.assertCellWithCopyAction("Actions", testSpaceReference);
        tableLayout.assertCellWithRenameAction("Actions", testSpaceReference);
        tableLayout.assertCellWithRightsAction("Actions", testSpaceReference);
    }
}
