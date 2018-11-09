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
import java.io.InputStream;
import java.util.Map;

import org.xwiki.officeimporter.converter.OfficeConverter;
import org.xwiki.officeimporter.converter.OfficeConverterException;
import org.xwiki.officeimporter.internal.converter.DefaultOfficeConverter;
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
     * The office document converter.
     */
    private final OfficeConverter converter;

    /**
     * Creates a new instance that wraps the given converter.
     * 
     * @param converter the actual converter
     */
    public DefaultOpenOfficeConverter(OfficeConverter converter)
    {
        this.converter = converter;
    }

    @Override
    public Map<String, byte[]> convert(Map<String, InputStream> inputStreams, String inputFileName,
        String outputFileName) throws OpenOfficeConverterException
    {
        try {
            return converter.convert(inputStreams, inputFileName, outputFileName);
        } catch (OfficeConverterException e) {
            throw new OpenOfficeConverterException(e.getMessage(), e.getCause());
        }
    }

    @Override
    public boolean isMediaTypeSupported(String mediaType)
    {
        return this.converter.getFormatRegistry().getFormatByMediaType(mediaType) != null;
    }
}
