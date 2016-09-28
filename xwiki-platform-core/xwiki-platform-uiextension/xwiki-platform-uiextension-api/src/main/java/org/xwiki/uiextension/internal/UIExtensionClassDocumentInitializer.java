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
import org.xwiki.rendering.syntax.Syntax;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.internal.mandatory.AbstractMandatoryDocumentInitializer;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.objects.classes.TextAreaClass.ContentType;
import com.xpn.xwiki.objects.classes.TextAreaClass.EditorType;

/**
 * Update XWiki.UIExtensionClass document with all required informations.
 *
 * @version $Id$
 * @since 8.1RC1
 */
@Component
@Named("XWiki.UIExtensionClass")
@Singleton
public class UIExtensionClassDocumentInitializer extends AbstractMandatoryDocumentInitializer
    implements WikiUIExtensionConstants
{
    /**
     * Default constructor.
     */
    public UIExtensionClassDocumentInitializer()
    {
        super(XWiki.SYSTEM_SPACE, "UIExtensionClass");
    }

    @Override
    public boolean updateDocument(XWikiDocument document)
    {
        boolean needsUpdate = false;

        BaseClass bclass = document.getXClass();

        // Force the class document to use the 2.1 syntax default syntax, the same syntax used in the custom displayer.
        if (!Syntax.XWIKI_2_1.equals(document.getSyntax())) {
            document.setSyntax(Syntax.XWIKI_2_1);
            needsUpdate = true;
        }

        needsUpdate |= bclass.addTextField(EXTENSION_POINT_ID_PROPERTY, "Extension Point ID", 30);
        needsUpdate |= bclass.addTextField(ID_PROPERTY, "Extension ID", 30);
        // The content property supports wiki syntax, but it uses script macros most of the time.
        needsUpdate |= bclass.addTextAreaField(CONTENT_PROPERTY, "Extension Content", 40, 10, EditorType.TEXT);
        // The parameters property doesn't support wiki syntax.
        needsUpdate |=
            bclass.addTextAreaField(PARAMETERS_PROPERTY, "Extension Parameters", 40, 10, ContentType.PURE_TEXT);
        needsUpdate |= bclass.addStaticListField(SCOPE_PROPERTY, "Extension Scope", 1, false,
            "wiki=Current Wiki|user=Current User|global=Global", "select");

        needsUpdate |= setClassDocumentFields(document, "UI Extension Class");

        return needsUpdate;
    }
}
