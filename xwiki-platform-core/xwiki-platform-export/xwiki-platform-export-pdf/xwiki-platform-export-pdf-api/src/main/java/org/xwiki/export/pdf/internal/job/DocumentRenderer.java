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
package org.xwiki.export.pdf.internal.job;

import org.xwiki.component.annotation.Role;
import org.xwiki.export.pdf.job.PDFExportJobStatus.DocumentRenderingResult;
import org.xwiki.model.reference.DocumentReference;

/**
 * Component used to render documents.
 * 
 * @version $Id$
 * @since 14.10.14
 * @since 15.5
 */
@Role
public interface DocumentRenderer
{
    /**
     * The parameter used to mark header blocks that correspond to document titles. In other words, this marks the
     * beginning of a new document when multiple documents are exported.
     */
    String PARAMETER_DOCUMENT_REFERENCE = "data-xwiki-document-reference";

    /**
     * Renders the specified document.
     * 
     * @param documentReference the document to render
     * @param parameters the rendering parameters
     * @return the rendering result
     * @throws Exception if rendering the specified document fails
     */
    DocumentRenderingResult render(DocumentReference documentReference, DocumentRendererParameters parameters)
        throws Exception;
}
