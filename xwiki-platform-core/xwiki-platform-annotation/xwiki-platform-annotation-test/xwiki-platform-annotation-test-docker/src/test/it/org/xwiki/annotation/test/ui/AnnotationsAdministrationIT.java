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
package org.xwiki.annotation.test.ui;

import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.xwiki.administration.test.po.AdministrationSectionPage;
import org.xwiki.administration.test.po.PresentationAdministrationSectionPage;
import org.xwiki.annotation.test.po.AnnotatableViewPage;
import org.xwiki.test.docker.junit5.TestReference;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;
import org.xwiki.test.ui.po.ViewPage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Validate the Annotation-related administration.
 *
 * @version $Id$
 */
@UITest
class AnnotationsAdministrationIT
{
    @Test
    @Order(1)
    void hideAnnotationsTab(TestUtils setup, TestReference testReference)
    {
        setup.loginAsSuperAdmin();

        ViewPage viewPage = setup.createPage(testReference, "");
        AnnotatableViewPage annotatableViewPage = new AnnotatableViewPage(viewPage);
        // Check that the annotations tab is displayed by default.
        assertTrue(annotatableViewPage.isAnnotationsDocExtraPaneAvailable());

        AdministrationSectionPage.gotoPage("Presentation");
        PresentationAdministrationSectionPage presentationSectionPage = new PresentationAdministrationSectionPage();
        // Note that here, the default value is "default" but in the settings that
        // are distributed with xwiki-platform-distribution-ui-base, it is in fact set to "No".
        assertEquals(PresentationAdministrationSectionPage.ShowTabValue.DEFAULT,
            presentationSectionPage.getShowAnnotations());

        presentationSectionPage.setShowAnnotations(PresentationAdministrationSectionPage.ShowTabValue.NO);
        presentationSectionPage.clickSave();

        assertEquals(PresentationAdministrationSectionPage.ShowTabValue.NO,
            presentationSectionPage.getShowAnnotations());

        // Check that the annotations tab is not displayed anymore.
        viewPage = setup.gotoPage(testReference);
        annotatableViewPage = new AnnotatableViewPage(viewPage);
        assertFalse(annotatableViewPage.isAnnotationsDocExtraPaneAvailable());
    }

    @Test
    @Order(2)
    void showAnnotationsTab(TestUtils setup, TestReference testReference)
    {
        ViewPage viewPage = setup.createPage(testReference, "");
        AnnotatableViewPage annotatableViewPage = new AnnotatableViewPage(viewPage);
        // Check that the annotations tab is not displayed.
        assertFalse(annotatableViewPage.isAnnotationsDocExtraPaneAvailable());

        AdministrationSectionPage.gotoPage("Presentation");
        PresentationAdministrationSectionPage presentationSectionPage = new PresentationAdministrationSectionPage();
        assertEquals(PresentationAdministrationSectionPage.ShowTabValue.NO,
            presentationSectionPage.getShowAnnotations());
        presentationSectionPage.setShowAnnotations(PresentationAdministrationSectionPage.ShowTabValue.YES);
        presentationSectionPage.clickSave();

        assertEquals(PresentationAdministrationSectionPage.ShowTabValue.YES,
            presentationSectionPage.getShowAnnotations());

        viewPage = setup.gotoPage(testReference);
        annotatableViewPage = new AnnotatableViewPage(viewPage);
        // Check that the annotations tab is displayed again.
        assertTrue(annotatableViewPage.isAnnotationsDocExtraPaneAvailable());
    }
}
