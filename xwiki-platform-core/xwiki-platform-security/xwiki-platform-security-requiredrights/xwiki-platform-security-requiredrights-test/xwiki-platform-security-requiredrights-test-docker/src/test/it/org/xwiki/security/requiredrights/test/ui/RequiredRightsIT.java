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
package org.xwiki.security.requiredrights.test.ui;

import org.junit.jupiter.api.Test;
import org.xwiki.security.requiredrights.test.po.RequiredRightsPreEditCheckElement;
import org.xwiki.test.docker.junit5.TestReference;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Main test class for the required rights functional tests.
 *
 * @version $Id$
 * @since 15.9RC1
 */
@UITest
class RequiredRightsIT
{
    private static final String VELOCITY_MACRO_REQUIREMENTS_MESSAGE =
        "A [velocity] scripting macro requires script rights "
            + "and might require programming right depending on the called methods.";

    @Test
    void checkTitleWithVelocityCode(TestUtils setup, TestReference testReference)
    {
        setup.loginAsSuperAdmin();

        setup.deletePage(testReference);

        // Create a page with a title containing a string that could be interpreted as velocity.
        setup.createPage(testReference, "", "Hello $a");

        setup.createUserAndLogin("U1", "U1p");

        setup.gotoPage(testReference, "edit");

        RequiredRightsPreEditCheckElement requiredRightsPreEditCheckElement = new RequiredRightsPreEditCheckElement()
            .toggleDetails();
        assertEquals(1, requiredRightsPreEditCheckElement.count());
        assertEquals("The document's title contains \"#\" or \"$\" which might be executed as Velocity code "
                + "if the document's author has script or programming rights.",
            requiredRightsPreEditCheckElement.getSummary(0));
        requiredRightsPreEditCheckElement.toggleDetailedMessage(0);
        requiredRightsPreEditCheckElement.waitForDetailedMessage(0, "The title is [Hello $a].");
        assertEquals(setup.getURL(testReference.getLastSpaceReference()) + "/",
            requiredRightsPreEditCheckElement.getTitleHref(0));
    }

    @Test
    void checkContentWithVelocityMacro(TestUtils setup, TestReference testReference)
    {
        setup.loginAsSuperAdmin();

        setup.deletePage(testReference);

        setup.createUserAndLogin("U1", "U1p");

        // Create a page with two velocity macros, by an user without script right.
        setup.createPage(testReference, "{{velocity}}macro1{{/velocity}}\n"
            + "{{velocity}}macro2{{/velocity}}", "");

        setup.loginAsSuperAdmin();

        setup.gotoPage(testReference, "edit");

        RequiredRightsPreEditCheckElement requiredRightsPreEditCheckElement = new RequiredRightsPreEditCheckElement()
            .toggleDetails();
        assertEquals(2, requiredRightsPreEditCheckElement.count());
        assertEquals(VELOCITY_MACRO_REQUIREMENTS_MESSAGE, requiredRightsPreEditCheckElement.getSummary(0));
        requiredRightsPreEditCheckElement.toggleDetailedMessage(0);
        requiredRightsPreEditCheckElement.waitForDetailedMessage(0, "Content\n"
            + "the velocity script to execute\n"
            + "macro1");

        assertEquals(VELOCITY_MACRO_REQUIREMENTS_MESSAGE, requiredRightsPreEditCheckElement.getSummary(1));
        requiredRightsPreEditCheckElement.toggleDetailedMessage(1);
        requiredRightsPreEditCheckElement.waitForDetailedMessage(1, "Content\n"
            + "the velocity script to execute\n"
            + "macro2");
    }
}
