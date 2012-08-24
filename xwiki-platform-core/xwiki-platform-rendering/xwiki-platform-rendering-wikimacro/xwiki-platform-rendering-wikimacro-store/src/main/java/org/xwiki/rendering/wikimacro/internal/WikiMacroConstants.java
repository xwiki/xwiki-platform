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
package org.xwiki.rendering.wikimacro.internal;

/**
 * Constants identifying various properties used for defining wiki macros.
 *
 * @version $Id$
 * @since 2.0M4
 */
public interface WikiMacroConstants
{
    /**
     * Constant for representing XWiki.WikiMacroClass xwiki class.
     */
    String WIKI_MACRO_CLASS = "XWiki.WikiMacroClass";
    
    /**
     * Constant for representing macro id property.
     */
    String MACRO_ID_PROPERTY = "id";

    /**
     * Constant for representing macro name property.
     */
    String MACRO_NAME_PROPERTY = "name";

    /**
     * Constant for representing macro description property.
     */
    String MACRO_DESCRIPTION_PROPERTY = "description";

    /**
     * Constant for representing default macro category property.
     */
    String MACRO_DEFAULT_CATEGORY_PROPERTY = "defaultCategory";

    /**
     * Name of the macro visibility property in the Wiki Macro Class.
     */
    String MACRO_VISIBILITY_PROPERTY = "visibility";
    
    /**
     * Constant for representing macro inline support property.
     */
    String MACRO_INLINE_PROPERTY = "supportsInlineMode";

    /**
     * Constant for representing macro content type property.
     */
    String MACRO_CONTENT_TYPE_PROPERTY = "contentType";

    /**
     * Constant for representing macro content mandatory status.
     */
    String MACRO_CONTENT_MANDATORY = "Mandatory";

    /**
     * Constant for representing macro content optional status.
     */
    String MACRO_CONTENT_OPTIONAL = "Optional";

    /**
     * Constant for representing macro content empty status.
     */
    String MACRO_CONTENT_EMPTY = "No content";

    /**
     * Constant for representing macro content description property.
     */
    String MACRO_CONTENT_DESCRIPTION_PROPERTY = "contentDescription";

    /**
     * Constant for representing macro code property.
     */
    String MACRO_CODE_PROPERTY = "code";

    /**
     * Constant for representing XWiki.WikiMacroParameterClass xwiki class.
     */
    String WIKI_MACRO_PARAMETER_CLASS = "XWiki.WikiMacroParameterClass";

    /**
     * Constant for representing parameter name property. Same as MACRO_NAME_PROPERTY (Check style Fix)
     */
    String PARAMETER_NAME_PROPERTY = "name";

    /**
     * Constant for representing parameter description property. Same as MACRO_DESCRIPTION_PROPERTY (Check style Fix)
     */
    String PARAMETER_DESCRIPTION_PROPERTY = "description";

    /**
     * Constant for representing parameter mandatory property.
     */
    String PARAMETER_MANDATORY_PROPERTY = "mandatory";
    
    /**
     * Constant for representing parameter defaultValue property.
     * @since 2.3M1
     */
    String PARAMETER_DEFAULT_VALUE_PROPERTY = "defaultValue";
}
