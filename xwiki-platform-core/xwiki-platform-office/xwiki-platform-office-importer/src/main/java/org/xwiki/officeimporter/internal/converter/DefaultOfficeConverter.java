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
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.jodconverter.core.DocumentConverter;
import org.jodconverter.core.document.DocumentFamily;
import org.jodconverter.core.document.DocumentFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.officeimporter.converter.OfficeConverter;
import org.xwiki.officeimporter.converter.OfficeConverterException;
import org.xwiki.officeimporter.converter.OfficeDocumentFormat;

/**
 * Default {@link OfficeConverter} implementation.
 * 
 * @version $Id$
 * @since 5.0M2
 */
public class DefaultOfficeConverter implements OfficeConverter
{
    private static final String CONVERSION_ERROR_MESSAGE = "Error while performing conversion.";

    /**
     * The logger to log.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultOfficeConverter.class);

    /**
     * Converter provided by JODConverter library.
     */
    private DocumentConverter converter;

    /**
     * Working directory to be used when working with files.
     */
    private File workDir;

    /**
     * Creates a new {@link DefaultOfficeConverter} instance.
     * 
     * @param converter provided by JODConverter library.
     * @param workDir space for holding temporary file.
     */
    public DefaultOfficeConverter(DocumentConverter converter, File workDir)
    {
        this.converter = converter;
        this.workDir = workDir;
    }

    private void checkInputStream(Map<String, InputStream> inputStreams, String inputFileName)
        throws OfficeConverterException
    {
        // Verify whether an input stream is present for the main input file.
        if (null == inputStreams.get(inputFileName)) {
            String message = "No input stream specified for main input file [%s].";
            throw new OfficeConverterException(String.format(message, inputFileName));
        }
    }

    @Override
    public DefaultOfficeConverterResult convertDocument(Map<String, InputStream> inputStreams, String inputFileName,
        String outputFileName) throws OfficeConverterException
    {
        this.checkInputStream(inputStreams, inputFileName);

        try {
            // Prepare temporary storage.
            OfficeConverterFileStorage storage = new OfficeConverterFileStorage(this.workDir, inputFileName,
                outputFileName);

            // Check that the potentially cleaned filename is actually in the input streams.
            this.checkInputStream(inputStreams, storage.getInputFile().getName());

            // Write out all the input streams.
            for (Map.Entry<String, InputStream> entry : inputStreams.entrySet()) {
                File temp = new File(storage.getInputDir(), entry.getKey());
                try (FileOutputStream fos = new FileOutputStream(temp)) {
                    IOUtils.copy(entry.getValue(), fos);
                }
            }

            // Perform the conversion.
            this.converter.convert(storage.getInputFile())
                .to(storage.getOutputFile())
                .execute();

            return new DefaultOfficeConverterResult(storage);
        } catch (Exception ex) {
            throw new OfficeConverterException(CONVERSION_ERROR_MESSAGE, ex);
        }
    }

    @Override
    public boolean isPresentation(String officeFileName)
    {
        String extension = officeFileName.substring(officeFileName.lastIndexOf('.') + 1);

        DocumentFormat format = this.converter.getFormatRegistry().getFormatByExtension(extension);
        return format != null && format.getInputFamily() == DocumentFamily.PRESENTATION;
    }

    @Override
    public boolean isMediaTypeSupported(String mediaType)
    {
        return this.converter.getFormatRegistry().getFormatByMediaType(mediaType) != null;
    }

    @Override
    public boolean isConversionSupported(String inputMediaType, String outputMediaType)
    {
        DocumentFormat inputFormat = converter.getFormatRegistry().getFormatByMediaType(inputMediaType);
        DocumentFormat outputFormat = converter.getFormatRegistry().getFormatByMediaType(outputMediaType);
        return inputFormat != null && outputFormat != null
            && outputFormat.getStoreProperties(inputFormat.getInputFamily()) != null;
    }

    @Override
    public OfficeDocumentFormat getDocumentFormat(String officeFileName)
    {
        String extension = officeFileName.substring(officeFileName.lastIndexOf('.') + 1);
        DocumentFormat format = this.converter.getFormatRegistry().getFormatByExtension(extension);
        return format != null ? new DefaultOfficeDocumentFormat(format) : null;
    }
}
