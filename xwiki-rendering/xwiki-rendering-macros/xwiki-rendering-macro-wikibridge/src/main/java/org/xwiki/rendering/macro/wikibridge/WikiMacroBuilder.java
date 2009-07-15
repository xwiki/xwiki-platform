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

import org.xwiki.component.annotation.ComponentRole;

/**
 * Component interface for defining wiki macro builders.
 * 
 * @version $Id$
 * @since 2.0M2
 */
@ComponentRole
public interface WikiMacroBuilder
{
    /**
     * Constant for representing XWiki.WikiMacroClass xwiki class.
     */
    String WIKI_MACRO_CLASS = "XWiki.WikiMacroClass";

    /**
     * Constant for representing macro name property.
     */
    String MACRO_NAME_PROPERTY = "name";
    
    /**
     * Constant for representing macro name property.
     */
    String MACRO_DESCRIPTION_PROPERTY = "description";
    
    /**
     * Constant for representing macro name property.
     */
    String MACRO_CONTENT_PROPERTY = "content";
    
    /**
     * Constant for representing XWiki.WikiMacroParameterClass xwiki class.
     */
    String WIKI_MACRO_PARAMETER_CLASS = "XWiki.WikiMacroParameterClass";
    
    /**
     * Constant for representing parameter name property.
     * 
     * Same as MACRO_NAME_PROPERTY (Check style Fix)
     */
    String PARAMETER_NAME_PROPERTY = MACRO_NAME_PROPERTY; 
    
    /**
     * Constant for representing parameter description property.
     * 
     * Same as MACRO_DESCRIPTION_PROPERTY (Check style Fix)
     */
    String PARAMETER_DESCRIPTION_PROPERTY = MACRO_DESCRIPTION_PROPERTY; 
    
    /**
     * Constant for representing parameter mandatory property.
     */
    String PARAMETER_MANDATORY_PROPERTY = "mandatory";    
    
    /**
     * Searches the given document for a wiki macro definition and tries to build a {@link WikiMacro} if a definition is
     * found.
     * 
     * @param documentName name of the document to search for wiki macros.
     * @return a {@link WikiMacro} corresponding to the macro definition found.
     * @throws WikiMacroBuilderException if no macro definition is found or if an error is encountered while building
     *             the macro.
     */
    WikiMacro buildMacro(String documentName) throws WikiMacroBuilderException;
}
