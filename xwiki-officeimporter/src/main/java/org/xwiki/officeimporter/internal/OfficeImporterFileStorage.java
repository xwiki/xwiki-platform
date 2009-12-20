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

import java.io.File;

import org.xwiki.officeimporter.OfficeImporterException;

/**
 * Keeps track of file system storage used by office importer for a particular conversion.
 * 
 * @version $Id$
 * @since 1.8M2
 * @deprecated since 2.2M2
 */
@Deprecated
public class OfficeImporterFileStorage
{
    /**
     * Some characters are not allowed as file names under the windows platform. Following regular expression is used to
     * match such characters.
     */
    public static final String INVALID_FILE_NAME_CHARS = "[/\\\\:\\*\\?\"<>|]";
    
    /**
     * Top-level temporary working directory.
     */
    private File storageDir;

    /**
     * Storage for input document.
     */
    private File inputFile;

    /**
     * Output directory (inside tempDir). All the by-products (artifacts) + outputFile will land here.
     */
    private File outputDir;

    /**
     * Output file (html).
     */
    private File outputFile;    

    /**
     * Creates a new {@link OfficeImporterFileStorage} instance.
     * 
     * @param tempDir temporary directory to be used.
     * @param uid unique id to be appended when creating the storage directory (to avoid conflicts).
     * @throws OfficeImporterException if an error occurs while creating temporary directories.
     */
    public OfficeImporterFileStorage(File tempDir, String uid) throws OfficeImporterException
    {
        String storageDirName = "xwiki-officeimporter-" + uid.replaceAll(INVALID_FILE_NAME_CHARS, "-");
        storageDir = new File(tempDir, storageDirName);
        boolean success = false;
        if (storageDir.mkdir()) {
            inputFile = new File(storageDir, "input.tmp");
            outputDir = new File(storageDir, "output");
            if (outputDir.mkdir()) {
                outputFile = new File(outputDir, "output.html");
                success = true;
            }
        }
        if (!success) {
            throw new OfficeImporterException("Error while creating temporary files.");
        }
    }

    /**
     * @return the input file holding the content for jodaconverter.
     */
    public File getInputFile()
    {
        return inputFile;
    }

    /**
     * @return the output directory where the output form jodaconverter lands.
     */
    public File getOutputDir()
    {
        return outputDir;
    }

    /**
     * @return the main html output file for the conversion.
     */
    public File getOutputFile()
    {
        return outputFile;
    }
    
    /**
     * Cleans up the allocated file storage.
     */
    public void cleanUp()
    {
        File[] outputFiles = outputDir.listFiles();
        for (File file : outputFiles) {
            file.delete();
        }
        outputDir.delete();
        inputFile.delete();
        storageDir.delete();
    }
}
