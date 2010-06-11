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
package org.xwiki.officeimporter.internal.openoffice;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.artofsolving.jodconverter.document.DefaultDocumentFormatRegistry;
import org.artofsolving.jodconverter.document.DocumentFormat;
import org.artofsolving.jodconverter.document.DocumentFormatRegistry;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.component.logging.AbstractLogEnabled;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.container.Container;
import org.xwiki.officeimporter.OfficeImporterException;
import org.xwiki.officeimporter.internal.OfficeImporterFileStorage;
import org.xwiki.officeimporter.openoffice.OpenOfficeDocumentConverter;
import org.xwiki.officeimporter.openoffice.OpenOfficeManager;

/**
 * Default implementation of {@link OpenOfficeDocumentConverter}.
 * 
 * @version $Id$
 * @since 1.8RC3
 * @deprecated use {@link OpenOfficeManager#getConverter()} instead since 2.2M1
 */
@Component
@Deprecated
public class DefaultOpenOfficeDocumentConverter extends AbstractLogEnabled implements OpenOfficeDocumentConverter,
    Initializable
{
    /**
     * Error message used to signal a missing openoffice server.
     */
    private static final String ERROR_SERVER_NOT_FOUND = "OpenOffice server not found.";
    
    /**
     * Error message used to signal that there is a problem writing to temporary files.
     */
    private static final String ERROR_WRITING_TEMP_FILES = "Error while writing temporary files.";
    
    /**
     * Error message used to signal that there is a problem reading an OOo generated artifact file.
     */
    private static final String ERROR_READING_ARTIFACT = "Error while reading artifact : %s";
    
    /**
     * The {@link OpenOfficeManager} component.
     */
    @Requirement
    private OpenOfficeManager ooManager;

    /**
     * Used for querying current user information.
     */
    @Requirement
    private DocumentAccessBridge docBridge;

    /**
     * Used for querying temporary directory information.
     */
    @Requirement
    private Container container;

    /**
     * Output format of this transformer.
     */
    private DocumentFormat htmlFormat;

    /**
     * {@inheritDoc}
     */
    public void initialize() throws InitializationException
    {
        DocumentFormatRegistry formatRegistry = new DefaultDocumentFormatRegistry();
        htmlFormat = formatRegistry.getFormatByExtension("html");
    }

    /**
     * {@inheritDoc}
     */
    @Deprecated
    public Map<String, InputStream> convert(InputStream in, OfficeImporterFileStorage storage)
        throws OfficeImporterException
    {
        // Make sure the openoffice server is connected.
        if (ooManager.getState() != OpenOfficeManager.ManagerState.CONNECTED) {
            throw new OfficeImporterException(ERROR_SERVER_NOT_FOUND);
        }
        
        Map<String, InputStream> result = new HashMap<String, InputStream>();
        
        // Copy bytes from the input stream into temporary input file.
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(storage.getInputFile());
            IOUtils.copy(in, fos);
        } catch (IOException ex) {
            throw new OfficeImporterException(ERROR_WRITING_TEMP_FILES, ex);
        } finally {
            IOUtils.closeQuietly(fos);
        }
        
        // Make the conversion.
        ooManager.getDocumentConverter().convert(storage.getInputFile(), storage.getOutputFile(), htmlFormat);
        
        // Collect the resulting artifact streams
        File[] artifacts = storage.getOutputDir().listFiles();
        for (File artifact : artifacts) {
            FileInputStream fis = null;
            try {
                fis = new FileInputStream(artifact);
                result.put(artifact.getName(), fis);
            } catch (IOException ex) {
                getLogger().error(String.format(ERROR_READING_ARTIFACT, artifact.getName()), ex);
            }
        }
        
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Deprecated
    public Map<String, byte[]> convert(byte[] officeFileData) throws OfficeImporterException
    {
        // Make sure the openoffice server is connected.
        if (ooManager.getState() != OpenOfficeManager.ManagerState.CONNECTED) {
            throw new OfficeImporterException(ERROR_SERVER_NOT_FOUND);
        }

        Map<String, byte[]> result = new HashMap<String, byte[]>();

        // Create temporary storage.
        File tempDir = container.getApplicationContext().getTemporaryDirectory();
        OfficeImporterFileStorage storage = new OfficeImporterFileStorage(tempDir, docBridge.getCurrentUser());

        // Create the temporary input file.
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(storage.getInputFile());
            fos.write(officeFileData);            
        } catch (IOException ex) {
            storage.cleanUp();
            throw new OfficeImporterException(ERROR_WRITING_TEMP_FILES, ex);
        } finally {
            IOUtils.closeQuietly(fos);
        }

        // Make the conversion. Ideally jodconverter should have a checked exception it it's convert() method because
        // it could throw an exception (it does). As a workaround we have to catch a generic Exception instance.
        try {
            ooManager.getDocumentConverter().convert(storage.getInputFile(), storage.getOutputFile(), htmlFormat);
        } catch (Exception ex) {
            storage.cleanUp();
            throw new OfficeImporterException("Error while performing conversion.", ex);
        }

        // Collect the resulting artifacts
        File[] artifacts = storage.getOutputDir().listFiles();
        for (File artifact : artifacts) {
            FileInputStream fis = null;                       
            try {
                fis = new FileInputStream(artifact);                
                result.put(artifact.getName(), IOUtils.toByteArray(fis));                
            } catch (IOException ex) {
                getLogger().error(String.format(ERROR_READING_ARTIFACT, artifact.getName()), ex);
            } finally {                
                IOUtils.closeQuietly(fis);
            }            
        }

        // Cleanup the storage.
        storage.cleanUp();

        return result;
    }
}
