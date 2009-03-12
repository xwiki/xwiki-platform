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

import java.io.ByteArrayInputStream;
import java.util.Map;

import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.logging.Logger;
import org.xwiki.context.Execution;
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
     * The key used to place any error messages while importing office documents.
     */
    public static final String OFFICE_IMPORTER_ERROR = "OFFICE_IMPORTER_ERROR";

    /**
     * The {@link Execution} component.
     */
    private Execution execution;

    /**
     * Internal {@link OfficeImporter} component.
     */
    private OfficeImporter importer;

    /**
     * The {@link DocumentAccessBridge} component.
     */
    private DocumentAccessBridge docBridge;

    /**
     * The {@link Logger} instance.
     */
    private Logger logger;

    /**
     * Default constructor.
     * 
     * @param importer internal {@link OfficeImporter} component.
     */
    public OfficeImporterVelocityBridge(Execution execution, OfficeImporter importer, DocumentAccessBridge docBridge,
        Logger logger)
    {
        this.execution = execution;
        this.importer = importer;
        this.docBridge = docBridge;
        this.logger = logger;
    }

    /**
     * Imports the passed Office document into the target wiki page.
     * 
     * @param fileContent the binary content of the input document.
     * @param fileName the name of the source document (should have a valid extension since the extension is used to
     *            find out the office document's format).
     * @param targetDocument the name of the resulting wiki page.
     * @param options the optional parameters for the conversion.
     * @return true if the operation was a success.
     */
    public boolean importDocument(byte[] fileContent, String fileName, String targetDocument,
        Map<String, String> options)
    {
        boolean success = false;
        try {
            validateRequest(targetDocument);
            importer.importStream(new ByteArrayInputStream(fileContent), fileName, targetDocument, options);
            success = true;
        } catch (OfficeImporterException ex) {
            logger.error(ex.getMessage(), ex);
            execution.getContext().setProperty(OFFICE_IMPORTER_ERROR, ex.getMessage());
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
            execution.getContext().setProperty(OFFICE_IMPORTER_ERROR,
                "Internal error while finalizing the target document.");
        }
        return success;
    }

    /**
     * Checks if this request is valid. For a request to be valid, the requested target document should not exist and
     * the user should have enough privileges to create & edit that particular page.
     * 
     * @param targetDocument the target document.
     * @throws OfficeImporterException if the request is invalid.
     */
    private void validateRequest(String targetDocument) throws OfficeImporterException
    {
        boolean exists = true;
        try {
            exists = docBridge.exists(targetDocument);
        } catch (Exception ex) {
            throw new OfficeImporterException("Internal error.", ex);
        }
        if (exists) {
            throw new OfficeImporterException("The target document " + targetDocument + " already exists.");
        } else if (!docBridge.isDocumentEditable(targetDocument)) {
            throw new OfficeImporterException("Inadequate privileges.");
        }
    }

    /**
     * @return any error messages thrown while importing.
     */
    public String getLastErrorMessage()
    {
        Object error = execution.getContext().getProperty(OFFICE_IMPORTER_ERROR);
        return (error != null) ? (String) error : null;
    }
}
