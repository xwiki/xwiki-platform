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
package org.xwiki.tour.test.ui;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;
import org.xwiki.test.ui.po.DeletingPage;
import org.xwiki.test.ui.po.ViewPage;
import org.xwiki.tour.test.po.PageWithTour;
import org.xwiki.tour.test.po.StepEditModal;
import org.xwiki.tour.test.po.TourEditPage;
import org.xwiki.tour.test.po.TourFromLivetable;
import org.xwiki.tour.test.po.TourHomePage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Functional tests for the Tour Application.
 *
 * @version $Id$
 */
@UITest
class TourApplicationIT
{
    private static final LocalDocumentReference TOUR_REFERENCE_1 =
        new LocalDocumentReference(List.of("Tour", "Test"), "WebHome");

    private static final LocalDocumentReference TOUR_REFERENCE_2 =
        new LocalDocumentReference(List.of("Tour", "NewTest"), "WebHome");

    @Test
    @Order(1)
    void verifyTourFeatures(TestUtils setup) throws Exception
    {
        setup.loginAsSuperAdmin();
        // Use a scenario since it's a best practice but also because each tour test has consequences on the other one.
        // For example, the second test binds to a TourClass and thus when displaying the first tour page (which also
        // contains a TourClass) it would display the second tour...

        setup.rest().delete(TOUR_REFERENCE_1);
        setup.rest().delete(TOUR_REFERENCE_2);

        tourBoundToPage();
        tourBoundToClass();
    }

    private void setUpTour(TourEditPage tourEditPage, String description, String targetPage, String targetClass)
    {
        tourEditPage.setDescription(description);
        tourEditPage.setIsActive(true);
        tourEditPage.setTargetPage(targetPage);
        tourEditPage.setTargetClass(targetClass);
    }

    private void setUpStep(TourEditPage tourEditPage, String title, String content, boolean backdrop,
        String targetPage)
    {
        StepEditModal stepEditModal = tourEditPage.newStep();
        stepEditModal.setElement("body");
        stepEditModal.setTitle(title);
        stepEditModal.setContent(content);
        stepEditModal.setBackdrop(backdrop);
        stepEditModal.setTargetPage(targetPage);
        stepEditModal.save();
    }

    private void deleteTour()
    {
        TourHomePage tourHomePage = TourHomePage.gotoPage();
        ViewPage tourPage = tourHomePage.getTourPage("Test");
        DeletingPage deletingPage = tourPage.deletePage().confirmDeletePage();
        deletingPage.waitUntilFinished();
        assertTrue(deletingPage.isSuccess());
    }

    private void tourBoundToPage()
    {
        // First, we need to create a tour
        TourHomePage tourHomePage = TourHomePage.gotoPage();
        TourEditPage tourEditPage = tourHomePage.addNewEntry("Test");
        setUpTour(tourEditPage, "My nice description", "Tour.StartTour.WebHome", "");

        // Test to put a translation key, use the translation macro
        setUpStep(tourEditPage, "tour.app.name", "{{translation key=\"TourCode.TourClass_description\" /}}",
            true, "");
        // I voluntary create the object 3 before the 2 to test the 'order' field
        setUpStep(tourEditPage, "Title 3", "Step 3", true, "");
        setUpStep(tourEditPage, "Title 2", "Step 2", true, "");
        // Add a step that will be removed
        setUpStep(tourEditPage, "to remove", "to remove", false, "");
        // Object 4 used to test the Multipage feature ('targetPage' field)
        setUpStep(tourEditPage, "Title 4", "Step 4", true, "TourCode.TourClass");

        // Test that we can change the order of a step
        StepEditModal stepEditModal = tourEditPage.editStep(2);
        assertEquals("body", stepEditModal.getElement());
        assertEquals("Title 3", stepEditModal.getTitle());
        assertEquals("Step 3", stepEditModal.getContent());
        assertEquals(2, stepEditModal.getOrder());
        assertTrue(stepEditModal.isBackdropEnabled());
        assertEquals("", stepEditModal.getTargetPage());
        stepEditModal.setOrder(3);
        stepEditModal.save();
        stepEditModal = tourEditPage.editStep(2);
        assertEquals("Step 2", stepEditModal.getContent());
        stepEditModal.close();

        // Test that we can remove a step
        stepEditModal = tourEditPage.editStep(4);
        assertEquals("to remove", stepEditModal.getContent());
        stepEditModal.close();
        tourEditPage.deleteStep(4, true);
        stepEditModal = tourEditPage.editStep(4);
        assertEquals("Step 4", stepEditModal.getContent());
        stepEditModal.close();

        // Save the tour...
        tourEditPage.clickSaveAndView();

        // Verify that the tour is displayed in the LT
        tourHomePage = TourHomePage.gotoPage();
        // Note that non-existing pages are considered (based on the meta tags in the HTML head) nested pages (even when
        // the URL doesn't end with a slash) and since we're not creating (saving) the target page for our tour it means
        // we have to use a nested page reference. Which is not bad because this way we also test how the tour behaves
        // with a nested page as target.
        assertTrue(
            tourHomePage.getTours().contains(new TourFromLivetable("Test", "Tour.StartTour.WebHome", true, "-")));

        // Try the tour by navigating to its target page (it'll be started automatically)
        LocalDocumentReference targetPageReference =
            new LocalDocumentReference(Arrays.asList("Tour", "StartTour"), "WebHome");
        PageWithTour homePage = PageWithTour.gotoPage(targetPageReference);
        assertTrue(homePage.isTourDisplayed());

        // Step 1
        assertEquals("Tour", homePage.getStepTitle());
        assertEquals("Description", homePage.getStepDescription());
        assertTrue(homePage.hasNextStep());
        assertFalse(homePage.hasPreviousStep());
        assertFalse(homePage.hasEndButton());

        // Step 2
        homePage.nextStep();
        assertEquals("Title 2", homePage.getStepTitle());
        assertEquals("Step 2", homePage.getStepDescription());
        assertTrue(homePage.hasNextStep());
        assertTrue(homePage.hasPreviousStep());
        assertFalse(homePage.hasEndButton());

        // Go back to step 1
        homePage.previousStep();
        assertEquals("Tour", homePage.getStepTitle());
        assertEquals("Description", homePage.getStepDescription());
        assertTrue(homePage.hasNextStep());
        assertFalse(homePage.hasPreviousStep());
        assertFalse(homePage.hasEndButton());

        // Go back to step 2
        homePage.nextStep();
        assertEquals("Title 2", homePage.getStepTitle());
        assertEquals("Step 2", homePage.getStepDescription());
        assertTrue(homePage.hasNextStep());
        assertTrue(homePage.hasPreviousStep());
        assertFalse(homePage.hasEndButton());

        // Step 3
        homePage.nextStep();
        assertEquals("Title 3", homePage.getStepTitle());
        assertEquals("Step 3", homePage.getStepDescription());
        assertTrue(homePage.hasNextStep());
        assertTrue(homePage.hasPreviousStep());
        assertFalse(homePage.hasEndButton());

        // Step 4
        homePage.nextStep();
        // Use a second page to test the Multipage feature
        PageWithTour secondPage = new PageWithTour();
        assertTrue(secondPage.getUrl().endsWith("TourCode/TourClass"));
        assertEquals("Title 4", secondPage.getStepTitle());
        assertEquals("Step 4", secondPage.getStepDescription());
        assertFalse(secondPage.hasNextStep());
        assertTrue(secondPage.hasPreviousStep());
        assertTrue(secondPage.hasEndButton());

        // End
        secondPage.end();
        assertFalse(secondPage.isTourDisplayed());
        assertTrue(secondPage.hasResumeButton());

        // Resume (to step 1, as we have ended the tour)
        secondPage.resume();
        assertTrue(secondPage.isTourDisplayed());
        assertFalse(secondPage.hasResumeButton());
        assertEquals("Tour", secondPage.getStepTitle());
        assertEquals("Description", secondPage.getStepDescription());
        assertTrue(secondPage.hasNextStep());
        assertFalse(secondPage.hasPreviousStep());
        assertFalse(secondPage.hasEndButton());

        // Go one step forward
        secondPage.nextStep();
        assertEquals("Title 2", secondPage.getStepTitle());
        assertEquals("Step 2", secondPage.getStepDescription());
        assertTrue(secondPage.hasNextStep());
        assertTrue(secondPage.hasPreviousStep());
        assertFalse(secondPage.hasEndButton());

        // Close the tour (it should start from where we left it)
        secondPage.close();
        assertFalse(secondPage.isTourDisplayed());
        assertTrue(secondPage.hasResumeButton());

        // Resume the tour
        secondPage.resume();
        assertTrue(secondPage.isTourDisplayed());
        assertFalse(secondPage.hasResumeButton());
        assertEquals("Title 2", secondPage.getStepTitle());
        assertEquals("Step 2", secondPage.getStepDescription());
        assertTrue(secondPage.hasNextStep());
        assertTrue(secondPage.hasPreviousStep());
        assertFalse(secondPage.hasEndButton());

        // Close
        secondPage.close();
        assertFalse(secondPage.isTourDisplayed());
        assertTrue(secondPage.hasResumeButton());

        // Go to an other page and then go back
        secondPage = PageWithTour.gotoPage("TourCode", "TourClass");
        assertFalse(secondPage.isTourDisplayed());
        assertTrue(secondPage.hasResumeButton());

        secondPage = PageWithTour.gotoPage(targetPageReference);
        assertFalse(secondPage.isTourDisplayed());
        assertTrue(secondPage.hasResumeButton());

        // Resume the tour
        secondPage.resume();
        assertTrue(secondPage.isTourDisplayed());
        assertFalse(secondPage.hasResumeButton());
        assertEquals("Title 2", secondPage.getStepTitle());
        assertEquals("Step 2", secondPage.getStepDescription());
        assertTrue(secondPage.hasNextStep());
        assertTrue(secondPage.hasPreviousStep());
        assertFalse(secondPage.hasEndButton());

        // Close
        secondPage.close();

        // Go back to the tour homepage
        TourHomePage.gotoPage();
        // Launch the tour (from the livetable)
        homePage = tourHomePage.startTour("Test");
        // So the step 1 is active
        assertEquals("Tour", homePage.getStepTitle());
        assertEquals("Description", homePage.getStepDescription());
        assertTrue(homePage.hasNextStep());
        assertFalse(homePage.hasPreviousStep());
        assertFalse(homePage.hasEndButton());
        homePage.close();

        // Verify that we can delete a tour in the LT UI
        deleteTour();
    }

    private void tourBoundToClass()
    {
        // First, we need to create a tour
        TourHomePage tourHomePage = TourHomePage.gotoPage();
        TourEditPage tourEditPage = tourHomePage.addNewEntry("NewTest");
        setUpTour(tourEditPage, "Description", "", "TourCode.TourClass");
        setUpStep(tourEditPage, "Tour Title", "Tour Content", true, "");
        tourEditPage.clickSaveAndView();

        tourHomePage = TourHomePage.gotoPage();
        assertTrue(tourHomePage.getTours().contains(new TourFromLivetable("NewTest", "-", true, "TourCode.TourClass")));

        PageWithTour homePage = PageWithTour.gotoPage("Tour", "NewTest");
        assertTrue(homePage.isTourDisplayed());
        homePage.end();
    }
}
