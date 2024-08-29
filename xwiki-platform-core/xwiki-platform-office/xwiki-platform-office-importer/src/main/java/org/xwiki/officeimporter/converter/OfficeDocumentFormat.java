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

import java.util.List;

/**
 * Representation of an OfficeDocumentFormat.
 *
 * @version $Id$
 * @since 13.1RC1
 */
public interface OfficeDocumentFormat
{
    /**
     * @return the name of the document format.
     */
    String getName();

    /**
     * @return the media content type corresponding to this format.
     */
    String getMediaType();

    /**
     * @return the list of known file extensions associated with this format.
     */
    List<String> getExtensions();

    /**
     * @return the first file extension associated with this format.
     */
    String getExtension();
}
