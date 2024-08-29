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
package org.xwiki.test.docker.internal.junit5;

import java.io.File;
import java.io.IOException;

import org.codehaus.plexus.archiver.zip.ZipUnArchiver;

/**
 * Utility methods related to File for setting up the test framework (unzip to directory, create directory, copy file,
 * etc).
 *
 * @version $Id$
 * @since 11.10.2
 * @since 12.0RC1
 */
public final class FileTestUtils
{
    private FileTestUtils()
    {
        // Prevents instantiation.
    }

    /**
     * @param source the zip file to unzip
     * @param targetDirectory the directory in which to unzip
     * @throws Exception when an error occurs during the unzip
     */
    public static void unzip(File source, File targetDirectory) throws Exception
    {
        createDirectory(targetDirectory);
        try {
            ZipUnArchiver unArchiver = new ZipUnArchiver();
            unArchiver.setSourceFile(source);
            unArchiver.setDestDirectory(targetDirectory);
            unArchiver.setOverwrite(true);
            unArchiver.extract();
        } catch (Exception e) {
            throw new Exception(
                String.format("Error unpacking file [%s] into [%s]", source, targetDirectory), e);
        }
    }

    /**
     * @param directory the directory to create. Works even if the directory already exists
     */
    public static void createDirectory(File directory)
    {
        if (!directory.exists()) {
            directory.mkdirs();
        }
    }

    /**
     * @param source the file to copy into the target directory, but only if the file is not already there or if it's
     * been modified
     * @param targetDirectory the directory into which to copy the file
     * @throws Exception when an error occurs during the copy
     */
    public static void copyFile(File source, File targetDirectory) throws Exception
    {
        try {
            org.codehaus.plexus.util.FileUtils.copyFileToDirectoryIfModified(source, targetDirectory);
        } catch (IOException e) {
            throw new Exception(String.format("Failed to copy file [%s] to [%s]", source, targetDirectory),
                e);
        }
    }

    /**
     * @param sourceDirectory the directory to copy
     * @param targetDirectory the directory into which to copy the files
     * @throws Exception when an error occurs during the copy
     */
    public static void copyDirectory(File sourceDirectory, File targetDirectory) throws Exception
    {
        createDirectory(targetDirectory);
        try {
            org.codehaus.plexus.util.FileUtils.copyDirectoryStructureIfModified(sourceDirectory, targetDirectory);
        } catch (IOException e) {
            throw new Exception(
                String.format("Failed to copy directory [%s] to [%s]", sourceDirectory, targetDirectory), e);
        }
    }
}
