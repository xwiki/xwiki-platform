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
package org.xwiki.officeimporter.internal;

import java.util.Map;

import org.xwiki.officeimporter.OfficeImporter;
import org.xwiki.officeimporter.OfficeImporterException;

/**
 * A bridge between velocity and office importer.
 * 
 * @version $Id$
 * @since 1.8M1
 */
public class OfficeImporterVelocityBridge
{
    /**
     * Internal {@link OfficeImporter} instance.
     */
    private OfficeImporter importer;

    /**
     * Holds any error messages thrown during the import operation.
     */
    private String message;
    
    /**
     * Default constructor.
     * 
     * @param importer internal {@link OfficeImporter} component.
     */
    public OfficeImporterVelocityBridge(OfficeImporter importer)
    {
        this.importer = importer;
    }

    /**
     * Imports the passed Office document into the target wiki page.
     * 
     * @param fileContent the binary content of the input document
     * @param fileName the name of the source document name (should have a valid extension since the extension is 
     *        used to find out the office document's format)
     * @param targetDocument the name of the resulting wiki page
     * @param options the optional parameters for the conversion
     * @return true if the operation was a success.
     */
    public boolean importDocument(byte[] fileContent, String fileName, String targetDocument,
        Map<String, String> options)
    {
        boolean success = false;
        try {
            importer.importDocument(fileContent, fileName, targetDocument, options);
            success = true;
        } catch(OfficeImporterException ex) {
            this.message = ex.getMessage();
        }
        return success;
    }
    
    /**
     * @return any error messages thrown while importing or null.
     */
    public String getMessage() {
        return this.message;
    }
}
