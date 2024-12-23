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
package com.xpn.xwiki.doc;

import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.LocalDocumentReference;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.objects.classes.BaseClass;

/**
 * Base class for standard mandatory document initializers.
 *
 * @version $Id$
 * @since 9.0RC1
 */
public abstract class AbstractMandatoryClassInitializer extends AbstractMandatoryDocumentInitializer
{
    private static final LocalDocumentReference CLASSSHEET_REFERENCE =
        new LocalDocumentReference(XWiki.SYSTEM_SPACE, "ClassSheet");

    private static final LocalDocumentReference XWIKICLASSES_REFERENCE =
        new LocalDocumentReference(XWiki.SYSTEM_SPACE, "XWikiClasses");

    /**
     * @param reference the reference of the document to update. Can be either local or absolute, depending on whether
     *                  the document is associated to a specific wiki or not
     */
    public AbstractMandatoryClassInitializer(EntityReference reference)
    {
        super(reference);
    }

    /**
     * @param reference the reference of the document to update. Can be either local or absolute, depending on whether
     *                  the document is associated to a specific wiki or not
     * @param title the title of the document
     */
    public AbstractMandatoryClassInitializer(EntityReference reference, String title)
    {
        super(reference, title);
    }

    @Override
    public boolean updateDocument(XWikiDocument document)
    {
        boolean needUpdate = super.updateDocument(document);

        // Class related document fields
        needUpdate |= updateClassDocumentFields(document);

        // Get document class
        BaseClass currentClass = document.getXClass();

        // Generate the class from scratch
        BaseClass newClass = new BaseClass();
        newClass.setDocumentReference(currentClass.getDocumentReference());
        createClass(newClass);

        // Make sure the current class contains required properties
        needUpdate |= currentClass.apply(newClass, false);

        return needUpdate;
    }

    /**
     * @param xclass the class to create
     * @since 9.0RC1
     */
    protected void createClass(BaseClass xclass)
    {
        // Override
    }

    /**
     * Update document with standard class related properties.
     *
     * @param document the document to update
     * @return true if the document has been modified, false otherwise
     * @since 9.0
     */
    protected boolean updateClassDocumentFields(XWikiDocument document)
    {
        boolean needsUpdate = false;

        // Set the parent since it is different from the current document's space homepage.
        if (document.getParentReference() == null) {
            needsUpdate = true;
            document.setParentReference(XWIKICLASSES_REFERENCE);
        }

        // Set the sheet of the document
        needsUpdate |= updateDocumentSheet(document);

        return needsUpdate;
    }

    /**
     * Update document sheet. By default, set the class sheet.
     * 
     * @param document the document to update
     * @return true if the document has been modified, false otherwise
     * @since 9.0
     */
    protected boolean updateDocumentSheet(XWikiDocument document)
    {
        // Use ClassSheet to display the class document if no other sheet is explicitly specified.
        if (this.documentSheetBinder.getSheets(document).isEmpty()) {
            return this.documentSheetBinder.bind(document, CLASSSHEET_REFERENCE);
        }

        return false;
    }
}
