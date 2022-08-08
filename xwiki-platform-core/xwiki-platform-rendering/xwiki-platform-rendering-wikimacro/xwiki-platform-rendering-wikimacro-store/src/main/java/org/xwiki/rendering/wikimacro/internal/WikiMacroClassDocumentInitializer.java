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

import java.util.Arrays;

import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.component.annotation.Component;

import com.xpn.xwiki.internal.mandatory.AbstractAsyncClassDocumentInitializer;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.objects.classes.ListClass;
import com.xpn.xwiki.objects.classes.TextAreaClass;

/**
 * Update XWiki.WikiMacroClass document with all required informations.
 * 
 * @version $Id$
 * @since 4.3M1
 */
@Component
@Named(WikiMacroConstants.WIKI_MACRO_CLASS)
@Singleton
public class WikiMacroClassDocumentInitializer extends AbstractAsyncClassDocumentInitializer
    implements WikiMacroConstants
{
    private static final String PROPERTY_PIPE = "|";

    /**
     * Default constructor.
     */
    public WikiMacroClassDocumentInitializer()
    {
        super(WIKI_MACRO_CLASS_REFERENCE);
    }

    @Override
    protected void createClass(BaseClass xclass)
    {
        xclass.addTextField(MACRO_ID_PROPERTY, "Macro id", 30);
        xclass.addTextField(MACRO_NAME_PROPERTY, "Macro name", 30);
        // The Macro description is using plain text (same as for Java Macros).
        xclass.addTextAreaField(MACRO_DESCRIPTION_PROPERTY, "Macro description", 40, 5,
            TextAreaClass.ContentType.PURE_TEXT);
        xclass.addStaticListField(MACRO_DEFAULT_CATEGORIES_PROPERTY, "Default categories", 1, true, "", "input");
        xclass.addBooleanField(MACRO_INLINE_PROPERTY, "Supports inline mode", "yesno");
        xclass.addStaticListField(MACRO_VISIBILITY_PROPERTY, "Macro visibility", 1, false,
            "Current User|Current Wiki|Global", ListClass.DISPLAYTYPE_SELECT, PROPERTY_PIPE);
        xclass.addStaticListField(MACRO_CONTENT_TYPE_PROPERTY, "Macro content availability", 1, false,
            "Optional|Mandatory|No content", ListClass.DISPLAYTYPE_SELECT, PROPERTY_PIPE);

        xclass.addStaticListField(MACRO_CONTENT_JAVA_TYPE_PROPERTY, "Macro content type", 1, false, false,
            StringUtils.join(Arrays.asList(MACRO_CONTENT_TYPE_UNKNOWN, MACRO_CONTENT_TYPE_WIKI), PROPERTY_PIPE),
            ListClass.DISPLAYTYPE_INPUT, PROPERTY_PIPE, MACRO_CONTENT_TYPE_UNKNOWN, ListClass.FREE_TEXT_ALLOWED, true);
        // The Macro content description is using plain text (same as for Java Macros).
        xclass.addTextAreaField(MACRO_CONTENT_DESCRIPTION_PROPERTY,
            "Content description (Not applicable for \"No content\" type)", 40, 5, TextAreaClass.ContentType.PURE_TEXT);
        // The code property contains wiki markup
        xclass.addTextAreaField(MACRO_CODE_PROPERTY, "Macro code", 40, 20, TextAreaClass.EditorType.TEXT);

        xclass.addNumberField(MACRO_PRIORITY_PROPERTY, "Priority", 10, "integer");

        super.createClass(xclass);
    }
}
