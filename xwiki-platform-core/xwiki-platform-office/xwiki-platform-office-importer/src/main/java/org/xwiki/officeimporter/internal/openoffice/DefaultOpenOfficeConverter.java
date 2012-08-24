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
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.artofsolving.jodconverter.OfficeDocumentConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.officeimporter.openoffice.OpenOfficeConverter;
import org.xwiki.officeimporter.openoffice.OpenOfficeConverterException;

/**
 * Default implementation of {@link OpenOfficeConverter}.
 * 
 * @version $Id$
 * @since 2.2M1
 */
public class DefaultOpenOfficeConverter implements OpenOfficeConverter
{
    /**
     * The logger to log.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultOpenOfficeConverter.class);

    /**
     * Converter provided by jodconverter library.
     */
    private OfficeDocumentConverter converter;

    /**
     * Working directory to be used when working with files.
     */
    private File workDir;

    /**
     * Creates a new {@link DefaultOpenOfficeConverter} instance.
     * 
     * @param converter provided by jodconverter library.
     * @param workDir space for holding temporary file.
     */
    public DefaultOpenOfficeConverter(OfficeDocumentConverter converter, File workDir)
    {
        this.converter = converter;
        this.workDir = workDir;
    }

    @Override
    public Map<String, byte[]> convert(Map<String, InputStream> inputStreams, String inputFileName,
        String outputFileName) throws OpenOfficeConverterException
    {
        // Verify whether an input stream is present for the main input file.
        if (null == inputStreams.get(inputFileName)) {
            String message = "No input stream specified for main input file [%s].";
            throw new OpenOfficeConverterException(String.format(message, inputFileName));
        }

        OpenOfficeConverterFileStorage storage = null;
        try {
            // Prepare temporary storage.
            storage = new OpenOfficeConverterFileStorage(this.workDir, inputFileName, outputFileName);

            // Write out all the input streams.
            FileOutputStream fos = null;
            for (Map.Entry<String, InputStream> entry : inputStreams.entrySet()) {
                try {
                    File temp = new File(storage.getInputDir(), entry.getKey());
                    fos = new FileOutputStream(temp);
                    IOUtils.copy(entry.getValue(), fos);
                } finally {
                    IOUtils.closeQuietly(fos);
                }
            }

            // Perform the conversion.
            this.converter.convert(storage.getInputFile(), storage.getOutputFile());

            // Collect all the output artifacts.
            Map<String, byte[]> result = new HashMap<String, byte[]>();
            FileInputStream fis = null;
            for (File file : storage.getOutputDir().listFiles()) {
                try {
                    fis = new FileInputStream(file);
                    result.put(file.getName(), IOUtils.toByteArray(fis));
                } finally {
                    IOUtils.closeQuietly(fis);
                }
            }

            return result;
        } catch (Exception ex) {
            throw new OpenOfficeConverterException("Error while performing conversion.", ex);
        } finally {
            if (!storage.cleanUp()) {
                LOGGER.error("Could not cleanup temporary storage after conversion.");
            }
        }
    }

    @Override
    public boolean isMediaTypeSupported(String mediaType)
    {
        return this.converter.getFormatRegistry().getFormatByMediaType(mediaType) != null;
    }
}
