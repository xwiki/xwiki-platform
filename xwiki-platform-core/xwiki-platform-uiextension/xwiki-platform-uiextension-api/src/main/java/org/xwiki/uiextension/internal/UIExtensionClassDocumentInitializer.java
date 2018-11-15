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
package org.xwiki.uiextension.internal;

import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;

import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.objects.classes.TextAreaClass.ContentType;

/**
 * Update XWiki.UIExtensionClass document with all required informations.
 *
 * @version $Id$
 * @since 8.1RC1
 */
@Component
@Named(WikiUIExtensionConstants.CLASS_REFERENCE_STRING)
@Singleton
public class UIExtensionClassDocumentInitializer extends AbstractUIExtensionClassDocumentInitializer
    implements WikiUIExtensionConstants
{
    /**
     * Default constructor.
     */
    public UIExtensionClassDocumentInitializer()
    {
        super(WikiUIExtensionConstants.UI_EXTENSION_CLASS);
    }

    @Override
    protected void createClass(BaseClass xclass)
    {
        super.createClass(xclass);

        xclass.addTextField(EXTENSION_POINT_ID_PROPERTY, "Extension Point ID", 30);
        xclass.addTextField(ID_PROPERTY, "Extension ID", 30);
        // The parameters property doesn't support wiki syntax.
        xclass.addTextAreaField(PARAMETERS_PROPERTY, "Extension Parameters", 40, 10, ContentType.PURE_TEXT);
        xclass.addStaticListField(SCOPE_PROPERTY, "Extension Scope", 1, false,
            "wiki=Current Wiki|user=Current User|global=Global", "select");
    }
}
