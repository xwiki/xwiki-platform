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
@UITest(properties = {
    "xwikiPropertiesAdditionalProperties=security.requiredRights.protection=warning",
    "xwikiCfgPlugins=com.xpn.xwiki.plugin.skinx.JsResourceSkinExtensionPlugin," 
        + "com.xpn.xwiki.plugin.skinx.CssResourceSkinExtensionPlugin"
})
class RequiredRightsIT
{
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
        assertEquals("The title is [Hello $a].", requiredRightsPreEditCheckElement.getDetailedMessage(0));
    }
}
