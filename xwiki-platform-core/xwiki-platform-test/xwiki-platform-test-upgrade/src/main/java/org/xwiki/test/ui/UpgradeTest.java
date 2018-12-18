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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.configuration2.PropertiesConfiguration;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.extension.ExtensionId;
import org.xwiki.extension.internal.ExtensionUtils;
import org.xwiki.extension.internal.converter.ExtensionIdConverter;
import org.xwiki.extension.test.po.ExtensionPane;
import org.xwiki.extension.test.po.ExtensionProgressPane;
import org.xwiki.extension.test.po.LogItemPane;
import org.xwiki.extension.test.po.distribution.DistributionStepIcon;
import org.xwiki.extension.test.po.distribution.ExtensionsDistributionStep;
import org.xwiki.extension.test.po.distribution.FlavorDistributionStep;
import org.xwiki.extension.test.po.distribution.ReportDistributionStep;
import org.xwiki.extension.test.po.distribution.WelcomeDistributionStep;
import org.xwiki.extension.test.po.flavor.FlavorPane;
import org.xwiki.extension.test.po.flavor.FlavorPicker;
import org.xwiki.extension.test.po.flavor.FlavorPickerInstallStep;
import org.xwiki.logging.LogLevel;
import org.xwiki.test.integration.XWikiExecutor;
import org.xwiki.test.ui.po.ViewPage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Validate the Distribution Wizard part of the upgrade process.
 * 
 * @version $Id$
 * @since 10.7RC1
 */
public class UpgradeTest extends AbstractTest
{
    private static final String PREVIOUSFLAVOR_NAME = System.getProperty("previousFlavorName");

    private static final ExtensionId PREVIOUSFLAVOR_ID =
        ExtensionIdConverter.toExtensionId(System.getProperty("previousFlavorId"), null);

    private static final String FLAVOR_NAME = System.getProperty("flavorName");

    private static final String FLAVOR_SUMMARY = System.getProperty("flavorSummary");

    private static final ExtensionId FLAVOR_ID =
        ExtensionIdConverter.toExtensionId(System.getProperty("flavorId"), null);

    private static final List<ExtensionId> KNOW_VALID_FLAVORS_IDS = ExtensionIdConverter
        .toExtensionIdList(ExtensionUtils.importPropertyStringList(System.getProperty("knowValidFlavors"), true), null);

    private static final Set<String> KNOW_VALID_FLAVORS =
        KNOW_VALID_FLAVORS_IDS.stream().map(extensionId -> extensionId.getId()).collect(Collectors.toSet());

    private static final Set<String> KNOW_INVALID_FLAVORS =
        new HashSet<>(ExtensionUtils.importPropertyStringList(System.getProperty("knowInvalidFlavors"), true));

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

        // Disable extension repositories to make sure it only look at local extensions
        properties.setProperty("extension.repositories", "");

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
        // Validate Flavor step

        extensionsStep();

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
        assertEquals(PREVIOUSFLAVOR_ID.getVersion().getValue(), currentFlavor.getVersion());
        assertEquals("installed-invalid", currentFlavor.getStatus());

        // Flavor upgrade
        if (KNOW_VALID_FLAVORS.contains(PREVIOUSFLAVOR_ID.getId())) {
            flavorStepKnownValidUpgrade(flavorStep);
        } else if (KNOW_INVALID_FLAVORS.contains(PREVIOUSFLAVOR_ID.getId())) {
            flavorStepKnownInvalidUpgrade(flavorStep);
        } else {
            // TODO
            fail("Unsupported Flavor step use case");
        }
    }

    private void flavorStepKnownValidUpgrade(FlavorDistributionStep flavorStep) throws Exception
    {
        upgrade(flavorStep.getKnownValieFlavorUpgradeExtensionPane());

        // Go to next step
        flavorStep.clickCompleteStep();
    }

    private void flavorStepKnownInvalidUpgrade(FlavorDistributionStep flavorStep) throws Exception
    {
        FlavorPicker flavorPicker = flavorStep.getKnowInvalidFlavorFlavorPicker();

        assertFalse(flavorPicker.isInstallFlavorEnabled());

        List<FlavorPane> flavors = flavorPicker.getFlavors();

        assertEquals(1, flavors.size());

        FlavorPane flavor = flavors.get(0);

        assertEquals(FLAVOR_NAME, flavor.getName());
        assertEquals(FLAVOR_ID.getVersion().getValue(), flavor.getVersion());
        assertEquals(FLAVOR_SUMMARY, flavor.getSummary());
        assertEquals("By XWiki Development Team", flavor.getAuthors());

        // Select the flavor
        flavor.select();

        // Install the flavor
        FlavorPickerInstallStep flavorInstall = flavorPicker.installSelectedFlavor();

        upgrade(flavorInstall.getFlavorExtensionPane());

        // Go to next step
        flavorInstall.clickCompleteStep();
    }

    private void upgrade(ExtensionPane extension)
    {
        ExtensionPane upgradeFlavor = extension;
        assertEquals(FLAVOR_NAME, upgradeFlavor.getName());
        assertEquals(FLAVOR_ID.getVersion().getValue(), upgradeFlavor.getVersion());
        assertEquals("remote-installed-invalid", upgradeFlavor.getStatus());

        // Upgrade the flavor
        int timeout = getUtil().getDriver().getTimeout();
        try {
            // 10 minutes should be more than enough to calculate the install plan and do the install
            getUtil().getDriver().setTimeout(600);

            // Start upgrade
            upgradeFlavor = upgradeFlavor.upgrade();

            // Make sure there hasn't been any error or warning during the install plan
            assertNoErrorWarningLog("Unexpected error(s) or warning(s) found in the log during flavor install plan.",
                upgradeFlavor.openProgressSection());

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
            assertNoErrorWarningLog("Unexpected error(s) or warning(s) found in the log during flavor install.",
                upgradeFlavor.openProgressSection());
        } finally {
            getUtil().getDriver().setTimeout(timeout);
        }

        assertEquals("installed", upgradeFlavor.getStatus());
    }

    private void assertNoErrorWarningLog(String message, ExtensionProgressPane progress)
    {
        List<LogItemPane> logs = progress.getJobLog(LogLevel.WARN, LogLevel.ERROR);

        if (!logs.isEmpty()) {
            fail("First one is [" + logs.get(0).getMessage() + "]");
        }
    }

    private void extensionsStep()
    {
        ExtensionsDistributionStep extensionsStep = new ExtensionsDistributionStep();

        // Steps

        List<DistributionStepIcon> icons = extensionsStep.getIcons();

        // Make sure the extensions step is active
        if (!icons.get(2).isActive()) {
            return;
        }

        assertTrue(icons.get(0).isDone());
        assertFalse(icons.get(0).isActive());
        assertEquals(STEP_ADMIN_NAME, icons.get(0).getName());
        assertTrue(icons.get(1).isDone());
        assertFalse(icons.get(1).isActive());
        assertEquals(STEP_FLAVOR_NAME, icons.get(1).getName());
        assertFalse(icons.get(2).isDone());
        assertTrue(icons.get(2).isActive());
        assertEquals(3, icons.get(2).getNumber());
        assertEquals(STEP_EXTENSIONS_NAME, icons.get(2).getName());

        // Search for extension update
        extensionsStep.checkForUpdates();

        // TODO: check some stuff

        // Go to next step
        extensionsStep.clickCompleteStep();
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
