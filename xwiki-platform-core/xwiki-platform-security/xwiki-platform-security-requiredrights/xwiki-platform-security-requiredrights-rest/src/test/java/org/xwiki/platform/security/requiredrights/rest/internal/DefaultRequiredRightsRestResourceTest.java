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
package org.xwiki.platform.security.requiredrights.rest.internal;

import java.util.List;
import java.util.Optional;

import jakarta.inject.Named;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.internal.document.DocumentRequiredRightsReader;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.platform.security.requiredrights.RequiredRightAnalysisResult;
import org.xwiki.platform.security.requiredrights.RequiredRightAnalyzer;
import org.xwiki.platform.security.requiredrights.RequiredRightsException;
import org.xwiki.rest.XWikiRestException;
import org.xwiki.security.authorization.Right;
import org.xwiki.security.authorization.requiredrights.DocumentRequiredRightsManager;
import org.xwiki.security.requiredrights.rest.model.jaxb.DocumentRequiredRights;
import org.xwiki.security.requiredrights.rest.model.jaxb.DocumentRightsAnalysisResult;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.test.MockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.InjectMockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.OldcoreTest;
import com.xpn.xwiki.test.reference.ReferenceComponentList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link DefaultRequiredRightsRestResource}.
 *
 * @version $Id$
 */
@OldcoreTest
@ReferenceComponentList
@ComponentList(DocumentRequiredRightsReader.class)
class DefaultRequiredRightsRestResourceTest
{
    private static final DocumentReference DOCUMENT_REFERENCE =
        new DocumentReference("wiki", "space", "page");

    @InjectMockitoOldcore
    private MockitoOldcore oldcore;

    @InjectMockComponents
    private DefaultRequiredRightsRestResource restResource;

    @MockComponent
    @Named("withTranslations")
    private RequiredRightAnalyzer<DocumentReference> requiredRightAnalyzer;

    @MockComponent
    private RequiredRightsObjectConverter objectConverter;

    @MockComponent
    private DocumentRequiredRightsUpdater updater;

    @BeforeEach
    void setUp() throws Exception
    {
        XWikiDocument document = this.oldcore.getSpyXWiki().getDocument(DOCUMENT_REFERENCE,
            this.oldcore.getXWikiContext());
        document.setTitle("Test");
        this.oldcore.getSpyXWiki().saveDocument(document, "Test setup", this.oldcore.getXWikiContext());

        when(this.oldcore.getMockRightService().hasAccessLevel(any(), any(), any(), any())).thenReturn(true);
    }

    @Test
    void analyzeReturnsCorrectResult() throws Exception
    {
        List<RequiredRightAnalysisResult> analysisResults =
            List.of(new RequiredRightAnalysisResult(DOCUMENT_REFERENCE, mock(), mock(), List.of()));
        DocumentRightsAnalysisResult expectedResult = mock();

        when(this.requiredRightAnalyzer.analyze(DOCUMENT_REFERENCE)).thenReturn(analysisResults);
        when(this.objectConverter.toDocumentRightsAnalysisResult(any(), eq(analysisResults), eq(DOCUMENT_REFERENCE)))
            .thenReturn(expectedResult);

        DocumentRightsAnalysisResult result = this.restResource.analyze("space", "page", "wiki");

        assertEquals(expectedResult, result);
    }

    @Test
    void analyzeThrowsXWikiRestExceptionOnRequiredRightsException() throws RequiredRightsException
    {
        RequiredRightsException expectedException = new RequiredRightsException("Test", new RuntimeException());
        when(this.requiredRightAnalyzer.analyze(DOCUMENT_REFERENCE)).thenThrow(expectedException);

        assertThrows(XWikiRestException.class, () -> this.restResource.analyze("space", "page", "wiki"));
    }

    @Test
    void updateRequiredRightsSucceeds() throws Exception
    {
        org.xwiki.security.authorization.requiredrights.DocumentRequiredRights updatedRequiredrights = mock();
        DocumentRequiredRightsManager requiredRightsManager =
            this.oldcore.getMocker().getInstance(DocumentRequiredRightsManager.class);
        when(requiredRightsManager.getRequiredRights(DOCUMENT_REFERENCE))
            .thenReturn(Optional.of(updatedRequiredrights));
        DocumentRequiredRights expectedResponse = mock();
        when(this.objectConverter.convertDocumentRequiredRights(updatedRequiredrights)).thenReturn(expectedResponse);

        when(this.oldcore.getMockContextualAuthorizationManager().hasAccess(Right.EDIT, DOCUMENT_REFERENCE))
            .thenReturn(true);

        DocumentRequiredRights inputRights = new DocumentRequiredRights().withEnforce(true);

        assertEquals(expectedResponse, this.restResource.updateRequiredRights("space", "page", "wiki", inputRights));

        verify(this.updater).updateRequiredRights(eq(inputRights),
            argThat(doc -> doc.getDocumentReference().equals(DOCUMENT_REFERENCE)));
    }
}
