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
package org.xwiki.officeimporter.converter;

import java.io.InputStream;
import java.util.Map;

/**
 * Interface used to convert documents between various office formats.
 * 
 * @version $Id$
 * @since 5.0M2
 */
public interface OfficeConverter
{
    /**
     * Attempts to convert the input document identified by <b>inputStreams</b> and <b>inputFileName</b> arguments into
     * the format identified by <b>outputFileName</b> argument.
     * Note that this method does not perform a cleanup of the generated files so they can be read in the further steps.
     * Don't forget to call {@link OfficeConverterResult#close()} to not keep remaining files.
     * 
     * @param inputStreams input streams corresponding to the input document; it's possible that some document types
     *            (e.g. HTML) consists of more than one input stream corresponding to different artifacts embedded
     *            within document content
     * @param inputFileName name of the main input file within <b>inputStreams</b> map; this argument is used to
     *            determine the format of the input document
     * @param outputFileName name of the main output file; an entry corresponding to this name will be available in the
     *            results map if the conversion succeeds; This argument is used to determine the format of the output
     *            document
     * @return a result containing the paths of the files created during the conversion.
     * @throws OfficeConverterException if an error occurs during the conversion
     * @since 13.1RC1
     */
    default OfficeConverterResult convertDocument(Map<String, InputStream> inputStreams, String inputFileName,
        String outputFileName) throws OfficeConverterException
    {
        return null;
    }

    /**
     * @param officeFileName the office file name to recognize
     * @return true if the file name / extension represents an office presentation format
     * @since 13.1RC1
     */
    default boolean isPresentation(String officeFileName)
    {
        return false;
    }

    /**
     * Retrieve the office document format based on the given file name.
     * @param officeFileName the name of an office document.
     * @return the office document format associated with the given file extension.
     * @since 13.1RC1
     */
    default OfficeDocumentFormat getDocumentFormat(String officeFileName)
    {
        return null;
    }

    /**
     * Checks if the office documents with the specified media type can be converted by this converter.
     *
     * @param mediaType a media type
     * @return {@code true} if the specified media type is supported, {@code false} otherwise
     * @since 13.1RC1
     */
    default boolean isMediaTypeSupported(String mediaType)
    {
        return false;
    }

    /**
     * Use this method to check if the unidirectional conversion from a document format (input media type) to another
     * document format (output media type) is supported by this converter.
     *
     * @param inputMediaType the media type of the input document
     * @param outputMediaType the media type of the output document
     * @return {@code true} if a document can be converted from the input media type to the output media type,
     *         {@code false} otherwise
     * @since 13.1RC1
     */
    default boolean isConversionSupported(String inputMediaType, String outputMediaType)
    {
        return false;
    }
}
