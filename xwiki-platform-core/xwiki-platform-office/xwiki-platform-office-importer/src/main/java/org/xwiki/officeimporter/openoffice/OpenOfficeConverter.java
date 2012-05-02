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
package org.xwiki.officeimporter.openoffice;

import java.io.InputStream;
import java.util.Map;

/**
 * Converter interface to be used for performing document conversion tasks.
 * 
 * @version $Id$
 * @since 2.2M1
 */
public interface OpenOfficeConverter
{
    /**
     * <p>
     * Attempts to convert the input document identified by <b>inputStreams</b> and <b>inputFileName</b> arguments into
     * the format identified by <b>outputFileName</b> argument.
     * </p>
     * 
     * @param inputStreams input streams corresponding to the input document. It's possible that some document types
     *        (e.g. html) consists of more than one input stream corresponding to different artifacts embedded within
     *        document content.
     * @param inputFileName name of the main input file within <b>inputStreams</b> map. This argument is used to
     *        determine the format of the input document.
     * @param outputFileName name of the main output file. An entry corresponding to this name will be available in the
     *        results map if the conversion succeeds. This argument is used to determine the format of the output
     *        document.
     * @return map of file names to file contents resulting from the conversion.
     * @throws OpenOfficeConverterException if an error occurs during the conversion.
     * @since 2.2M1
     */
    Map<String, byte[]> convert(Map<String, InputStream> inputStreams, String inputFileName, String outputFileName)
        throws OpenOfficeConverterException;

    /**
     * Checks if the office documents with the specified media type can be converted by this converter.
     * 
     * @param mediaType a media type
     * @return {@code true} if the specified media type is supported, {@code false} otherwise
     * @since 3.0M1
     */
    boolean isMediaTypeSupported(String mediaType);
}
