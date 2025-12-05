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

import com.xpn.xwiki.doc.AbstractMandatoryClassInitializer;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.objects.classes.ListClass;
import com.xpn.xwiki.objects.classes.NumberClass;

/**
 * Update XWiki.WikiMacroParameterClass document with all required informations.
 * 
 * @version $Id$
 * @since 4.3M1
 */
@Component
@Named(WikiMacroConstants.WIKI_MACRO_PARAMETER_CLASS)
@Singleton
public class WikiMacroParameterClassDocumentInitializer extends AbstractMandatoryClassInitializer
    implements WikiMacroConstants
{
    private static final String PROPERTY_PIPE = "|";
    private static final String PROPERTY_YESNO = "yesno";

    /**
     * Default constructor.
     */
    public WikiMacroParameterClassDocumentInitializer()
    {
        super(WIKI_MACRO_PARAMETER_CLASS_REFERENCE);
    }

    @Override
    protected void createClass(BaseClass xclass)
    {
        xclass.addTextField(PARAMETER_NAME_PROPERTY, "Parameter name", 30);
        xclass.addTextAreaField(PARAMETER_DESCRIPTION_PROPERTY, "Parameter description", 40, 5);
        xclass.addBooleanField(PARAMETER_MANDATORY_PROPERTY, "Parameter mandatory", PROPERTY_YESNO);
        xclass.addTextField(PARAMETER_DEFAULT_VALUE_PROPERTY, "Parameter default value", 30);
        xclass.addStaticListField(PARAMETER_TYPE_PROPERTY, "Parameter type", 1, false, false,
            StringUtils.join(Arrays.asList(PARAMETER_TYPE_UNKNOWN, PARAMETER_TYPE_WIKI), PROPERTY_PIPE),
            ListClass.DISPLAYTYPE_INPUT, PROPERTY_PIPE, PARAMETER_TYPE_UNKNOWN, ListClass.FREE_TEXT_ALLOWED, true);
        xclass.addTextField(PARAMETER_FEATURE_PROPERTY, "Parameter feature", 30);
        xclass.addStaticListField(PARAMETER_GROUP_PROPERTY, "Parameter group property", 1, true, false, null,
            ListClass.DISPLAYTYPE_INPUT, PROPERTY_PIPE, null, ListClass.FREE_TEXT_ALLOWED, false);
        xclass.addBooleanField(PARAMETER_HIDDEN_PROPERTY, "Parameter hidden", PROPERTY_YESNO);
        xclass.addBooleanField(PARAMETER_ADVANCED_PROPERTY, "Parameter advanced", PROPERTY_YESNO);
        xclass.addBooleanField(PARAMETER_DEPRECATED_PROPERTY, "Parameter deprecated", PROPERTY_YESNO);
        xclass.addBooleanField(PARAMETER_FEATURE_MANDATORY_PROPERTY, "Parameter feature mandatory", PROPERTY_YESNO);
        xclass.addNumberField(PARAMETER_ORDER_PROPERTY, "Parameter order property", 30, NumberClass.TYPE_INTEGER);
    }
}
