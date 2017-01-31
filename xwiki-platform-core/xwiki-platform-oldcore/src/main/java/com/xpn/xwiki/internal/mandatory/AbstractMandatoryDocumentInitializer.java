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

import org.xwiki.model.EntityType;
import org.xwiki.model.reference.EntityReference;

import com.xpn.xwiki.doc.AbstractMandatoryClassInitializer;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.objects.classes.BooleanClass;

/**
 * Base class for standard class providers.
 *
 * @version $Id$
 * @since 4.3M1
 * @deprecated since 9.0RC1, use {@link com.xpn.xwiki.doc.AbstractMandatoryDocumentInitializer} instead
 */
@Deprecated
public abstract class AbstractMandatoryDocumentInitializer extends AbstractMandatoryClassInitializer
{
    /**
     * @param reference the reference of the document to update. Can be either local or absolute depending if the
     *            document is associated to a specific wiki or not
     */
    public AbstractMandatoryDocumentInitializer(EntityReference reference)
    {
        super(reference);
    }

    /**
     * @param spaceName the space name of the document
     * @param documentName the document name of the document
     */
    public AbstractMandatoryDocumentInitializer(String spaceName, String documentName)
    {
        this(new EntityReference(documentName, EntityType.DOCUMENT, new EntityReference(spaceName, EntityType.SPACE)));
    }

    /**
     * Set the default value of a boolean field of a XWiki class.
     *
     * @param baseClass the XWiki class.
     * @param fieldName the name of the field.
     * @param value the default value.
     * @return true if <code>baseClass</code> modified.
     */
    protected boolean updateBooleanClassDefaultValue(BaseClass baseClass, String fieldName, Boolean value)
    {
        boolean needsUpdate = false;

        BooleanClass bc = (BooleanClass) baseClass.get(fieldName);

        int old = bc.getDefaultValue();
        int intvalue = intFromBoolean(value);

        if (intvalue != old) {
            bc.setDefaultValue(intvalue);
            needsUpdate = true;
        }

        return needsUpdate;
    }

    /**
     * Set the fields of the class document passed as parameter. Can generate content for both XWiki Syntax 1.0 and
     * XWiki Syntax 2.0. If new documents are set to be created in XWiki Syntax 1.0 then generate XWiki 1.0 Syntax
     * otherwise generate XWiki Syntax 2.0.
     *
     * @param document the document
     * @param title the page title to set
     * @return true if the document has been modified, false otherwise
     */
    protected boolean setClassDocumentFields(XWikiDocument document, String title)
    {
        boolean needsUpdate = updateClassDocumentFields(document);

        needsUpdate |= updateDocumentFields(document, title);

        return needsUpdate;
    }
}
