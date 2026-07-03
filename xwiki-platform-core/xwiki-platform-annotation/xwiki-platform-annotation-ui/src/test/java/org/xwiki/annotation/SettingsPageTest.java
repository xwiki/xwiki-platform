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
package org.xwiki.annotation;

import java.util.List;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.annotation.script.AnnotationScriptService;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.script.service.ScriptService;
import org.xwiki.test.page.HTML50ComponentList;
import org.xwiki.test.page.PageTest;
import org.xwiki.test.page.XWikiSyntax20ComponentList;

import com.xpn.xwiki.doc.XWikiDocument;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Test of {@code AnnotationCode.Settings}.
 *
 * @version $Id$
 */
@XWikiSyntax20ComponentList
@HTML50ComponentList
class SettingsPageTest extends PageTest
{
    private static final DocumentReference SETTINGS =
        new DocumentReference("xwiki", "AnnotationCode", "Settings");

    private static final DocumentReference ANNOTATION_CONFIG =
        new DocumentReference("xwiki", "AnnotationCode", "AnnotationConfig");

    private static final DocumentReference COMMENTS_CLASS =
        new DocumentReference("xwiki", "XWiki", "XWikiComments");

    private static final DocumentReference TARGET = new DocumentReference("xwiki", "Space", "Target");

    private AnnotationScriptService annotationScriptService;

    @BeforeEach
    void setUp() throws Exception
    {
        // The annotation class referenced by the configuration. Only its "author" field is used, to render the label
        // of the filter criterion.
        XWikiDocument commentsClass = this.xwiki.getDocument(COMMENTS_CLASS, this.context);
        commentsClass.getXClass().addTextField("author", "Author", 30);
        this.xwiki.saveDocument(commentsClass, this.context);

        // The document holding the annotations.
        XWikiDocument target = this.xwiki.getDocument(TARGET, this.context);
        target.setSyntax(Syntax.XWIKI_2_1);
        this.xwiki.saveDocument(target, this.context);

        // The configuration provides the annotation class reference (XWiki.XWikiComments).
        loadPage(ANNOTATION_CONFIG);
        loadPage(SETTINGS);

        // The annotations displayed in the filters are provided by the annotation service.
        this.annotationScriptService = mock(AnnotationScriptService.class);
        this.componentManager.registerComponent(ScriptService.class, "annotations", this.annotationScriptService);

        this.stubRequest.put("target", "Space.Target");
    }

    @Test
    void authorIsEscapedInFilterCheckbox() throws Exception
    {
        String malformedAuthor = "\"><script>console.log(1)</script>";
        Annotation annotation = new Annotation("0");
        annotation.setAuthor(malformedAuthor);
        when(this.annotationScriptService.getAnnotations(any())).thenReturn(List.of(annotation));

        Document document = renderHTMLPage(SETTINGS);

        // The author is used as the value of the filtering checkbox. It must be escaped so that it stays inside the
        // attribute instead of breaking out of it.
        Element checkbox = document.selectFirst(".criterion-option-list input[type=checkbox]");
        assertNotNull(checkbox);
        assertEquals(malformedAuthor, checkbox.attr("value"));
        // The payload must not have been able to inject an executable script element.
        assertTrue(document.select("script").stream().noneMatch(script -> script.data().contains("console.log")),
            "The author payload was rendered as an executable script element");
    }
}
