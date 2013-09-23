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
package org.xwiki.officeimporter.builder;

import java.io.InputStream;

import org.xwiki.component.annotation.Role;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.officeimporter.OfficeImporterException;
import org.xwiki.officeimporter.document.XDOMOfficeDocument;

/**
 * Component responsible for building {@link XDOMOfficeDocument} objects from binary (presentation) office files. This
 * component builds {@link XDOMOfficeDocument} objects which when rendered produces a slid-show like feeling on the wiki
 * page.
 * 
 * @version $Id$
 * @since 2.1M1
 */
@Role
public interface PresentationBuilder
{
    /**
     * Builds a (slide-show) {@link XDOMOfficeDocument} corresponding to the given office presentation.
     * 
     * @param officeFileStream {@link InputStream} corresponding to the office presentation.
     * @param officeFileName name of the office document (used to determine input document format).
     * @param reference reference document w.r.t which the presentation is built.
     * @return {@link XDOMOfficeDocument} corresponding to the given office presentation.
     * @throws OfficeImporterException if an error occurs while performing the import operation.
     */
    XDOMOfficeDocument build(InputStream officeFileStream, String officeFileName, DocumentReference reference)
        throws OfficeImporterException;
}
