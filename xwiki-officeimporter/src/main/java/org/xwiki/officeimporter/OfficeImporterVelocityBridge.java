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

import java.io.ByteArrayInputStream;
import java.util.Map;

import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.logging.Logger;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.context.Execution;
import org.xwiki.model.DocumentNameFactory;

/**
 * A bridge between velocity and office importer.
 * 
 * @version $Id: OfficeImporterVelocityBridge.java 24508 2009-10-15 10:05:22Z asiri $
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
     * Used for converting string document names to {@link DocumentName} instances.
     */
    private DocumentNameFactory nameFactory;

    /**
     * The {@link Logger} instance.
     */
    private Logger logger;

    /**
     * Default constructor.
     * 
     * @param componentManager used to lookup for other necessary components.
     * @param logger logger.
     * @throws OfficeImporterException if an error occurs while looking up for other required components.
     */
    public OfficeImporterVelocityBridge(ComponentManager componentManager, Logger logger)
        throws OfficeImporterException
    {
        this.logger = logger;
        try {
            this.execution = componentManager.lookup(Execution.class);
            this.importer = componentManager.lookup(OfficeImporter.class);
            this.docBridge = componentManager.lookup(DocumentAccessBridge.class);
            this.nameFactory = componentManager.lookup(DocumentNameFactory.class);
        } catch (Exception ex) {
            throw new OfficeImporterException("Error while initializing office importer velocity bridge.", ex);
        }
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
            validateRequest(targetDocument, options);
            importer.importStream(new ByteArrayInputStream(fileContent), fileName, targetDocument, options);
            success = true;
        } catch (OfficeImporterException ex) {
            logger.error(ex.getMessage(), ex);
            execution.getContext().setProperty(OFFICE_IMPORTER_ERROR, ex.getMessage());
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
            setErrorMessage("Internal error while finalizing the target document.");
        }
        return success;
    }

    /**
     * @return an error message set inside current execution (during import process) or null.
     */
    public String getErrorMessage()
    {
        return (String) execution.getContext().getProperty(OFFICE_IMPORTER_ERROR);
    }

    /**
     * Utility method for setting an error message inside current execution.
     * 
     * @param message error message.
     */
    private void setErrorMessage(String message)
    {
        execution.getContext().setProperty(OFFICE_IMPORTER_ERROR, message);
    }

    /**
     * @return any error messages thrown while importing.
     * @deprecated use {@link #getErrorMessage()} instead since 2.2M1.
     */
    @Deprecated
    public String getLastErrorMessage()
    {
        return getErrorMessage();
    }

    /**
     * Checks if this request is valid. For a request to be valid, the target document should be editable by the current
     * user. And if this is not an append request, the target document should not exist.
     * 
     * @param targetDocument the target document.
     * @param options additional parameters passed in for the import operation.
     * @throws OfficeImporterException if the request is invalid.
     */
    private void validateRequest(String targetDocument, Map<String, String> options) throws OfficeImporterException
    {
        if (!docBridge.isDocumentEditable(nameFactory.createDocumentName(targetDocument))) {
            throw new OfficeImporterException("Inadequate privileges.");
        } else if (docBridge.exists(targetDocument) && !isAppendRequest(options)) {
            throw new OfficeImporterException("The target document " + targetDocument + " already exists.");
        }
    }

    /**
     * Utility method for checking if a request is made to append the importer result to an existing page.
     * 
     * @param options additional parameters passed in for the import operation.
     * @return true if the params indicate that this is an append request.
     */
    private boolean isAppendRequest(Map<String, String> options)
    {
        String appendParam = options.get("appendContent");
        return (appendParam != null) ? appendParam.equals("true") : false;
    }
}
