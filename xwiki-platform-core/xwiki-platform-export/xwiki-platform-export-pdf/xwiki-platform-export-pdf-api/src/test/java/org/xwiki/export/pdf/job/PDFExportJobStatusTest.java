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
package org.xwiki.export.pdf.job;

import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.xwiki.logging.LoggerManager;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.observation.ObservationManager;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link PDFExportJobStatus}.
 * 
 * @version $Id$
 */
@ComponentTest
class PDFExportJobStatusTest
{
    @MockComponent
    private ObservationManager observationManager;

    @MockComponent
    private LoggerManager loggerManager;

    @Test
    void newPDFExportJobStatus()
    {
        PDFExportJobRequest request = new PDFExportJobRequest();
        PDFExportJobStatus status =
            new PDFExportJobStatus("export/pdf", request, this.observationManager, this.loggerManager);

        assertTrue(status.isCancelable());
        assertNull(status.getPDFFileReference());

        request.setDocuments(Collections.singletonList(new DocumentReference("test", "Some", "Page")));
        status = new PDFExportJobStatus("export/pdf", request, this.observationManager, this.loggerManager);

        assertNull(status.getPDFFileReference());

        request.setServerSide(true);
        request.setFileName("test.pdf");
        status = new PDFExportJobStatus("export/pdf", request, this.observationManager, this.loggerManager);

        assertEquals("test.pdf", status.getPDFFileReference().getParameterValue("fileName"));

        request.setDocuments(Collections.emptyList());
        status = new PDFExportJobStatus("export/pdf", request, this.observationManager, this.loggerManager);

        assertNull(status.getPDFFileReference());
    }
}
