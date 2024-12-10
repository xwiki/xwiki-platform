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
package org.xwiki.officeimporter.document;

import java.io.InputStream;

import org.xwiki.officeimporter.OfficeImporterException;

/**
 * An artifact for an office document.
 *
 * @version $Id$
 * @since 14.10.8
 * @since 15.3RC1
 */
public interface OfficeDocumentArtifact
{
    /**
     * @return the name of the artifact
     */
    String getName();

    /**
     * @return an input stream for reading the artifact's content
     */
    InputStream getContentInputStream() throws OfficeImporterException;
}
