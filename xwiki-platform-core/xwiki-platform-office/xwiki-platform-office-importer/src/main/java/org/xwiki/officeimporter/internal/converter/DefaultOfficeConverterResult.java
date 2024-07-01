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
package org.xwiki.officeimporter.internal.converter;

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.xwiki.officeimporter.converter.OfficeConverterException;
import org.xwiki.officeimporter.converter.OfficeConverterResult;

/**
 * Default implementation of {@link OfficeConverterResult}.
 *
 * @version $Id$
 * @since 13.1RC1
 */
public class DefaultOfficeConverterResult implements OfficeConverterResult
{
    private final OfficeConverterFileStorage fileStorage;
    private final Set<File> allFiles;

    /**
     * Default constructor.
     * This constructor performs checks on the existence of output directory and output file.
     *
     * @param fileStorage the file storage used during the conversion.
     * @throws OfficeConverterException in case the output directory or the output file does not exist.
     */
    public DefaultOfficeConverterResult(OfficeConverterFileStorage fileStorage) throws
        OfficeConverterException
    {
        this.fileStorage = fileStorage;
        File[] files = fileStorage.getOutputDir().listFiles();
        if (files == null) {
            throw new OfficeConverterException(
                String.format("The output directory of the office conversion does not contain any files: [%s]",
                    fileStorage.getOutputDir().getAbsolutePath()));
        }
        this.allFiles = new HashSet<>(Arrays.asList(files));

        if (!this.fileStorage.getOutputFile().exists()) {
            throw new OfficeConverterException(
                String.format("Output file [%s] does not exist.", fileStorage.getOutputFile().getAbsolutePath()));
        }
    }

    @Override
    public File getOutputFile()
    {
        return this.fileStorage.getOutputFile();
    }

    @Override
    public File getOutputDirectory()
    {
        return this.fileStorage.getOutputDir();
    }

    @Override
    public Set<File> getAllFiles()
    {
        return this.allFiles;
    }

    @Override
    public void close()
    {
        this.fileStorage.cleanUp();
    }
}
