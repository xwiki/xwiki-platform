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
package org.xwiki.export.pdf.internal.script;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.xwiki.export.pdf.job.PDFExportJobRequest;
import org.xwiki.export.pdf.job.PDFExportJobStatus;
import org.xwiki.export.pdf.job.PDFExportJobStatus.DocumentRenderingResult;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.user.CurrentUserReference;
import org.xwiki.user.UserReference;
import org.xwiki.user.UserReferenceResolver;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Test of {@link SafePDFExportJobStatus}.
 *
 * @version $Id$
 * @since 14.7RC1
 */
@ComponentTest
class SafePDFExportJobStatusTest
{
    @Mock
    private UserReferenceResolver<CurrentUserReference> currentUserResolver;

    @Mock
    private UserReferenceResolver<DocumentReference> userResolver;

    @Mock
    private PDFExportJobStatus pdfExportJobStatus;

    @Mock
    private PDFExportJobRequest pdfExportJobRequest;

    @Mock
    private UserReference userReferenceA;

    @Mock
    private UserReference userReferenceB;

    @Mock
    private DocumentReference userDocumentReference;

    @Test
    void getDocumentRenderingResultsDifferentUsers()
    {
        when(this.pdfExportJobStatus.getRequest()).thenReturn(this.pdfExportJobRequest);
        when(this.pdfExportJobRequest.getUserReference()).thenReturn(this.userDocumentReference);
        when(this.currentUserResolver.resolve(CurrentUserReference.INSTANCE)).thenReturn(this.userReferenceA);
        when(this.userResolver.resolve(this.userDocumentReference)).thenReturn(this.userReferenceB);

        SafePDFExportJobStatus safePDFExportJobStatus =
            new SafePDFExportJobStatus(this.pdfExportJobStatus, null, this.userResolver, this.currentUserResolver);

        List<DocumentRenderingResult> documentRenderingResults = safePDFExportJobStatus.getDocumentRenderingResults();

        assertEquals(List.of(), documentRenderingResults);
    }

    @Test
    void getDocumentRenderingResultsSameUser()
    {
        DocumentRenderingResult documentRenderingResult = mock(DocumentRenderingResult.class);

        when(this.pdfExportJobStatus.getRequest()).thenReturn(this.pdfExportJobRequest);
        when(this.pdfExportJobStatus.getDocumentRenderingResults()).thenReturn(List.of(documentRenderingResult));
        when(this.pdfExportJobRequest.getUserReference()).thenReturn(this.userDocumentReference);
        when(this.currentUserResolver.resolve(CurrentUserReference.INSTANCE)).thenReturn(this.userReferenceA);
        when(this.userResolver.resolve(this.userDocumentReference)).thenReturn(this.userReferenceA);

        SafePDFExportJobStatus safePDFExportJobStatus =
            new SafePDFExportJobStatus(this.pdfExportJobStatus, null, this.userResolver, this.currentUserResolver);
        List<DocumentRenderingResult> documentRenderingResults = safePDFExportJobStatus.getDocumentRenderingResults();
        assertEquals(List.of(documentRenderingResult), documentRenderingResults);
    }
}
