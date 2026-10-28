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
package org.xwiki.test.ui.docker;

import java.util.List;

import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.xwiki.extension.security.test.po.ExtensionVulnerabilitiesAdminPage;
import org.xwiki.extension.test.po.ExtensionAdministrationPage;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.xwiki.extension.security.test.po.ExtensionVulnerabilitiesAdminPage.goToExtensionVulnerabilitiesAdmin;

/**
 * Performs a security scan of the extensions of the standard flavor and of its environment, and fails if any security
 * issue that hasn't been reviewed as safe is found.
 *
 * @version $Id$
 * @since 18.10.0RC1
 */
// standardFlavor = true because the scan must cover the extensions that the users get with the standard flavor.
// The legacy profile is needed because the distribution WAR is built with it: it bundles the legacy variants of some
// core extensions (e.g. xwiki-platform-legacy-rendering-wikimacro-store instead of
// xwiki-platform-rendering-wikimacro-store), and these variants have their own extension ids, with their own known
// vulnerabilities.
// Note that the Extension Security UI isn't part of the standard flavor: it's installed because it's a dependency of
// this module.
@UITest(standardFlavor = true, profiles = "legacy")
class ExtensionSecurityScanIT
{
    /**
     * The maximum time to wait for the indexation of the extensions and for the security scan, in seconds. It's large
     * since both go over all the installed extensions and query remote services.
     */
    private static final int JOB_TIMEOUT = 1000;

    @Test
    @Order(1)
    void scanHasNoSecurityIssueToReview(TestUtils setup)
    {
        setup.loginAsSuperAdmin();

        ExtensionAdministrationPage extensionAdministrationPage = ExtensionAdministrationPage.gotoPage();
        extensionAdministrationPage.startIndex();
        extensionAdministrationPage.waitUntilIndexIsDone(JOB_TIMEOUT);

        goToExtensionVulnerabilitiesAdmin().waitUntilScanIsDone(JOB_TIMEOUT);

        // Reload the page so that it displays the results of the scan that just finished.
        ExtensionVulnerabilitiesAdminPage extensionVulnerabilitiesAdminPage = goToExtensionVulnerabilitiesAdmin();
        List<String> cveIDs = extensionVulnerabilitiesAdminPage.getCveIDsToReview();

        assertTrue(cveIDs.isEmpty(), () -> String.format("The following security issues are to be analyzed:%n- %s",
            String.join(String.format("%n- "), cveIDs)));
    }
}
