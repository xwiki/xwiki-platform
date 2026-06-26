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

import org.xwiki.model.reference.LocalDocumentReference;

/**
 * Constants identifying various properties used for defining wiki macros.
 *
 * @version $Id$
 * @since 2.0M4
 */
public interface WikiMacroConstants
{
    /**
     * Constant for representing XWiki.WikiMacroClass xwiki class space name.
     */
    String WIKI_MACRO_CLASS_SPACE = "XWiki";

    /**
     * Constant for representing XWiki.WikiMacroClass xwiki class page name.
     */
    String WIKI_MACRO_CLASS_PAGE = "WikiMacroClass";

    /**
     * Constant for representing XWiki.WikiMacroClass xwiki class.
     */
    String WIKI_MACRO_CLASS = WIKI_MACRO_CLASS_SPACE + '.' + WIKI_MACRO_CLASS_PAGE;

    /**
     * Constant for representing XWiki.WikiMacroClass xwiki class local reference.
     * 
     * @since 11.8RC1
     */
    LocalDocumentReference WIKI_MACRO_CLASS_REFERENCE =
        new LocalDocumentReference(WIKI_MACRO_CLASS_SPACE, WIKI_MACRO_CLASS_PAGE);

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
     * Constant for representing default macro categories property.
     * @since 14.6RC1
     */
    String MACRO_DEFAULT_CATEGORIES_PROPERTY = "defaultCategories";

    /**
     * Name of the macro visibility property in the Wiki Macro Class.
     */
    String MACRO_VISIBILITY_PROPERTY = "visibility";

    /**
     * Constant for representing macro inline support property.
     */
    String MACRO_INLINE_PROPERTY = "supportsInlineMode";

    /**
     * Constant for representing macro content optionality property.
     */
    String MACRO_CONTENT_TYPE_PROPERTY = "contentType";

    /**
     * Constant for representing macro content java type property.
     */
    String MACRO_CONTENT_JAVA_TYPE_PROPERTY = "contentJavaType";

    /**
     * Constant for the "Wiki" choice for Macro Content Type.
     */
    String MACRO_CONTENT_TYPE_WIKI = "Wiki";

    /**
     * Constant for the "Unkown" choice for Macro Content Type.
     */
    String MACRO_CONTENT_TYPE_UNKNOWN = "Unknown";

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
     * Constant for representing macro priority property.
     * 
     * @since 13.1RC1
     */
    String MACRO_PRIORITY_PROPERTY = "priority";

    /**
     * Constant for representing if the macro execution is isolated.
     *
     * @since 17.3.0RC1
     * @since 16.10.9
     */
    String MACRO_EXECUTION_ISOLATED_PROPERTY = "executionIsolated";

    /**
     * Constant for representing XWiki.WikiMacroParameterClass xwiki class space name.
     */
    String WIKI_MACRO_PARAMETER_CLASS_SPACE = "XWiki";

    /**
     * Constant for representing XWiki.WikiMacroParameterClass xwiki class page name.
     */
    String WIKI_MACRO_PARAMETER_CLASS_PAGE = "WikiMacroParameterClass";

    /**
     * Constant for representing XWiki.WikiMacroParameterClass xwiki class.
     */
    String WIKI_MACRO_PARAMETER_CLASS = WIKI_MACRO_PARAMETER_CLASS_SPACE + '.' + WIKI_MACRO_PARAMETER_CLASS_PAGE;
    /**
     * Constant for representing XWiki.WikiMacroParameterClass local reference.
     * @since 17.3.0RC1
     * @since 16.10.6
     */
    LocalDocumentReference WIKI_MACRO_PARAMETER_CLASS_REFERENCE =
        new LocalDocumentReference(WIKI_MACRO_PARAMETER_CLASS_SPACE, WIKI_MACRO_PARAMETER_CLASS_PAGE);

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
     * 
     * @since 2.3M1
     */
    String PARAMETER_DEFAULT_VALUE_PROPERTY = "defaultValue";

    /**
     * Constant for representing parameter type property.
     *
     * @since 10.10RC1
     */
    String PARAMETER_TYPE_PROPERTY = "type";

    /**
     * Constant for the "Wiki" choice for Macro Content Type.
     *
     * @since 15.3RC1
     */
    String PARAMETER_TYPE_WIKI = MACRO_CONTENT_TYPE_WIKI;

    /**
     * Constant for the "Unkown" choice for Macro Content Type.
     *
     * @since 15.3RC1
     */
    String PARAMETER_TYPE_UNKNOWN = MACRO_CONTENT_TYPE_UNKNOWN;

    /**
     * Constant for representing the feature property of the parameter.
     *
     * @since 17.3.0RC1
     * @since 16.10.6
     */
    String PARAMETER_FEATURE_PROPERTY = "feature";

    /**
     * Constant for representing the group property of the parameter.
     *
     * @since 17.3.0RC1
     * @since 16.10.6
     */
    String PARAMETER_GROUP_PROPERTY = "group";

    /**
     * Constant for representing the hidden property of the parameter.
     *
     * @since 17.3.0RC1
     * @since 16.10.6
     */
    String PARAMETER_HIDDEN_PROPERTY = "hidden";

    /**
     * Constant for representing the advanced property of the parameter.
     *
     * @since 17.3.0RC1
     * @since 16.10.6
     */
    String PARAMETER_ADVANCED_PROPERTY = "advanced";

    /**
     * Constant for representing the feature mandatory property of the parameter.
     *
     * @since 17.3.0RC1
     * @since 16.10.6
     */
    String PARAMETER_FEATURE_MANDATORY_PROPERTY = "featureMandatory";

    /**
     * Constant for representing the deprecated property of the parameter.
     *
     * @since 17.3.0RC1
     * @since 16.10.6
     */
    String PARAMETER_DEPRECATED_PROPERTY = "deprecated";

    /**
     * Constant for representing the order of the parameter.
     *
     * @since 17.5.0
     */
    String PARAMETER_ORDER_PROPERTY = "order";

    /**
     * Constant for representing the order the content should have to be displayed among other parameters.
     *
     * @since 17.5.0
     */
    String MACRO_CONTENT_ORDER_PROPERTY = "contentOrder";
}
