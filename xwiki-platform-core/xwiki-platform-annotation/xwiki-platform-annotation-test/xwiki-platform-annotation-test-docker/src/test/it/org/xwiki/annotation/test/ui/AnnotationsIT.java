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

import java.util.Locale;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.xwiki.annotation.test.po.AnnotatableViewPage;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.test.docker.junit5.TestReference;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;
import org.xwiki.test.ui.po.ViewPage;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @version $Id$
 * @since 11.3RC1
 */
@UITest
public class AnnotationsIT
{
    @BeforeAll
    public void setup(TestUtils setup) throws Exception
    {
        setup.loginAsSuperAdmin();
    }

    @Test
    public void addAnnotationTranslation(TestUtils setup, TestReference testReference) throws Exception
    {
        setup.setWikiPreference("multilingual", "true");
        setup.setWikiPreference("languages", "en,fr");

        LocalDocumentReference referenceFR = new LocalDocumentReference(testReference, Locale.FRENCH);
        LocalDocumentReference referenceEN = new LocalDocumentReference(testReference, Locale.ENGLISH);

        setup.createPage(referenceEN, "Some content in english.", "An english page");
        setup.createPage(referenceFR, "Un peu de contenu en français.", "Une page en français");

        AnnotatableViewPage viewPage = new AnnotatableViewPage(setup.gotoPage(referenceEN));
        viewPage.addAnnotation("Some", "English word.");

        viewPage = new AnnotatableViewPage(setup.gotoPage(referenceFR));
        // We cannot wait for success since the UI is in french...
        viewPage.addAnnotation("peu", "Un mot français", false);

        viewPage = new AnnotatableViewPage(setup.gotoPage(referenceEN));
        viewPage.showAnnotationsPane();
        viewPage.clickShowAnnotations();
        assertEquals("English word.", viewPage.getAnnotationContentByText("Some"));

        viewPage = new AnnotatableViewPage(setup.gotoPage(referenceFR));
        viewPage.showAnnotationsPane();
        viewPage.clickShowAnnotations();
        assertEquals("Un mot français", viewPage.getAnnotationContentByText("peu"));
    }
}
