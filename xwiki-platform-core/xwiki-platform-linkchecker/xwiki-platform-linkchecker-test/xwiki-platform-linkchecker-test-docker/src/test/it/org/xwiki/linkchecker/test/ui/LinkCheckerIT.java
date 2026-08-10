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
package org.xwiki.linkchecker.test.ui;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.xwiki.linkchecker.test.po.LinkCheckerAllDocsPage;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.integration.junit.LogCaptureConfiguration;
import org.xwiki.test.ui.TestUtils;
import org.xwiki.test.ui.po.LiveTableElement;

/**
 * UI tests for the Link Checker feature.
 *
 * @version $Id$
 * @since 18.7.0RC1
 */
@UITest(
    // StubHTTPChecker (this module's src/main component, shipped in WEB-INF/classes) is parsed by the component
    // annotation loader at WAR startup, so the HTTPChecker role it implements must already be in WEB-INF/lib at that
    // point. The linkchecker transformation isn't in the minimal WAR and is normally installed as a runtime
    // extension (too late for startup parsing), hence it must be added as an extra JAR.
    extraJARs = {
        "org.xwiki.rendering:xwiki-rendering-transformation-linkchecker"
    },
    // Activate the linkchecker rendering transformation (in addition to the defaults macro + icon) so that link states
    // are computed and surfaced in the AllDocs "External Links" livetable.
    properties = {
        "xwikiPropertiesAdditionalProperties=rendering.transformations=macro,icon,linkchecker"
    }
)
class LinkCheckerIT
{
    @BeforeAll
    static void beforeAll(TestUtils setup)
    {
        setup.loginAsSuperAdmin();
    }

    @Test
    void linkChecker(TestUtils setup, LogCaptureConfiguration logCaptureConfiguration)
    {
        setup.deletePage("LinkCheckerIT", "linkChecker");

        // Create a page with a URL so that it appears in the Link Checker livetable.
        setup.createPage("LinkCheckerIT", "linkChecker", "http://doesntexist", "LinkCheckerIT");

        // Navigate to the Index page and click on the "External Links" tab.
        LinkCheckerAllDocsPage page = LinkCheckerAllDocsPage.gotoPage();

        // Since the LinkChecker works asynchronously there's a small possibility that the link hasn't been added
        // before the livetable displays, thus we wait till we get the link state.
        // The expected state is 404 because StubHTTPChecker answers it for this URL. Asserting it also proves that the
        // stub is the HTTPChecker in use: the real one would answer 0 (connection failure) for an unresolvable host,
        // and the row would show up all the same since the state is recorded whatever the response code.
        setup.getDriver().waitUntilCondition(new ExpectedCondition<Boolean>()
        {
            @Override
            public Boolean apply(WebDriver driver)
            {
                LiveTableElement livetable = page.clickLinkCheckerTab();
                return livetable.hasRow("Link", "http://doesntexist") && livetable.hasRow("State", "404");
            }
        });

        logCaptureConfiguration.registerExcludes(
            "Link checker Thread was stopped due to some problem",
            "org.infinispan.IllegalLifecycleStateException: ISPN000323: Cache 'configuration.document.space' is in "
                + "'TERMINATED' state");
    }
}
