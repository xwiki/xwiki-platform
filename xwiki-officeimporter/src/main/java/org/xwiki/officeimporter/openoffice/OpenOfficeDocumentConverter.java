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

import org.xwiki.officeimporter.OfficeImporterException;
import org.xwiki.officeimporter.internal.OfficeImporterFileStorage;

/**
 * Component interface for converting office documents into html.
 * 
 * @version $Id$
 * @since 1.8RC3
 */
public interface OpenOfficeDocumentConverter
{
    /**
     * Converts the input office document into html. This conversion results in multiple files on disk including
     * output.html which holds the html equivalent of the office document. Non textual data present in the document will
     * be converted into images. This method returns an {@link InputStream} object for each resulting output file.
     * 
     * @param in the {@link InputStream} of the office document.
     * @param storage temporary disk storage to hold the resulting artifacts.
     * @return
     */
    public Map<String, InputStream> convert(InputStream in, OfficeImporterFileStorage storage)
        throws OfficeImporterException;
}
