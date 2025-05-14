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
package org.xwiki.security.test.ui;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Order;
import org.junit.jupiter.params.ParameterizedTest;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.test.docker.junit5.TestLocalReference;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.docker.junit5.WikisSource;
import org.xwiki.test.ui.TestUtils;
import org.xwiki.test.ui.po.InformationPane;
import org.xwiki.test.ui.po.RequiredRightsModal;
import org.xwiki.test.ui.po.ViewPage;
import org.xwiki.test.ui.po.editor.WikiEditPage;
import org.xwiki.text.StringUtils;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * UI integration tests for the required rights feature.
 *
 * @version $Id$
 */
@UITest
class RequiredRightsIT
{
    @ParameterizedTest
    @WikisSource(extensions = "org.xwiki.platform:xwiki-platform-security-requiredrights-ui")
    @Order(1)
    void enforceRequiredRightsOnPlainPage(WikiReference wiki, TestLocalReference testLocalReference, TestUtils setup)
        throws Exception
    {
        DocumentReference testReference = new DocumentReference(testLocalReference, wiki);

        setup.loginAsSuperAdmin();

        // Delete the page just to be sure.
        setup.rest().delete(testReference);

        ViewPage viewPage = setup.createPage(testReference, "Content");

        InformationPane informationPane = viewPage.openInformationDocExtraPane();
        assertThat(informationPane.getRequiredRightsStatusMessage(), containsString("not enforcing any right"));
        assertEquals(List.of(), informationPane.getRequiredRights());
        assertEquals("Review the required rights and enforce them to increase the security of this page.",
            informationPane.getRequiredRightsModificationMessage().orElseThrow());
        assertTrue(informationPane.canReviewRequiredRights());
        RequiredRightsModal requiredRightsModal = informationPane.openRequiredRightsModal();
        assertTrue(requiredRightsModal.isDisplayed());
        assertFalse(requiredRightsModal.isEnforceRequiredRights());
        requiredRightsModal.setEnforceRequiredRights(true);
        List<RequiredRightsModal.RequiredRight> requiredRights = requiredRightsModal.getRequiredRights();
        assertEquals(4, requiredRights.size());
        assertEquals("None", requiredRights.get(0).label());
        assertEquals("enough", requiredRights.get(0).suggestionClass());
        assertEquals("Enough", requiredRights.get(0).suggestionText());
        assertEquals("The automated analysis hasn't found any content that requires any rights.",
            requiredRights.get(0).suggestionTooltip());
        assertEquals("Script", requiredRights.get(1).label());
        assertEquals("Wiki Admin", requiredRights.get(2).label());
        assertEquals("Programming", requiredRights.get(3).label());
        for (int i = 1; i < 4; ++i) {
            assertNull(requiredRights.get(i).suggestionText());
            assertNull(requiredRights.get(i).suggestionTooltip());
            assertNull(requiredRights.get(i).suggestionClass());
        }
        assertEquals("", requiredRightsModal.setEnforcedRequiredRight(""));
        assertFalse(requiredRightsModal.hasAnalysisDetails());
        requiredRightsModal.clickSave(true);

        // Wait for the required rights information to reload.
        setup.getDriver()
            .waitUntilCondition(driver -> StringUtils.contains(informationPane.getRequiredRightsStatusMessage(),
                "This page is enforcing required rights but no rights"));
        assertEquals(List.of(), informationPane.getRequiredRights());
        assertFalse(informationPane.getRequiredRightsModificationMessage().isPresent());
    }

    @ParameterizedTest
    @WikisSource(extensions = "org.xwiki.platform:xwiki-platform-security-requiredrights-ui")
    @Order(2)
    void enforceRequiredRightsOnDocumentThatMightNeedScriptRight(WikiReference wiki,
        TestLocalReference testLocalReference, TestUtils setup) throws Exception
    {
        DocumentReference testReference = new DocumentReference(testLocalReference, wiki);

        setup.rest().delete(testReference);

        ViewPage viewPage = setup.createPage(testReference, "Content");
        enabledRequiredRights(viewPage, setup);

        // Add an HTML macro with wiki="true": in this case, script right might be needed, but no right might also be
        // enough.
        WikiEditPage wikiEditPage = viewPage.editWiki();
        wikiEditPage.setContent("{{html wiki=\"true\"}}{{/html}}");
        viewPage = wikiEditPage.clickSaveAndView();
        // No need to wait, the warning would have been present in the initial page load.
        assertFalse(viewPage.hasRequiredRightsWarning(false));
        InformationPane informationPane = viewPage.openInformationDocExtraPane();
        assertThat(informationPane.getRequiredRightsStatusMessage(),
            containsString("This page is enforcing required rights"));
        assertEquals(List.of(), informationPane.getRequiredRights());
        assertThat(informationPane.getRequiredRightsModificationMessage().orElseThrow(),
            containsString("This document's content might be missing a required right"));
        RequiredRightsModal requiredRightsModal = informationPane.openRequiredRightsModal();
        assertTrue(requiredRightsModal.isDisplayed());
        assertTrue(requiredRightsModal.isEnforceRequiredRights());
        List<RequiredRightsModal.RequiredRight> requiredRights = requiredRightsModal.getRequiredRights();
        assertEquals("maybe-enough", requiredRights.get(0).suggestionClass());
        assertEquals("Might be enough", requiredRights.get(0).suggestionText());
        assertEquals("maybe-required", requiredRights.get(1).suggestionClass());
        assertEquals("Might be required", requiredRights.get(1).suggestionText());
        assertEquals("script", requiredRightsModal.setEnforcedRequiredRight("script"));
        assertTrue(requiredRightsModal.hasAnalysisDetails());
        assertFalse(requiredRightsModal.isAnalysisDetailsDisplayed());
        requiredRightsModal.toggleAnalysisDetails();
        assertTrue(requiredRightsModal.isAnalysisDetailsDisplayed());
        requiredRightsModal.clickSave(true);
        setup.getDriver().waitUntilCondition(driver -> informationPane.getRequiredRights().contains("Script right"));
        assertThat(informationPane.getRequiredRightsStatusMessage(),
            containsString("This page is enforcing required rights. The following"));
        assertThat(informationPane.getRequiredRightsModificationMessage().orElseThrow(),
            containsString("This document's content might not need the configured required"));
    }

    @ParameterizedTest
    @WikisSource(extensions = "org.xwiki.platform:xwiki-platform-security-requiredrights-ui")
    @Order(3)
    void enforceRequiredRightsOnDocumentThatNeedsScriptRight(WikiReference wiki, TestLocalReference testLocalReference,
        TestUtils setup) throws Exception
    {
        DocumentReference testReference = new DocumentReference(testLocalReference, wiki);

        setup.rest().delete(testReference);

        ViewPage viewPage = setup.createPage(testReference, "Content");
        enabledRequiredRights(viewPage, setup);

        // HTML macro with clean=false definitely requires script right.
        WikiEditPage wikiEditPage = viewPage.editWiki();
        wikiEditPage.setContent("{{html clean=\"false\"}}{{/html}}");
        viewPage = wikiEditPage.clickSaveAndView();
        // No need to wait, but as we expect the warning to be there, waiting doesn't harm, either.
        assertTrue(viewPage.hasRequiredRightsWarning(true));
        InformationPane informationPane = viewPage.openInformationDocExtraPane();
        assertThat(informationPane.getRequiredRightsStatusMessage(),
            containsString("This page is enforcing required rights"));
        assertEquals(List.of(), informationPane.getRequiredRights());
        assertThat(informationPane.getRequiredRightsModificationMessage().orElseThrow(),
            containsString("This document's content is missing a required right."));
        RequiredRightsModal requiredRightsModal = viewPage.openRequiredRightsModal();
        assertTrue(requiredRightsModal.isDisplayed());
        assertTrue(requiredRightsModal.isEnforceRequiredRights());
        List<RequiredRightsModal.RequiredRight> requiredRights = requiredRightsModal.getRequiredRights();
        assertNull(requiredRights.get(0).suggestionClass());
        assertNull(requiredRights.get(0).suggestionText());
        assertEquals("required", requiredRights.get(1).suggestionClass());
        assertEquals("Required", requiredRights.get(1).suggestionText());
        assertEquals("script", requiredRightsModal.setEnforcedRequiredRight("script"));
        assertTrue(requiredRightsModal.hasAnalysisDetails());
        assertFalse(requiredRightsModal.isAnalysisDetailsDisplayed());
        requiredRightsModal.toggleAnalysisDetails();
        assertTrue(requiredRightsModal.isAnalysisDetailsDisplayed());
        requiredRightsModal.clickSave(true);
        setup.getDriver().waitUntilCondition(driver -> informationPane.getRequiredRights().contains("Script right"));
        assertThat(informationPane.getRequiredRightsStatusMessage(),
            containsString("This page is enforcing required rights. The following"));
        assertTrue(informationPane.getRequiredRightsModificationMessage().isEmpty());
        // The warning should disappear - wait for that to happen.
        ViewPage finalViewPage = viewPage;
        setup.getDriver().waitUntilCondition(driver -> !finalViewPage.hasRequiredRightsWarning(false));

        // Remove the required right again and verify that the warning is back. This also verifies that the modal can
        // be opened from the reloaded information.
        requiredRightsModal = informationPane.openRequiredRightsModal();
        requiredRightsModal.setEnforcedRequiredRight("");
        requiredRightsModal.clickSave(true);
        assertTrue(viewPage.hasRequiredRightsWarning(true));
        // Ensure that we can open the modal from the reloaded warning.
        requiredRightsModal = viewPage.openRequiredRightsModal();
        assertTrue(requiredRightsModal.isDisplayed());
        requiredRightsModal.clickCancel();
    }

    @ParameterizedTest
    @WikisSource(extensions = "org.xwiki.platform:xwiki-platform-security-requiredrights-ui")
    @Order(4)
    void testAllSupportedValues(WikiReference wiki, TestLocalReference testLocalReference, TestUtils setup)
        throws Exception
    {
        DocumentReference testReference = new DocumentReference(testLocalReference, wiki);

        setup.rest().delete(testReference);

        ViewPage viewPage = setup.createPage(testReference, "Content");
        enabledRequiredRights(viewPage, setup);

        InformationPane informationPane = viewPage.openInformationDocExtraPane();

        String previousRight = "";

        Map<String, String> displayForRight =
            Map.of("script", "Script right", "admin", "Admin right on the wiki level", "programming",
                "Programming right");

        for (Map.Entry<String, String> entry : displayForRight.entrySet()) {
            String right = entry.getKey();
            String display = entry.getValue();

            RequiredRightsModal requiredRightsModal = informationPane.openRequiredRightsModal();
            assertEquals(previousRight, requiredRightsModal.getEnforcedRequiredRight());

            assertEquals(right, requiredRightsModal.setEnforcedRequiredRight(right));
            requiredRightsModal.clickSave(true);

            setup.getDriver().waitUntilCondition(driver -> informationPane.getRequiredRights().contains(display));
            assertEquals(List.of(display), informationPane.getRequiredRights());

            previousRight = right;
        }

        RequiredRightsModal requiredRightsModal = informationPane.openRequiredRightsModal();
        assertEquals(previousRight, requiredRightsModal.getEnforcedRequiredRight());
        requiredRightsModal.clickCancel();
    }

    private static void enabledRequiredRights(ViewPage viewPage, TestUtils setup)
    {
        InformationPane informationPane = viewPage.openInformationDocExtraPane();
        RequiredRightsModal requiredRightsModal = informationPane.openRequiredRightsModal();
        requiredRightsModal.setEnforceRequiredRights(true);
        requiredRightsModal.setEnforcedRequiredRight("");
        requiredRightsModal.clickSave(true);
        setup.getDriver()
            .waitUntilCondition(driver -> StringUtils.contains(informationPane.getRequiredRightsStatusMessage(),
                "This page is enforcing required rights but no rights"));
    }
}
