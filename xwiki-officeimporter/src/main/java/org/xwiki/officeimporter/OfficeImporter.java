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
package org.xwiki.officeimporter;

import java.util.Map;

/**
 * Entry point to import Office documents into wiki pages.
 * 
 * @version $Id$
 * @since 1.8M1
 */
public interface OfficeImporter
{
    /**
     * This component's role, used when code needs to look it up.
     */
    String ROLE = OfficeImporter.class.getName();

    /**
     * Imports the passed Office document into the target wiki page.
     * 
     * @param fileContent the binary content of the input document
     * @param fileName the name of the source document name (should have a valid extension since the extension is used
     *            to find out the office document's format)
     * @param targetDocument the name of the resulting wiki page
     * @param options the optional parameters for the conversion
     * @throws OfficeImporterException if an error occurred during the import
     */
    void importDocument(byte[] fileContent, String fileName, String targetDocument, Map<String, String> options)
        throws OfficeImporterException;

    /**
     * Imports an office document attached to a wiki page into xhtml. This method only returns the resulting xhtml, it
     * does not modify the original content of targetDocument. Although, if there are non-textual content present in the
     * office document, they will be attached to targetDocument.
     * 
     * @param targetDocument the name of the target wiki page.
     * @param attachmentName name of the attached office document.
     * @param options additional options for the import operation.
     * @throws OfficeImporterException if an error occurs during the import.
     */
    String importDocument(String targetDocument, String attachmentName, Map<String, String> options)
        throws OfficeImporterException;
}
