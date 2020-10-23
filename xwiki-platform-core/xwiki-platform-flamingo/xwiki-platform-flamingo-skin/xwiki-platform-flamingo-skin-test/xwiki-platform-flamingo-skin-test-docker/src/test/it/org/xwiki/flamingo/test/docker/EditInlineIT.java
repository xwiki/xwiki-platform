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
package org.xwiki.flamingo.test.docker;

import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;
import org.xwiki.test.ui.po.ViewPage;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests the inline Edition.
 *
 * @version $Id$
 */
@UITest
public class EditInlineIT
{
    // Note: We're not testing basic inline editing since this is already covered by the User Profile tests

    @Test
    @Order(1)
    public void defaultEditButtonCanTriggerInlineEditing(TestUtils setup)
    {
        setup.loginAsAdmin();

        // Go to the Admin user profile page and edit it since editing a user profile page is supposed to go in inline
        // editing by default. Note that this tests 2 things at once:
        // - that clicking the "edit" button on the user profile goes in inline edit mode
        // - that a page with a sheet defined goes in inline edit mode when clicking "edit" on it
        ViewPage vp = setup.gotoPage("XWiki", "Admin");
        vp.edit();
        assertTrue(new ViewPage().isInlinePage());
    }
}
