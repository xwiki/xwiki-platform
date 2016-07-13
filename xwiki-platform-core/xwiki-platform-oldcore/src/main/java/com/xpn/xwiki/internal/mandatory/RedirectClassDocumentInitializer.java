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
package com.xpn.xwiki.internal.mandatory;

import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.rendering.syntax.Syntax;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.classes.BaseClass;

/**
 * Update XWiki.Redirect document with all required informations.
 *
 * @version $Id$
 * @since 8.1RC1
 */
@Component
@Named("XWiki.RedirectClass")
@Singleton
public class RedirectClassDocumentInitializer extends AbstractMandatoryDocumentInitializer
{
    /**
     * The local reference of the redirect class.
     */
    public static final LocalDocumentReference REFERENCE =
        new LocalDocumentReference(XWiki.SYSTEM_SPACE, "RedirectClass");

    /**
     * Default constructor.
     */
    public RedirectClassDocumentInitializer()
    {
        super(REFERENCE);
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

        needsUpdate |= bclass.addTextField("location", "Location", 30);

        needsUpdate |= setClassDocumentFields(document, "Redirect Class");

        return needsUpdate;
    }
}
