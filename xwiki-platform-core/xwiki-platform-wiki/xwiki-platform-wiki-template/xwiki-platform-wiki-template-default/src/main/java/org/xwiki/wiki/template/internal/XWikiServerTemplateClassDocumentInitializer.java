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
package org.xwiki.wiki.template.internal;

import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.EntityReference;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.internal.mandatory.AbstractMandatoryDocumentInitializer;
import com.xpn.xwiki.objects.classes.BaseClass;

/**
 * Update the XWiki.XWikiServerClass document with all required information.
 *
 * @since 5.3M2
 */
@Component
@Named("XWiki.XWikiServerTemplateClass")
@Singleton
public class XWikiServerTemplateClassDocumentInitializer extends AbstractMandatoryDocumentInitializer
{
    /**
     * The name of the mandatory document.
     */
    public static final String DOCUMENT_NAME = "XWikiServerTemplateClass";

    /**
     * Reference to the server class.
     */
    public static final EntityReference SERVER_CLASS =  new EntityReference(DOCUMENT_NAME, EntityType.DOCUMENT,
            new EntityReference(XWiki.SYSTEM_SPACE, EntityType.SPACE));

    /**
     * Name of field <code>iswikitemplate</code> for the XWiki class XWiki.XWikiServerClass.
     */
    public static final String FIELD_ISWIKITEMPLATE = "iswikitemplate";

    /**
     * Pretty name of field <code>iswikitemplate</code> for the XWiki class XWiki.XWikiServerClass.
     */
    public static final String FIELDPN_ISWIKITEMPLATE = "Template";

    /**
     * Display type of field <code>iswikitemplate</code> for the XWiki class XWiki.XWikiServerClass.
     */
    public static final String FIELDDT_ISWIKITEMPLATE = "checkbox";

    /**
     * Default value of field <code>iswikitemplate</code> for the XWiki class XWiki.XWikiServerClass.
     */
    public static final Boolean DEFAULT_ISWIKITEMPLATE = Boolean.FALSE;

    public XWikiServerTemplateClassDocumentInitializer()
    {
        // Since we can`t get the main wiki here, this is just to be able to use the Abstract class.
        // getDocumentReference() returns the actual main wiki document reference.
        super(XWiki.SYSTEM_SPACE, DOCUMENT_NAME);
    }

    @Override
    public boolean updateDocument(XWikiDocument document)
    {
        boolean needsUpdate = false;

        // Add missing class fields
        BaseClass baseClass = document.getXClass();

        needsUpdate |= baseClass.addBooleanField(FIELD_ISWIKITEMPLATE, FIELDPN_ISWIKITEMPLATE, FIELDDT_ISWIKITEMPLATE);
        needsUpdate |= updateBooleanClassDefaultValue(baseClass, FIELD_ISWIKITEMPLATE, DEFAULT_ISWIKITEMPLATE);

        return needsUpdate;
    }
}
