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
package org.xwiki.lesscss.resources;

import org.xwiki.lesscss.compiler.LESSCompilerException;
import org.xwiki.model.reference.DocumentReference;

/**
 * Extends {@link LESSResourceReference} with wiki entity specific information.
 * 
 * @version $Id$
 * @since 14.0RC1
 */
public interface WikiLESSResourceReference extends LESSResourceReference
{
    /**
     * @return the reference of the document to use as secure document (generally the document containing the code to
     *     execute) or null if the template is not associated with any document (for example filesystem template)
     * @throws LESSCompilerException in case of issue when resolving the document reference
     */
    DocumentReference getDocumentReference() throws LESSCompilerException;

    /**
     * @return the author of this resource
     * @throws LESSCompilerException in case of issue when resolving the author reference
     */
    DocumentReference getAuthorReference() throws LESSCompilerException;
}
