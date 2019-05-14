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
package org.xwiki.test.selenium;

import org.junit.Test;
import org.xwiki.test.selenium.framework.AbstractXWikiTestCase;

/**
 * Test the Panel feature. Note that some panels tests are also done by the
 * {@link PanelWizardTest} test case class.
 * 
 * @version $Id$
 * @since 1.6M1
 */
public class PanelsTest extends AbstractXWikiTestCase
{
    /**
     * This method makes the following tests:
     * <ul>
     * <li>Opens the Panels page for XWiki instance.</li>
     * <li>Creates a panel - setting the title.</li>
     * <li>Completes the form with details for the newly created panel.</li>
     * <li>Goes to preview and pushes the button save and continue.</li>
     * <li>Completes the form and pushes the save&view button.</li>
     * <li>Goes back to panels list and check for the presence of the newly created panel.</li>
     * </ul>
     */
    @Test
    public void testCreatePanel()
    {
        try {
            open("Panels", "WebHome");
            setFieldValue("panelTitle", "Thesecondpaneltobecreated");
            clickLinkWithXPath("//input[@value='Create']");
            setFieldValue("Panels.PanelClass_0_description", "Tester panel");
            setFieldValue("Panels.PanelClass_0_content",
              "{{velocity}}\n#panelheader(\"Test panel\")\nTest Panel\n#panelfooter()\n{{/velocity}}");
            clickEditSaveAndView();
            open("Panels", "WebHome");
            assertElementPresent("//a[. = 'Thesecondpaneltobecreated']");
        } finally {
            deletePage("Panels", "Thesecondpaneltobecreated");
        }
        this.validateConsole.getLogCaptureConfiguration().registerExcludes(
            "Exception in macro #jsonResponse",
            "Exception in macro #gridresultwithfilter");
    }

    /**
     * This method makes the following tests:
     * <ul>
     * <li>Opens the Panels page for XWiki instance.</li>
     * <li>Creates a panel - setting a title with special characters ('$&/\?#).</li>
     * <li>...Checking that no error occurred</li>
     * <li>Goes back to panels list and check for the presence of the newly created panel.</li>
     * </ul>
     */
    @Test
    public void testCreatePanelWithSpecialSymbols()
    {
        String title = "Is # & \u0163 'triky\"? c:\\windows /root $util";
        try {
            open("Panels", "WebHome");
            setFieldValue("panelTitle", title);
            clickLinkWithXPath("//input[@value='Create']");
            clickEditSaveAndView();
            assertTextPresent(title);
        } finally {
            deletePage("Panels", title);
        }
    }
}
