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
package org.xwiki.test.ui;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.configuration2.PropertiesConfiguration;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.extension.test.po.ExtensionPane;
import org.xwiki.extension.test.po.ExtensionProgressPane;
import org.xwiki.extension.test.po.LogItemPane;
import org.xwiki.extension.test.po.distribution.DistributionStepIcon;
import org.xwiki.extension.test.po.distribution.FlavorDistributionStep;
import org.xwiki.extension.test.po.distribution.ReportDistributionStep;
import org.xwiki.extension.test.po.distribution.WelcomeDistributionStep;
import org.xwiki.test.integration.XWikiExecutor;
import org.xwiki.test.ui.po.ViewPage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Runs all functional tests found in the classpath.
 * 
 * @version $Id$
 * @since 10.7RC1
 */
public class UpgradeTest extends AbstractTest
{
    private static final String PREVIOUSFLAVOR_NAME = System.getProperty("previousFlavorName");

    private static final String PREVIOUSFLAVOR_VERSION = System.getProperty("previousFlavorVersion");

    private static final String STEP_ADMIN_NAME = "Admin user";

    private static final String STEP_FLAVOR_NAME = "Flavor";

    private static final String STEP_EXTENSIONS_NAME = "Extensions";

    /**
     * Automatically register as Admin user.
     */
    @Rule
    public AdminAuthenticationRule adminAuthenticationRule = new AdminAuthenticationRule(getUtil());

    /**
     * Prepare and start XWiki.
     * 
     * @throws Exception when failing to configure XWiki
     */
    @BeforeClass
    public static void init() throws Exception
    {
        XWikiExecutor executor = new XWikiExecutor(0);

        /////////////////////
        // Configure

        PropertiesConfiguration properties = executor.loadXWikiPropertiesConfiguration();

        // Put self and Maven as extensions repository
        properties.setProperty("extension.repositories",
            "localmaven:maven:file://" + System.getProperty("user.home") + "/.m2/repository");
        // Local Maven repository does not maintain any checksum and we don't want false positive warning in the install
        // log
        properties.setProperty("extension.repositories.localmaven.checksumPolicy", "ignore");
        
        executor.saveXWikiProperties();

        /////////////////////
        // Init and start

        init(Arrays.asList(executor));
    }

    // Test

    /**
     * Execute the Distribution Wizard for an upgrade from previous version to current SNAPSHOT.
     * 
     * @throws Exception when failing the test
     */
    @Test
    public void upgrade() throws Exception
    {
        // Access home page (and be automatically redirected)
        getUtil().gotoPage("Main", "WebHome");

        // Make sure we are redirected to the Distribution Wizard
        assertEquals(
            getUtil().getBaseBinURL() + "distribution/XWiki/Distribution?xredirect="
                + URLEncoder.encode("/xwiki/bin/view/Main/WebHome", StandardCharsets.UTF_8.name()),
            getUtil().getDriver().getCurrentUrl());

        ////////////////////
        // Validate Welcome step

        welcomeStep();

        ////////////////////
        // Validate Flavor step

        flavorStep();

        ////////////////////
        // Validate Report step

        reportStep();

        // Make sure we have back on home page
        ViewPage page = new ViewPage();

        assertEquals("xwiki:Main.WebHome", page.getMetaDataValue("reference"));
    }

    private void welcomeStep()
    {
        WelcomeDistributionStep welcomeStep = new WelcomeDistributionStep();

        // Steps

        List<DistributionStepIcon> icons = welcomeStep.getIcons();

        assertFalse(icons.get(0).isDone());
        assertFalse(icons.get(0).isActive());
        assertEquals(1, icons.get(0).getNumber());
        assertEquals(STEP_ADMIN_NAME, icons.get(0).getName());
        assertFalse(icons.get(1).isDone());
        assertFalse(icons.get(1).isActive());
        assertEquals(2, icons.get(1).getNumber());
        assertEquals(STEP_FLAVOR_NAME, icons.get(1).getName());
        assertFalse(icons.get(2).isDone());
        assertFalse(icons.get(2).isActive());
        assertEquals(3, icons.get(2).getNumber());
        assertEquals(STEP_EXTENSIONS_NAME, icons.get(2).getName());

        // Go to next step
        welcomeStep.clickCompleteStep();
    }

    private void flavorStep() throws Exception
    {
        FlavorDistributionStep flavorStep = new FlavorDistributionStep();

        // Steps
        List<DistributionStepIcon> icons = flavorStep.getIcons();

        assertTrue(icons.get(0).isDone());
        assertFalse(icons.get(0).isActive());
        assertEquals(STEP_ADMIN_NAME, icons.get(0).getName());
        assertEquals(2, icons.get(1).getNumber());
        assertFalse(icons.get(1).isDone());
        assertTrue(icons.get(1).isActive());
        assertEquals(STEP_FLAVOR_NAME, icons.get(1).getName());
        assertFalse(icons.get(2).isDone());
        assertFalse(icons.get(2).isActive());
        assertEquals(3, icons.get(2).getNumber());
        assertEquals(STEP_EXTENSIONS_NAME, icons.get(2).getName());

        // Make sure complete step is disabled
        assertFalse(flavorStep.isCompleteStepDisabled());

        // Check current flavor
        ExtensionPane currentFlavor = flavorStep.getCurrentFlavorExtensionPane();
        assertEquals(PREVIOUSFLAVOR_NAME, currentFlavor.getName());
        assertEquals(PREVIOUSFLAVOR_VERSION, currentFlavor.getVersion());
        assertEquals("installed-invalid", currentFlavor.getStatus());

        // Flavor upgrade
        // TODO: support more use cases (invalid flavor, no flavor, etc.)
        flavorStepKnownUpgrade(flavorStep);

        // Go to next step
        flavorStep.clickCompleteStep();
    }

    private void flavorStepKnownUpgrade(FlavorDistributionStep flavorStep) throws Exception
    {

        // Check upgrade flavor
        ExtensionPane upgradeFlavor = flavorStep.getKnownUpgradeFlavorExtensionPane();
        assertEquals("XWiki Standard Flavor", upgradeFlavor.getName());
        assertEquals(getUtil().getVersion(), upgradeFlavor.getVersion());
        assertEquals("remote-installed-invalid", upgradeFlavor.getStatus());

        // Upgrade the flavor
        int timeout = getUtil().getDriver().getTimeout();
        try {
            // 10 minutes should be more than enough to calculate the install plan and do the install
            getUtil().getDriver().setTimeout(600);

            // Start upgrade
            upgradeFlavor = upgradeFlavor.upgrade();
            // Confirm upgrade
            upgradeFlavor = upgradeFlavor.confirm();

            if (upgradeFlavor.getStatus().equals("loading")) {
                ExtensionProgressPane extensionProgress = upgradeFlavor.openProgressSection();

                if (extensionProgress.getUnusedPages() != null) {
                    // Confirm delete unused pages
                    upgradeFlavor = upgradeFlavor.confirm();
                } else if (extensionProgress.getMergeConflict() != null) {
                    fail("Merge conflict hit during flavor upgrade");
                } else {
                    fail("The flavor upgrade is waiting");
                }
            }

            // Make sure there hasn't been any error or warning during the install
            List<LogItemPane> logs = upgradeFlavor.openProgressSection().getJobLog().stream()
                .filter(l -> l.getLevel().equals("error") || l.getLevel().equals("warning"))
                .collect(Collectors.toList());
            if (!logs.isEmpty()) {
                fail("Warning and/or errors found in the log during flavor install");
            }
        } finally {
            getUtil().getDriver().setTimeout(timeout);
        }

        assertEquals("installed", upgradeFlavor.getStatus());

    }

    private void reportStep()
    {
        ReportDistributionStep reportStep = new ReportDistributionStep();

        // Steps
        List<DistributionStepIcon> icons = reportStep.getIcons();

        assertTrue(icons.get(0).isDone());
        assertFalse(icons.get(0).isActive());
        assertEquals(STEP_ADMIN_NAME, icons.get(0).getName());
        assertTrue(icons.get(1).isDone());
        assertFalse(icons.get(1).isActive());
        assertEquals(STEP_FLAVOR_NAME, icons.get(1).getName());
        assertTrue(icons.get(2).isDone());
        assertFalse(icons.get(2).isActive());
        assertEquals(STEP_EXTENSIONS_NAME, icons.get(2).getName());

        // Finish
        reportStep.clickCompleteStep();
    }
}
