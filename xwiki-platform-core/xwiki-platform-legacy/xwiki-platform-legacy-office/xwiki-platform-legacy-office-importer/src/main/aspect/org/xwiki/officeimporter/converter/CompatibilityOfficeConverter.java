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

public interface CompatibilityOfficeConverter
{
    /**
     * Attempts to convert the input document identified by <b>inputStreams</b> and <b>inputFileName</b> arguments into
     * the format identified by <b>outputFileName</b> argument.
     *
     * @param inputStreams input streams corresponding to the input document; it's possible that some document types
     *            (e.g. HTML) consists of more than one input stream corresponding to different artifacts embedded
     *            within document content
     * @param inputFileName name of the main input file within <b>inputStreams</b> map; this argument is used to
     *            determine the format of the input document
     * @param outputFileName name of the main output file; an entry corresponding to this name will be available in the
     *            results map if the conversion succeeds; This argument is used to determine the format of the output
     *            document
     * @return map of file names to file contents resulting from the conversion
     * @throws OfficeConverterException if an error occurs during the conversion
     * @deprecated Since 13.1RC1 use {@link OfficeConverter#convertDocument(Map, String, String)}.
     */
    @Deprecated
    Map<String, byte[]> convert(Map<String, InputStream> inputStreams, String inputFileName, String outputFileName)
        throws OfficeConverterException;
}
