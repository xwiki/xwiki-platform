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
package org.xwiki.contrib.tour.test.ui;

import org.junit.Rule;
import org.junit.Test;
import org.xwiki.contrib.tour.test.po.PageWithTour;
import org.xwiki.contrib.tour.test.po.TourFromLivetable;
import org.xwiki.contrib.tour.test.po.TourEditPage;
import org.xwiki.contrib.tour.test.po.TourHomePage;
import org.xwiki.test.ui.AbstractTest;
import org.xwiki.test.ui.SuperAdminAuthenticationRule;
import org.xwiki.test.ui.po.ViewPage;
import org.xwiki.test.ui.po.editor.ObjectEditPage;
import org.xwiki.test.ui.po.editor.ObjectEditPane;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @version $Id: $
 * @since 0.2 
 */
public class TourApplicationTest extends AbstractTest
{
    @Rule
    public SuperAdminAuthenticationRule superAdminAuthenticationRule =
            new SuperAdminAuthenticationRule(getUtil(), getDriver());

    private void setUpTour() throws Exception
    {
        // First, we need to create a tour
        TourHomePage tourHomePage = TourHomePage.gotoPage();
        TourEditPage tourEditPage = tourHomePage.addNewEntry("Test");
        tourEditPage.setDescription("My nice description");
        tourEditPage.setIsActive(true);
        tourEditPage.setTargetPage("Main.WebHome");
        tourEditPage.clickSaveAndView();
        // Todo: replace by a nice UI
        ObjectEditPage objectEditPage = tourEditPage.editObjects();
        ObjectEditPane obj1 = objectEditPage.addObject("TourCode.StepClass");
        obj1.setFieldValue(obj1.byPropertyName("element"), "body");
        // Test to put a translation key
        obj1.setFieldValue(obj1.byPropertyName("title"), "tour.app.name");
        // Test to use the translation macro
        obj1.setFieldValue(obj1.byPropertyName("content"), "{{translation key=\"TourCode.TourClass_description\" /}}");
        obj1.setFieldValue(obj1.byPropertyName("order"), "1");
        obj1.setCheckBox(obj1.byPropertyName("backdrop"), true);
        // I voluntary create the object 3 before the 2 to test the 'order' field
        ObjectEditPane obj3 = objectEditPage.addObject("TourCode.StepClass");
        obj3.setFieldValue(obj3.byPropertyName("element"), "body");
        obj3.setFieldValue(obj3.byPropertyName("title"), "Title 3");
        obj3.setFieldValue(obj3.byPropertyName("content"), "Step 3");
        obj3.setFieldValue(obj3.byPropertyName("order"), "3");
        obj3.setCheckBox(obj3.byPropertyName("backdrop"), true);
        ObjectEditPane obj2 = objectEditPage.addObject("TourCode.StepClass");
        obj2.setFieldValue(obj2.byPropertyName("element"), "body");
        obj2.setFieldValue(obj2.byPropertyName("title"), "Title 2");
        obj2.setFieldValue(obj2.byPropertyName("content"), "Step 2");
        obj2.setFieldValue(obj2.byPropertyName("order"), "2");
        obj2.setCheckBox(obj2.byPropertyName("backdrop"), true);
        // Object 4 used to test the Multipage feature ('targetPage' field)
        ObjectEditPane obj4 = objectEditPage.addObject("TourCode.StepClass");
        obj4.setFieldValue(obj4.byPropertyName("element"), "body");
        obj4.setFieldValue(obj4.byPropertyName("title"), "Title 4");
        obj4.setFieldValue(obj4.byPropertyName("content"), "Step 4");
        obj4.setFieldValue(obj4.byPropertyName("order"), "4");
        obj4.setCheckBox(obj4.byPropertyName("backdrop"), true);
        obj4.setFieldValue(obj4.byPropertyName("targetPage"), "TourCode.TourClass");
        objectEditPage.clickSaveAndView();
        
        tourHomePage = TourHomePage.gotoPage();
        assertTrue(tourHomePage.getTours().contains(new TourFromLivetable("Test", "Main.WebHome", true)));
    }
    
    private void cleanUp() throws Exception
    {
        TourHomePage tourHomePage = TourHomePage.gotoPage();
        ViewPage tourPage = tourHomePage.getTourPage("Test");
        tourPage.delete().clickYes();
    }
    
    @Test
    public void testTour() throws Exception
    {
        setUpTour();

        PageWithTour homePage = PageWithTour.gotoPage("Main", "WebHome");
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
        
        // Resume (to step 4)
        secondPage.resume();
        assertTrue(secondPage.isTourDisplayed());
        assertFalse(secondPage.hasResumeButton());
        assertEquals("Title 4", secondPage.getStepTitle());
        assertEquals("Step 4", secondPage.getStepDescription());
        assertFalse(secondPage.hasNextStep());
        assertTrue(secondPage.hasPreviousStep());
        assertTrue(secondPage.hasEndButton());
        
        // Close
        secondPage.close();
        assertFalse(secondPage.isTourDisplayed());
        assertTrue(secondPage.hasResumeButton());
        
        // Go to an other page and then go back
        TourHomePage.gotoPage();
        secondPage = PageWithTour.gotoPage("TourCode", "TourClass");
        assertFalse(secondPage.isTourDisplayed());
        assertTrue(secondPage.hasResumeButton());

        // Resume (to step 4)
        secondPage.resume();
        assertTrue(secondPage.isTourDisplayed());
        assertFalse(secondPage.hasResumeButton());
        assertEquals("Title 4", secondPage.getStepTitle());
        assertEquals("Step 4", secondPage.getStepDescription());
        assertFalse(secondPage.hasNextStep());
        assertTrue(secondPage.hasPreviousStep());
        assertTrue(secondPage.hasEndButton());

        // Close
        secondPage.close();
        assertFalse(secondPage.isTourDisplayed());
        assertTrue(secondPage.hasResumeButton());
        
        // Go back to the tour homepage
        TourHomePage tourHomePage = TourHomePage.gotoPage();
        // Launch the tour
        homePage = tourHomePage.startTour("Test");
        // So the step 1 is active
        assertEquals("Tour", homePage.getStepTitle());
        assertEquals("Description", homePage.getStepDescription());
        assertTrue(homePage.hasNextStep());
        assertFalse(homePage.hasPreviousStep());
        assertFalse(homePage.hasEndButton());
        
        cleanUp();
    }

}
