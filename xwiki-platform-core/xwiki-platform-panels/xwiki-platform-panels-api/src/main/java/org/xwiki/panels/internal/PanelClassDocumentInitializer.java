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
package org.xwiki.panels.internal;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.sheet.SheetBinder;
import org.xwiki.uiextension.internal.AbstractUIExtensionClassDocumentInitializer;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.objects.classes.ListClass;
import com.xpn.xwiki.objects.classes.TextAreaClass.EditorType;

/**
 * Update XWiki.UIExtensionClass document with all required informations.
 *
 * @version $Id$
 * @since 9.8RC1
 */
@Component
@Named(PanelClassDocumentInitializer.CLASS_REFERENCE_STRING)
@Singleton
public class PanelClassDocumentInitializer extends AbstractUIExtensionClassDocumentInitializer
{
    /**
     * The local reference of the class used to defined panels xobjects as a String.
     */
    public static final String CLASS_REFERENCE_STRING = "Panels.PanelClass";

    /**
     * The name of the space containing the panel class.
     */
    public static final String CLASS_SPACE = "Panels";

    /**
     * The local reference of the class used to defined panels xobjects.
     */
    public static final LocalDocumentReference CLASS_REFERENCE = new LocalDocumentReference(CLASS_SPACE, "PanelClass");

    /**
     * Used to bind a class to a document sheet.
     */
    @Inject
    @Named("class")
    private SheetBinder classSheetBinder;

    /**
     * Default constructor.
     */
    public PanelClassDocumentInitializer()
    {
        super(CLASS_REFERENCE);
    }

    @Override
    protected void createClass(BaseClass xclass)
    {
        xclass.addStaticListField("category", "Category", 1, false, "Information|Navigation|Tools|Administration|Other",
            ListClass.DISPLAYTYPE_SELECT);
        xclass.addTextAreaField("description", "Description", 40, 5, EditorType.TEXT);
        xclass.addTextField("name", "Name", 40);
        xclass.addStaticListField("type", "Panel type", 1, false, "view|edit", ListClass.DISPLAYTYPE_SELECT);

        super.createClass(xclass);
    }

    @Override
    public boolean updateDocument(XWikiDocument document)
    {
        boolean needsUpdate = super.updateDocument(document);

        // Use PanelSheet to display documents having PanelClass objects if no other class sheet is specified.
        if (this.classSheetBinder.getSheets(document).isEmpty()) {
            String wikiName = document.getDocumentReference().getWikiReference().getName();
            DocumentReference sheet = new DocumentReference(wikiName, CLASS_SPACE, "PanelSheet");
            needsUpdate |= this.classSheetBinder.bind(document, sheet);
        }

        return needsUpdate;
    }
}
