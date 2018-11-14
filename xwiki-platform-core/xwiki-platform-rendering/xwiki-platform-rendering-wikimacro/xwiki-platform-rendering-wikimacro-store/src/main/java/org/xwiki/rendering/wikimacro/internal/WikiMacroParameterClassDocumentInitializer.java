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

import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.LocalDocumentReference;

import com.xpn.xwiki.doc.AbstractMandatoryClassInitializer;
import com.xpn.xwiki.objects.classes.BaseClass;

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
    /**
     * Default constructor.
     */
    public WikiMacroParameterClassDocumentInitializer()
    {
        super(new LocalDocumentReference(WIKI_MACRO_PARAMETER_CLASS_SPACE, WIKI_MACRO_PARAMETER_CLASS_PAGE));
    }

    @Override
    protected void createClass(BaseClass xclass)
    {
        xclass.addTextField(PARAMETER_NAME_PROPERTY, "Parameter name", 30);
        xclass.addTextAreaField(PARAMETER_DESCRIPTION_PROPERTY, "Parameter description", 40, 5);
        xclass.addBooleanField(PARAMETER_MANDATORY_PROPERTY, "Parameter mandatory", "yesno");
        xclass.addTextField(PARAMETER_DEFAULT_VALUE_PROPERTY, "Parameter default value", 30);
        xclass.addTextField(PARAMETER_TYPE_PROPERTY, "Parameter type", 30);
    }
}
