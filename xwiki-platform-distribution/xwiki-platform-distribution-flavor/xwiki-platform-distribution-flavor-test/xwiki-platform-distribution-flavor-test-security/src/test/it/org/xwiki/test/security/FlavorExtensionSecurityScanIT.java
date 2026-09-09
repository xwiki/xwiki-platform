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

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.xwiki.extension.security.test.po.ExtensionVulnerabilitiesAdminPage;
import org.xwiki.extension.test.po.ExtensionAdministrationPage;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.xwiki.extension.security.test.po.ExtensionVulnerabilitiesAdminPage.goToExtensionVulnerabilitiesAdmin;

/**
 * Runs the extension security scan against the live vulnerability sources on a standard-flavor instance and fails if
 * any vulnerability is left to review. Unlike {@code ExtensionSecurityIT} in
 * {@code xwiki-platform-extension-security-test-docker}, which drives the feature against mocked sources, this test
 * exercises the real sources over the whole set of extensions shipped by the standard flavor.
 *
 * @version $Id$
 * @since 18.7.0RC1
 */
// standardFlavor = true: the scan must cover the extensions of a standard-flavor instance. The extension-security UI
// itself is provisioned from the module's runtime xar dependency by the test framework.
@UITest(standardFlavor = true)
class FlavorExtensionSecurityScanIT
{
    @BeforeAll
    void beforeAll(TestUtils setup)
    {
        setup.loginAsSuperAdmin();
    }

    /**
     * Indexes the installed extensions, then asserts that the Extension Vulnerabilities admin section lists no CVE
     * left to review. A failure here means the flavor ships an extension with a vulnerability that nobody has reviewed
     * yet, and the failure message lists the offending CVE ids.
     */
    @Test
    void checkForExtensionSecurityScanResult(TestUtils setup)
    {
        ExtensionAdministrationPage.gotoPage().startIndex();
        waitForSuccessMessage(setup);

        ExtensionVulnerabilitiesAdminPage extensionVulnerabilitiesAdminPage = goToExtensionVulnerabilitiesAdmin();
        waitForSuccessMessage(setup);

        setup.getDriver().navigate().refresh();

        List<String> cveIDs = extensionVulnerabilitiesAdminPage.getCveIDsToReview();

        assertTrue(cveIDs.isEmpty(), () -> {
            StringBuilder stringBuilder = new StringBuilder();
            for (String cveID : cveIDs) {
                stringBuilder.append("- ").append(cveID).append(System.lineSeparator());
            }
            return String.format("The following list of security issues are to be analyzed:%n%s", stringBuilder);
        });
    }

    private static void waitForSuccessMessage(TestUtils setup)
    {
        // Indexing every extension against the remote vulnerability source is slow, hence the very large multiplier.
        setup.getDriver().waitUntilElementIsVisible(By.cssSelector(".box.successmessage"),
            setup.getDriver().getTimeout() * 100);
    }
}
