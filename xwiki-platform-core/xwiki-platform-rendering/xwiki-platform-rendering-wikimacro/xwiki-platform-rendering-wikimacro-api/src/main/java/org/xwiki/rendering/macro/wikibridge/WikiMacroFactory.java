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
package org.xwiki.rendering.macro.wikibridge;

import org.xwiki.component.annotation.Role;
import org.xwiki.model.reference.DocumentReference;

/**
 * Create a Wiki Macro object by gathering the Macro metadata from a document. 
 * 
 * @version $Id$
 * @since 2.0RC1
 */
@Role
public interface WikiMacroFactory
{
    /**
     * Searches the given document for a wiki macro definition.
     * 
     * @param documentReference name of the document to search for a wiki macro definition.
     * @return true if the given document contains a wiki macro definition, false otherwise.
     * @since 2.2M1
     */
    boolean containsWikiMacro(DocumentReference documentReference);
    
    /**
     * Tries to build a {@link WikiMacro} if a definition is found on the given document.
     * 
     * @param documentReference name of the document on which the macro is defined.
     * @return a {@link WikiMacro} corresponding to the macro definition found.
     * @throws WikiMacroException if no macro definition is found or if an error is encountered while building
     *             the macro.
     * @since 2.2M1
     */
    WikiMacro createWikiMacro(DocumentReference documentReference) throws WikiMacroException;
}
