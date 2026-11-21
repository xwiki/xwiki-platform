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
package org.xwiki.platform.security.requiredrights.internal;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.validation.edit.EditConfirmationChecker;
import org.xwiki.platform.security.requiredrights.RequiredRightAnalyzer;
import org.xwiki.platform.security.requiredrights.RequiredRightsException;
import org.xwiki.platform.security.requiredrights.internal.configuration.RequiredRightsConfiguration;
import org.xwiki.rendering.renderer.BlockRenderer;
import org.xwiki.rendering.renderer.printer.DefaultWikiPrinter;
import org.xwiki.security.authorization.Right;
import org.xwiki.test.LogLevel;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.junit5.LogCaptureExtension;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.test.page.HTML50ComponentList;
import org.xwiki.test.page.PageTest;
import org.xwiki.test.page.XWikiSyntax21ComponentList;

import com.xpn.xwiki.doc.XWikiDocument;

import static com.xpn.xwiki.doc.XWikiDocument.CKEY_TDOC;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.xwiki.platform.security.requiredrights.internal.configuration.RequiredRightsConfiguration.RequiredRightDocumentProtection.WARNING;

/**
 * Test of {@link RequiredRightsEditConfirmationChecker} with its templates.
 *
 * @version $Id$
 */
@ComponentList({
    RequiredRightsEditConfirmationChecker.class
})
@HTML50ComponentList
@XWikiSyntax21ComponentList
class RequiredRightsEditConfirmationCheckerPageTest extends PageTest
{
    private static final DocumentReference DOCUMENT_REFERENCE = new DocumentReference("xwiki", "Space", "Page");

    @MockComponent
    private RequiredRightAnalyzer<XWikiDocument> analyzer;

    @MockComponent
    private RequiredRightsChangedFilter requiredRightsChangedFilter;

    @MockComponent
    private RequiredRightsConfiguration requiredRightsConfiguration;

    @RegisterExtension
    private LogCaptureExtension logCapture = new LogCaptureExtension(LogLevel.WARN);

    @Test
    void checkDisplaysTheCauseOfAFailedAnalysis() throws Exception
    {
        when(this.requiredRightsConfiguration.getDocumentProtection()).thenReturn(WARNING);
        XWikiDocument document = new XWikiDocument(DOCUMENT_REFERENCE);
        this.xwiki.saveDocument(document, this.context);
        this.context.put(CKEY_TDOC, this.xwiki.getDocument(DOCUMENT_REFERENCE, this.context));
        when(this.oldcore.getMockContextualAuthorizationManager().hasAccess(Right.EDIT, DOCUMENT_REFERENCE))
            .thenReturn(true);
        when(this.analyzer.analyze(any())).thenThrow(new RequiredRightsException("analysis failure", null));

        EditConfirmationChecker checker =
            this.componentManager.getInstance(EditConfirmationChecker.class, "requiredRights");
        DefaultWikiPrinter printer = new DefaultWikiPrinter();
        this.componentManager.<BlockRenderer>getInstance(BlockRenderer.class, "html/5.0")
            .render(checker.check().orElseThrow().getMessage(), printer);

        Element details = Jsoup.parse(printer.toString()).selectFirst(".xwikirenderingerrordescription pre");
        assertTrue(details.text().contains("RequiredRightsException: analysis failure"), details::text);
        assertTrue(this.logCapture.getMessage(0).contains("Root cause is [RequiredRightsException: analysis failure]"));
    }
}
