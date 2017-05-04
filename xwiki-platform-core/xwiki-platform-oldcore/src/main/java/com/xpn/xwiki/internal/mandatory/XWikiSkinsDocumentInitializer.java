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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.sheet.SheetBinder;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.doc.AbstractMandatoryClassInitializer;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.classes.BaseClass;

/**
 * Update XWiki.XWikiSkins document with all required informations.
 *
 * @version $Id$
 * @since 4.3M1
 */
@Component
@Named("XWiki.XWikiSkins")
@Singleton
public class XWikiSkinsDocumentInitializer extends AbstractMandatoryClassInitializer
{
    /**
     * Used to bind a class to a document sheet.
     */
    @Inject
    @Named("class")
    private SheetBinder classSheetBinder;

    /**
     * Default constructor.
     */
    public XWikiSkinsDocumentInitializer()
    {
        super(new LocalDocumentReference(XWiki.SYSTEM_SPACE, "XWikiSkins"));
    }

    @Override
    protected void createClass(BaseClass xclass)
    {
        xclass.addTextField("name", "Name", 30);
        xclass.addTextField("baseskin", "Base Skin", 30);
        xclass.addTextField("logo", "Logo", 30);
        xclass.addStaticListField("outputSyntax", "Output Syntax", "html/5.0=HTML 5|xhtml/1.0=XHTML 1.0");
        xclass.addTemplateField("style.css", "Style");
        xclass.addTemplateField("header.vm", "Header");
        xclass.addTemplateField("footer.vm", "Footer");
        xclass.addTemplateField("viewheader.vm", "View Header");
        xclass.addTemplateField("view.vm", "View");
        xclass.addTemplateField("edit.vm", "Edit");
    }

    @Override
    public boolean updateDocument(XWikiDocument document)
    {
        boolean needsUpdate = super.updateDocument(document);

        // Use XWikiSkinsSheet to display documents having XWikiSkins objects if no other class sheet is specified.
        if (this.classSheetBinder.getSheets(document).isEmpty()) {
            String wikiName = document.getDocumentReference().getWikiReference().getName();
            DocumentReference sheet = new DocumentReference(wikiName, "SkinsCode", "XWikiSkinsSheet");
            needsUpdate |= this.classSheetBinder.bind(document, sheet);
        }

        return needsUpdate;
    }
}
