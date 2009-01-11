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
package org.xwiki.officeimporter.internal.transformer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.ConnectException;

import org.xwiki.component.logging.AbstractLogEnabled;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.officeimporter.OfficeImporterContext;
import org.xwiki.officeimporter.OfficeImporterException;
import org.xwiki.officeimporter.transformer.DocumentTransformer;

import com.artofsolving.jodconverter.DefaultDocumentFormatRegistry;
import com.artofsolving.jodconverter.DocumentConverter;
import com.artofsolving.jodconverter.DocumentFormat;
import com.artofsolving.jodconverter.DocumentFormatRegistry;
import com.artofsolving.jodconverter.openoffice.connection.OpenOfficeConnection;
import com.artofsolving.jodconverter.openoffice.connection.SocketOpenOfficeConnection;
import com.artofsolving.jodconverter.openoffice.converter.OpenOfficeDocumentConverter;

/**
 * Transforms an Office Document into a corresponding Html document.
 * 
 * @version $Id$
 * @since 1.8M1
 */
public class OfficeToHtmlTransformer extends AbstractLogEnabled implements DocumentTransformer, Initializable
{
    /**
     * The host address of the Open Office server.
     */
    private String openOfficeServerIp;

    /**
     * The port number of the the Open Office service
     */
    private int openOfficeServerPort;

    /**
     * The connection to the Open Office server.
     */
    private OpenOfficeConnection openOfficeServerConnection;

    /**
     * The document converter capable of transforming office documents into html.
     */
    private DocumentConverter openOfficeDocumentConverter;

    /**
     * Output format of this transformer.
     */
    private DocumentFormat htmlFormat;

    /**
     * Temporary working directory.
     */
    private File tempDir;

    /**
     * Storage for input document.
     */
    private File inputFile = null;

    /**
     * Output directory (inside tempDir). All the by-products (artifacts) + outputFile will land here.
     */
    private File outputDir = null;

    /**
     * Output file (html).
     */
    private File outputFile = null;

    /**
     * {@inheritDoc}
     */
    public void initialize() throws InitializationException
    {
        openOfficeServerConnection = new SocketOpenOfficeConnection(openOfficeServerIp, openOfficeServerPort);
        DocumentFormatRegistry formatRegistry = new DefaultDocumentFormatRegistry();
        htmlFormat = formatRegistry.getFormatByFileExtension("html");
    }

    /**
     * {@inheritDoc}
     */
    public void transform(OfficeImporterContext importerContext) throws OfficeImporterException
    {
        // Make a connection with the Open Office server.
        try {
            openOfficeServerConnection.connect();
        } catch (ConnectException ex) {
            String message =
                "Could not connect to OpenOffice server at " + openOfficeServerIp + ":" + openOfficeServerPort;
            getLogger().error(message, ex);
            throw new OfficeImporterException(message, ex);
        }
        // Create an instance of the converter.
        openOfficeDocumentConverter = new OpenOfficeDocumentConverter(openOfficeServerConnection);
        // Prepare the temporary directory structure.
        prepareTempStorage("xwiki-office-importer-" + importerContext.getCurrentUser());
        // Fill in the input file.
        try {
            FileOutputStream fos = new FileOutputStream(inputFile);
            fos.write(importerContext.getSourceData());
            fos.close();
        } catch (IOException ex) {
            String message = "Internal error while creating temporary files.";
            getLogger().error(message, ex);
            cleanTempStorage();
            throw new OfficeImporterException(message, ex);
        }
        // Make the conversion.
        openOfficeDocumentConverter.convert(inputFile, importerContext.getSourceFormat(), outputFile, htmlFormat);
        openOfficeServerConnection.disconnect();
        // Collect the output into context.
        // First the html output.
        try {
            FileInputStream fis = new FileInputStream(outputFile);
            byte[] content = new byte[(int) outputFile.length()];
            fis.read(content);
            fis.close();
            importerContext.setTargetDocumentContent(new String(content));
        } catch (IOException ex) {
            String message = "Internal error while reading temporary files.";
            getLogger().error(message, ex);
            cleanTempStorage();
            throw new OfficeImporterException(message, ex);
        }
        // Start collecting the artifacts.
        File[] artifacts = outputDir.listFiles();
        for (File artifact : artifacts) {
            try {
                FileInputStream fis = new FileInputStream(artifact);
                byte[] data = new byte[(int) artifact.length()];
                fis.read(data);
                importerContext.addArtifact(artifact.getName(), data);
            } catch (IOException ex) {
                String message = "Internal error while reading artifact : " + artifact.getName();
                getLogger().error(message, ex);                
                // Skip the artifact.
            }
        }
        // Cleanup the mess.
        cleanTempStorage();
    }

    /**
     * Creates the temporary directory structure required for the conversion.
     * 
     * @param tempDirName Name of the top level temporary directory.
     */
    private void prepareTempStorage(String tempDirName)
    {
        tempDir = new File(System.getProperty("java.io.tmpdir"), tempDirName);
        tempDir.mkdir();
        inputFile = new File(tempDir, "input.tmp");
        outputDir = new File(tempDir, "output");
        outputDir.mkdir();
        outputFile = new File(outputDir, "output.html");
    }

    /**
     * Sweeps out all the temporary files created.
     */
    private void cleanTempStorage()
    {
        File[] outputFiles = outputDir.listFiles();
        for (File file : outputFiles) {
            file.delete();
        }
        outputDir.delete();
        inputFile.delete();
        tempDir.delete();
    }
}
