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
import java.io.IOException;
import java.util.UUID;

/**
 * Keeps track of file system storage used by {@link org.xwiki.officeimporter.openoffice.OpenOfficeConverter} for a
 * particular conversion.
 * 
 * @version $Id$
 * @since 2.2M2
 */
public class OpenOfficeConverterFileStorage
{
    /**
     * Top-level temporary working directory.
     */
    private File rootDir;

    /**
     * Input directory where all the input files are located.
     */
    private File inputDir;

    /**
     * Input office document file.
     */
    private File inputFile;

    /**
     * Output directory.
     */
    private File outputDir;

    /**
     * Main output file.
     */
    private File outputFile;

    /**
     * Creates a new {@link OpenOfficeConverterFileStorage} instance for tracking file system storage for a convert
     * operation.
     * 
     * @param parentDir parent directory under which temporary storage is to be allocated.
     * @param inputFileName main input file which will be fed into openoffice server.
     * @param outputFileName main output file into which result of the openoffice conversion will be written into.
     * @throws IOException if an error occurs while creating temporary directory structure.
     */
    public OpenOfficeConverterFileStorage(File parentDir, String inputFileName, String outputFileName)
        throws IOException
    {
        boolean success = false;

        // Realize the temporary directory hierarchy.
        this.rootDir = new File(parentDir, UUID.randomUUID().toString());
        if (this.rootDir.mkdir()) {
            this.inputDir = new File(this.rootDir, "input");
            this.outputDir = new File(this.rootDir, "output");
            if (this.inputDir.mkdir() && this.outputDir.mkdir()) {
                this.inputFile = new File(this.inputDir, inputFileName);
                this.outputFile = new File(this.outputDir, outputFileName);
                success = true;
            }
        }

        // Cleanup & signal if an error is encountered.
        if (!success) {
            cleanUp();
            throw new IOException("Could not create temporary directory hierarchy.");
        }
    }

    /**
     * @return {@link File} representing the input directory where the main input document as well as any other
     *         dependent artifacts should be located.
     */
    public File getInputDir()
    {
        return this.inputDir;
    }

    /**
     * @return {@link File} representing the main input file.
     */
    public File getInputFile()
    {
        return this.inputFile;
    }

    /**
     * @return {@link File} representing the output directory where the main output file as well as any other dependent
     *         artifacts are located.
     */
    public File getOutputDir()
    {
        return this.outputDir;
    }

    /**
     * @return {@link File} representing the main output file.
     */
    public File getOutputFile()
    {
        return this.outputFile;
    }

    /**
     * Cleans up the allocated file storage.
     * 
     * @return true if the cleanup operation succeeded, false otherwise.
     */
    public boolean cleanUp()
    {
        if (this.rootDir.exists()) {
            return delete(this.rootDir);
        }

        return true;
    }

    /**
     * Utility method for deleting a directory or a file.
     * 
     * @param file file or directory to be deleted.
     * @return true if the whole operation succeeded, false otherwise.
     */
    private boolean delete(File file)
    {
        // If directory, recursively delete all of it's content.
        if (file.isDirectory()) {
            for (File child : file.listFiles()) {
                delete(child);
            }
        }

        // Finally attempt to delete the parent (or the ordinary file).
        return file.delete();
    }
}
