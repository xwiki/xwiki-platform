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
package org.xwiki.messagestream.test.ui;

import org.junit.Rule;
import org.junit.Test;
import org.xwiki.administration.test.po.AdministrationPage;
import org.xwiki.test.ui.AbstractTest;
import org.xwiki.test.ui.SuperAdminAuthenticationRule;

import static org.junit.Assert.*;

/**
 * Verify the Message Stream features.
 *
 * @version $Id$
 * @since 4.3M1
 */
public class MessageStreamTest extends AbstractTest
{
    @Rule
    public SuperAdminAuthenticationRule authenticationRule = new SuperAdminAuthenticationRule(getUtil());

    @Test
    public void verifyGlobalAndSpaceAdministrationSections()
    {
        // Go to the wiki's administration page directly (the goal of this test is not to test the navigation
        // from the Drawer menu to the Administration page).
        AdministrationPage wikiAdministrationPage = AdministrationPage.gotoPage();

        // The MessageStream section should be present
        assertTrue(wikiAdministrationPage.hasSection("MessageStream"));

        // Go to a space's administration page (we use the XWiki space).
        AdministrationPage spaceAdministrationPage = AdministrationPage.gotoSpaceAdministrationPage("XWiki");

        // The MessageStream section should not be present since it's only a wiki level option
        assertTrue(spaceAdministrationPage.hasNotSection("MessageStream"));
    }
}
