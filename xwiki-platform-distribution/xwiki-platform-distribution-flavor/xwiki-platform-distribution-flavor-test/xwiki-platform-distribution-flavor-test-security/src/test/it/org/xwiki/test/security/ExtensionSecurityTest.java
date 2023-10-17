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
package org.xwiki.test.security;

import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.openqa.selenium.By;
import org.xwiki.extension.ExtensionId;
import org.xwiki.extension.security.test.po.ExtensionVulnerabilitiesAdminPage;
import org.xwiki.extension.test.po.ExtensionAdministrationPage;
import org.xwiki.test.extension.RestExtensionInstaller;
import org.xwiki.test.ui.AbstractTest;
import org.xwiki.test.ui.AdminAuthenticationRule;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.xwiki.extension.security.test.po.ExtensionVulnerabilitiesAdminPage.goToExtensionVulnerabilitiesAdmin;
import static org.xwiki.test.ui.TestUtils.ADMIN_CREDENTIALS;

/**
 * Performs a security scan and checks for any security issues. The test fails if any non-review security issue is
 * found.
 *
 * @version $Id$
 * @since 15.9RC1
 * @since 15.5.3
 */
public class ExtensionSecurityTest extends AbstractTest
{
    @Rule
    public AdminAuthenticationRule adminAuthenticationRule = new AdminAuthenticationRule(getUtil());

    @Test
    public void checkForExtensionSecurityScanResult() throws Exception
    {
        ExtensionId extensionId =
            new ExtensionId("org.xwiki.platform:xwiki-platform-extension-security-ui", getUtil().getVersion());
        componentManager.initialize(getClass().getClassLoader());
        new RestExtensionInstaller(componentManager).installExtensions(getUtil().getBaseURL(), List.of(extensionId),
            ADMIN_CREDENTIALS, ADMIN_CREDENTIALS.getUserName(), null, false);

        ExtensionAdministrationPage.gotoPage().startIndex();
        waitForSuccessMessage();

        ExtensionVulnerabilitiesAdminPage extensionVulnerabilitiesAdminPage = goToExtensionVulnerabilitiesAdmin();
        waitForSuccessMessage();

        getDriver().navigate().refresh();

        List<String> cveIDs = extensionVulnerabilitiesAdminPage.getCveIDsToReview();

        assertTrue(cveIDs.isEmpty(), () -> {
            StringBuilder stringBuilder = new StringBuilder();
            for (String cveID : cveIDs) {
                stringBuilder.append("- ");
                stringBuilder.append(cveID);
                stringBuilder.append(System.lineSeparator());
            }
            return String.format("The following list of security issues are to be analyzed:\n%s", stringBuilder);
        });
    }

    private static void waitForSuccessMessage()
    {
        // Sets a large timeout as indexing can take a lot of time.
        getDriver().waitUntilElementIsVisible(By.cssSelector(".box.successmessage"), getDriver().getTimeout() * 100);
    }
}
