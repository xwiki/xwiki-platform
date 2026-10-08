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

import org.junit.jupiter.api.Test;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;
import org.xwiki.tour.test.po.PageWithTour;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Validate the Homepage tour shipped with the standard flavor.
 *
 * @version $Id$
 * @since 18.9.0RC1
 */
// standardFlavor = true because the Homepage tour is defined by xwiki-platform-distribution-flavor-tour, which reaches
// the users only through the standard flavor.
@UITest(standardFlavor = true)
class HomepageTourIT
{
    private static final LocalDocumentReference HOME_PAGE = new LocalDocumentReference("Main", "WebHome");

    private static final List<String> STEP_TITLES = List.of("Welcome to XWiki", "Breadcrumbs", "Page Menu", "Header",
        "Panels", "Page Content", "Page Tabs", "Tour Complete");

    private static final String CLOSED_STEP_TITLE = "Header";

    @Test
    void homepageTour(TestUtils setup)
    {
        // Visit the home page as a new visitor would: as guest and without any tour state in the browser.
        setup.forceGuestUser();
        PageWithTour homePage = PageWithTour.gotoPage(HOME_PAGE);
        homePage.clearToursState();
        homePage = PageWithTour.gotoPage(HOME_PAGE);

        // The tour starts by itself.
        assertTrue(homePage.isTourDisplayed());
        assertEquals(STEP_TITLES.get(0), homePage.getStepTitle());
        assertFalse(homePage.hasPreviousStep());

        for (int i = 1; i < STEP_TITLES.size(); i++) {
            homePage.nextStep();
            assertEquals(STEP_TITLES.get(i), homePage.getStepTitle());
            // Check that we can go back to the previous step at every step.
            assertTrue(homePage.hasPreviousStep());
            homePage.previousStep();
            assertEquals(STEP_TITLES.get(i - 1), homePage.getStepTitle());
            homePage.nextStep();
            assertEquals(STEP_TITLES.get(i), homePage.getStepTitle());

            if (CLOSED_STEP_TITLE.equals(STEP_TITLES.get(i))) {
                // Close the tour (this waits for it to disappear), then resume it: it shows the last seen step.
                homePage.close();
                assertTrue(homePage.hasResumeButton());
                homePage.resume();
                assertEquals(CLOSED_STEP_TITLE, homePage.getStepTitle());
            }
        }

        // The last step links to the documentation of the XWiki features.
        assertFalse(homePage.hasNextStep());
        assertTrue(homePage.hasEndButton());
        assertEquals("http://www.xwiki.org/xwiki/bin/view/Documentation/UserGuide/Features/",
            homePage.getStepDescriptionLinkURL("documentation"));

        // Coming back to the home page (e.g. after following the documentation link) shows the last seen step.
        homePage = PageWithTour.gotoPage(HOME_PAGE);
        assertTrue(homePage.isTourDisplayed());
        assertEquals(STEP_TITLES.get(STEP_TITLES.size() - 1), homePage.getStepTitle());

        // End the tour (this waits for it to disappear): a button to restart it is displayed instead.
        homePage.end();
        assertTrue(homePage.hasResumeButton());
        assertEquals("You can restart the tour by clicking this button at anytime", homePage.getResumeHint());
    }
}
