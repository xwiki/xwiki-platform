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
package org.xwiki.officeimporter.transformer;

import org.xwiki.officeimporter.OfficeImporterContext;
import org.xwiki.officeimporter.OfficeImporterException;

/**
 * A Document Transformer is responsible for transforming a document (Office, HTML, etc) into some
 * other format.
 * 
 * @version $Id$
 * @since 1.8M1
 */
public interface DocumentTransformer
{    
    /**
     * Transforms a document into some other format. The specific transformation is up to the
     * implementing class. If there are any artifacts collected while performing the transformation
     * (like images), they should be stored into the {@link OfficeImporterContext}.
     * 
     * @param importerContext The {@link OfficeImporterContext} holding all the information about the input document.
     */
    void transform(OfficeImporterContext importerContext) throws OfficeImporterException;
}
