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
import org.apache.commons.lang3.StringUtils;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.extension.ExtensionId;
import org.xwiki.extension.internal.ExtensionUtils;
import org.xwiki.extension.internal.converter.ExtensionIdConverter;
import org.xwiki.extension.test.ExtensionTestUtils;
import org.xwiki.extension.test.po.ExtensionPane;
import org.xwiki.extension.test.po.ExtensionProgressPane;
import org.xwiki.extension.test.po.LogItemPane;
import org.xwiki.extension.test.po.distribution.CleanApplyDistributionStep;
import org.xwiki.extension.test.po.distribution.CleanApplyFinalizeDistributionStep;
import org.xwiki.extension.test.po.distribution.CleanApplyReportDistributionStep;
import org.xwiki.extension.test.po.distribution.CleanDistributionStep;
import org.xwiki.extension.test.po.distribution.DistributionStepIcon;
import org.xwiki.extension.test.po.distribution.ExtensionsDistributionStep;
import org.xwiki.extension.test.po.distribution.FlavorDistributionStep;
import org.xwiki.extension.test.po.distribution.ReportDistributionStep;
import org.xwiki.extension.test.po.distribution.WelcomeDistributionStep;
import org.xwiki.extension.test.po.flavor.FlavorPane;
import org.xwiki.extension.test.po.flavor.FlavorPicker;
import org.xwiki.extension.test.po.flavor.FlavorPickerInstallStep;
import org.xwiki.logging.LogLevel;
import org.xwiki.model.namespace.WikiNamespace;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.test.integration.XWikiExecutor;
import org.xwiki.test.integration.junit.LogCaptureValidator;
import org.xwiki.test.ui.po.ViewPage;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Validate the Distribution Wizard part of the upgrade process.
 * 
 * @version $Id$
 * @since 10.7RC1
 */
public class UpgradeTest extends AbstractTest
{
    protected static final ExtensionId EXTENSIONID_WATCHLIST_UI =
        new ExtensionId("org.xwiki.platform:xwiki-platform-watchlist-ui");

    protected static final ExtensionId EXTENSIONID_CODEMIRROR_58 = new ExtensionId("org.webjars:codemirror", "5.8");

    protected static final ExtensionId EXTENSIONID_CODEMIRROR_5242 =
        new ExtensionId("org.webjars:codemirror", "5.24.2");

    protected static ExtensionTestUtils extensionTestUtil;

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

    private static final int STEP_ADMIN_ID = 0;

    private static final String STEP_FLAVOR_NAME = "Flavor";

    private static final int STEP_FLAVOR_ID = 1;

    private static final String STEP_ORPHANED_NAME = "Orphaned dependencies";

    private static final int STEP_ORPHANED_ID = 2;

    private static final String STEP_EXTENSIONS_NAME = "Extensions";

    private static final int STEP_EXTENSIONS_ID = 3;

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

        // Initializing XWiki takes longer with the migrations
        executor.setTimeoutSeconds(300);

        /////////////////////
        // Configure

        PropertiesConfiguration properties = executor.loadXWikiPropertiesConfiguration();

        // Disable extension repositories to make sure it only look at local extensions
        properties.setProperty("extension.repositories", "");

        // Disable Active Installs ping since there's no ElasticSearch instance setup and it's not required by the
        // test.
        properties.setProperty("activeinstalls.pingURL", "");

        executor.saveXWikiProperties();

        /////////////////////
        // Init and start

        init(Arrays.asList(executor));

        // Use Admin credentials since superadmin is not enabled by default
        extensionTestUtil = new ExtensionTestUtils(getUtil(), TestUtils.ADMIN_CREDENTIALS);
    }

    protected void assertInstalledOnMainWiki(ExtensionId extensionId) throws Exception
    {
        assertTrue(extensionTestUtil.isInstalled(extensionId, new WikiNamespace("xwiki")));
    }

    protected void assertNotInstalledOnMainWiki(ExtensionId extensionId) throws Exception
    {
        assertFalse(extensionTestUtil.isInstalled(extensionId, new WikiNamespace("xwiki")));
    }

    /**
     * @since 12.9RC1
     */
    protected void assertNotInstalled(ExtensionId extensionId) throws Exception
    {
        assertFalse(extensionTestUtil.isInstalled(extensionId));
    }

    // Test

    protected void setupLogs()
    {
        // Extended to ignore expected logs
    }

    /**
     * Execute the Distribution Wizard for an upgrade from previous version to current SNAPSHOT.
     * 
     * @throws Exception when failing the test
     */
    @Test
    public void upgrade() throws Exception
    {
        // Setup logs ignores
        setupLogs();

        // Access home page (and be automatically redirected)
        getUtil().gotoPage("Main", "WebHome", "view");

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
        // Validate Orphaned Dependencies step

        orphanedDependenciesStep();

        ////////////////////
        // Validate Outdated Extensions step

        extensionsStep();

        ////////////////////
        // Validate Report step

        reportStep();

        // Make sure we are back on home page
        ViewPage page = new ViewPage();

        assertEquals("xwiki:Main.WebHome", page.getMetaDataValue("reference"));

        ////////////////////
        // Common Validations

        // Make sure the watchlist UI has been uninstalled
        assertNotInstalledOnMainWiki(EXTENSIONID_WATCHLIST_UI);

        // Make sure the previous codemirror versions have been replaced
        assertNotInstalled(EXTENSIONID_CODEMIRROR_58);
        assertNotInstalled(EXTENSIONID_CODEMIRROR_5242);

        // Make sure it's possible to create a page with 768 characters in the reference
        getUtil().rest()
            .savePage(new LocalDocumentReference("Upgrade", StringUtils.repeat("a", 768 - "Upgrade".length() - 1)));

        ////////////////////
        // Custom validation

        postUpdateValidate();
    }

    protected void postUpdateValidate() throws Exception
    {

    }

    private void welcomeStep()
    {
        WelcomeDistributionStep step = new WelcomeDistributionStep();

        // Steps

        List<DistributionStepIcon> icons = step.getIcons();

        assertFalse(icons.get(STEP_ADMIN_ID).isDone());
        assertFalse(icons.get(STEP_ADMIN_ID).isActive());
        assertEquals(STEP_ADMIN_ID + 1, icons.get(STEP_ADMIN_ID).getNumber());
        assertEquals(STEP_ADMIN_NAME, icons.get(STEP_ADMIN_ID).getName());
        assertFalse(icons.get(STEP_FLAVOR_ID).isDone());
        assertFalse(icons.get(STEP_FLAVOR_ID).isActive());
        assertEquals(2, icons.get(STEP_FLAVOR_ID).getNumber());
        assertEquals(STEP_FLAVOR_ID + 1, icons.get(STEP_FLAVOR_ID).getNumber());
        assertEquals(STEP_FLAVOR_NAME, icons.get(STEP_FLAVOR_ID).getName());
        assertFalse(icons.get(STEP_ORPHANED_ID).isDone());
        assertFalse(icons.get(STEP_ORPHANED_ID).isActive());
        assertEquals(3, icons.get(STEP_ORPHANED_ID).getNumber());
        assertEquals(STEP_ORPHANED_ID + 1, icons.get(STEP_ORPHANED_ID).getNumber());
        assertEquals(STEP_ORPHANED_NAME, icons.get(STEP_ORPHANED_ID).getName());
        assertFalse(icons.get(STEP_EXTENSIONS_ID).isDone());
        assertFalse(icons.get(STEP_EXTENSIONS_ID).isActive());
        assertEquals(4, icons.get(STEP_EXTENSIONS_ID).getNumber());
        assertEquals(STEP_EXTENSIONS_ID + 1, icons.get(STEP_EXTENSIONS_ID).getNumber());
        assertEquals(STEP_EXTENSIONS_NAME, icons.get(STEP_EXTENSIONS_ID).getName());

        // Go to next step
        step.clickCompleteStep();
    }

    private void flavorStep() throws Exception
    {
        FlavorDistributionStep step = new FlavorDistributionStep();

        // Steps
        List<DistributionStepIcon> icons = step.getIcons();

        assertTrue(icons.get(STEP_ADMIN_ID).isDone());
        assertFalse(icons.get(STEP_ADMIN_ID).isActive());
        assertEquals(STEP_ADMIN_NAME, icons.get(STEP_ADMIN_ID).getName());
        assertFalse(icons.get(STEP_FLAVOR_ID).isDone());
        assertTrue(icons.get(STEP_FLAVOR_ID).isActive());
        assertEquals(STEP_FLAVOR_NAME, icons.get(STEP_FLAVOR_ID).getName());
        assertFalse(icons.get(STEP_ORPHANED_ID).isDone());
        assertFalse(icons.get(STEP_ORPHANED_ID).isActive());
        assertEquals(STEP_ORPHANED_NAME, icons.get(STEP_ORPHANED_ID).getName());
        assertFalse(icons.get(STEP_EXTENSIONS_ID).isDone());
        assertFalse(icons.get(STEP_EXTENSIONS_ID).isActive());
        assertEquals(STEP_EXTENSIONS_NAME, icons.get(STEP_EXTENSIONS_ID).getName());

        // Make sure complete step is disabled
        assertFalse(step.isCompleteStepDisabled());

        // Check current flavor
        ExtensionPane currentFlavor = step.getCurrentFlavorExtensionPane();
        assertEquals(PREVIOUSFLAVOR_NAME, currentFlavor.getName());
        assertEquals(PREVIOUSFLAVOR_ID.getVersion().getValue(), currentFlavor.getVersion());
        assertEquals("installed-invalid", currentFlavor.getStatus());

        // Flavor upgrade
        if (KNOW_VALID_FLAVORS.contains(PREVIOUSFLAVOR_ID.getId())) {
            flavorStepKnownValidUpgrade(step);
        } else if (KNOW_INVALID_FLAVORS.contains(PREVIOUSFLAVOR_ID.getId())) {
            flavorStepKnownInvalidUpgrade(step);
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
            // 20 minutes should be more than enough to calculate the install plan and do the install
            getUtil().getDriver().setTimeout(1200);

            // Start upgrade
            upgradeFlavor = upgradeFlavor.upgrade();

            // Make sure there hasn't been any error or warning during the install plan
            assertNoErrorWarningLog(upgradeFlavor.openProgressSection());

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
            assertNoErrorWarningLog(upgradeFlavor.openProgressSection());
        } finally {
            getUtil().getDriver().setTimeout(timeout);
        }

        assertEquals("installed", upgradeFlavor.getStatus());
    }

    private void assertNoErrorWarningLog(ExtensionProgressPane progress)
    {
        List<LogItemPane> logs = progress.getJobLog(LogLevel.WARN, LogLevel.ERROR);

        StringBuilder builder = new StringBuilder();
        for (LogItemPane log : logs) {
            builder.append(log.getMessage());
            builder.append('\n');
        }

        LogCaptureValidator validator = new LogCaptureValidator();
        validator.validate(builder.toString(), validateConsole.getLogCaptureConfiguration(), false);
    }

    private void orphanedDependenciesStep()
    {
        CleanDistributionStep step = new CleanDistributionStep();

        // Steps

        List<DistributionStepIcon> icons = step.getIcons();

        // Make sure the extensions step is active
        if (!icons.get(STEP_ORPHANED_ID).isActive()) {
            return;
        }

        assertTrue(icons.get(STEP_ADMIN_ID).isDone());
        assertFalse(icons.get(STEP_ADMIN_ID).isActive());
        assertEquals(STEP_ADMIN_NAME, icons.get(STEP_ADMIN_ID).getName());
        assertTrue(icons.get(STEP_FLAVOR_ID).isDone());
        assertFalse(icons.get(STEP_FLAVOR_ID).isActive());
        assertEquals(STEP_FLAVOR_NAME, icons.get(STEP_FLAVOR_ID).getName());
        assertFalse(icons.get(STEP_ORPHANED_ID).isDone());
        assertTrue(icons.get(STEP_ORPHANED_ID).isActive());
        assertEquals(STEP_ORPHANED_NAME, icons.get(STEP_ORPHANED_ID).getName());
        assertFalse(icons.get(STEP_EXTENSIONS_ID).isDone());
        assertFalse(icons.get(STEP_EXTENSIONS_ID).isActive());
        assertEquals(STEP_EXTENSIONS_NAME, icons.get(STEP_EXTENSIONS_ID).getName());

        // Confirm the extension to uninstall/make top level
        CleanApplyDistributionStep cleanApply = step.clickContinue();

        // Validate the plan
        CleanApplyFinalizeDistributionStep cleanApplyFinalize = cleanApply.clickContinue();

        // Wait for uninstall to finish
        cleanApplyFinalize.waitForUninstallComplete();

        // Get a report
        CleanApplyReportDistributionStep applyReport = cleanApplyFinalize.clickContinue();

        // Go to next step
        applyReport.clickCompleteStep();
    }

    private void extensionsStep()
    {
        ExtensionsDistributionStep step = new ExtensionsDistributionStep();

        // Steps

        List<DistributionStepIcon> icons = step.getIcons();

        // Make sure the extensions step is active
        if (!icons.get(STEP_EXTENSIONS_ID).isActive()) {
            return;
        }

        assertTrue(icons.get(STEP_ADMIN_ID).isDone());
        assertFalse(icons.get(STEP_ADMIN_ID).isActive());
        assertEquals(STEP_ADMIN_NAME, icons.get(STEP_ADMIN_ID).getName());
        assertTrue(icons.get(STEP_FLAVOR_ID).isDone());
        assertFalse(icons.get(STEP_FLAVOR_ID).isActive());
        assertEquals(STEP_FLAVOR_NAME, icons.get(STEP_FLAVOR_ID).getName());
        assertTrue(icons.get(STEP_ORPHANED_ID).isDone());
        assertFalse(icons.get(STEP_ORPHANED_ID).isActive());
        assertEquals(STEP_ORPHANED_NAME, icons.get(STEP_ORPHANED_ID).getName());
        assertFalse(icons.get(STEP_EXTENSIONS_ID).isDone());
        assertTrue(icons.get(STEP_EXTENSIONS_ID).isActive());
        assertEquals(STEP_EXTENSIONS_NAME, icons.get(STEP_EXTENSIONS_ID).getName());

        // Search for extension update
        step.checkForUpdates();

        // TODO: check some stuff

        // Go to next step
        step.clickCompleteStep();
    }

    private void reportStep()
    {
        ReportDistributionStep step = new ReportDistributionStep();

        // Steps
        List<DistributionStepIcon> icons = step.getIcons();

        assertTrue(icons.get(STEP_ADMIN_ID).isDone());
        assertFalse(icons.get(STEP_ADMIN_ID).isActive());
        assertEquals(STEP_ADMIN_NAME, icons.get(STEP_ADMIN_ID).getName());
        assertTrue(icons.get(STEP_FLAVOR_ID).isDone());
        assertFalse(icons.get(STEP_FLAVOR_ID).isActive());
        assertEquals(STEP_FLAVOR_NAME, icons.get(STEP_FLAVOR_ID).getName());
        assertTrue(icons.get(STEP_ORPHANED_ID).isDone());
        assertFalse(icons.get(STEP_ORPHANED_ID).isActive());
        assertEquals(STEP_ORPHANED_NAME, icons.get(STEP_ORPHANED_ID).getName());
        assertTrue(icons.get(STEP_EXTENSIONS_ID).isDone());
        assertFalse(icons.get(STEP_EXTENSIONS_ID).isActive());
        assertEquals(STEP_EXTENSIONS_NAME, icons.get(STEP_EXTENSIONS_ID).getName());

        // Finish
        step.clickCompleteStep();
    }
}
