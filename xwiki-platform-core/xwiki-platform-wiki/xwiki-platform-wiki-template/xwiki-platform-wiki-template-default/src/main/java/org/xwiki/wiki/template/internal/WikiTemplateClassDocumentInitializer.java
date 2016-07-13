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

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.internal.mandatory.AbstractMandatoryDocumentInitializer;
import com.xpn.xwiki.objects.classes.BaseClass;

/**
 * Update the WikiManagerCode.WikiTemplateClass document with all required information.
 *
 * @since 5.3M2
 * @version $Id$
 */
@Component
@Named("WikiManagerCode.WikiTemplateClass")
@Singleton
public class WikiTemplateClassDocumentInitializer extends AbstractMandatoryDocumentInitializer
{
    /**
     * The name of the mandatory document.
     */
    public static final String DOCUMENT_NAME = "WikiTemplateClass";

    /**
     * The space of the mandatory document.
     */
    public static final String DOCUMENT_SPACE = "WikiManager";

    /**
     * Reference to the server class.
     */
    public static final EntityReference SERVER_CLASS =  new EntityReference(DOCUMENT_NAME, EntityType.DOCUMENT,
            new EntityReference(DOCUMENT_SPACE, EntityType.SPACE));

    /**
     * Name of field <code>iswikitemplate</code> for the XWiki class WikiManagerCode.WikiTemplateClass.
     */
    public static final String FIELD_ISWIKITEMPLATE = "iswikitemplate";

    /**
     * Pretty name of field <code>iswikitemplate</code> for the XWiki class WikiManagerCode.WikiTemplateClass.
     */
    public static final String FIELDPN_ISWIKITEMPLATE = "Template";

    /**
     * Display type of field <code>iswikitemplate</code> for the XWiki class WikiManagerCode.WikiTemplateClass.
     */
    public static final String FIELDDT_ISWIKITEMPLATE = "checkbox";

    /**
     * Default value of field <code>iswikitemplate</code> for the XWiki class WikiManagerCode.WikiTemplateClass.
     */
    public static final Boolean DEFAULT_ISWIKITEMPLATE = Boolean.FALSE;

    /**
     * Constructor.
     */
    public WikiTemplateClassDocumentInitializer()
    {
        // Since we can`t get the main wiki here, this is just to be able to use the Abstract class.
        // getDocumentReference() returns the actual main wiki document reference.
        super(DOCUMENT_SPACE, DOCUMENT_NAME);
    }

    @Override
    protected boolean isMainWikiOnly()
    {
        // The class is used inside wiki descriptors which are located on main wiki
        return true;
    }

    @Override
    public boolean updateDocument(XWikiDocument document)
    {
        boolean needsUpdate = false;

        // Add missing class fields
        BaseClass baseClass = document.getXClass();

        needsUpdate |= baseClass.addBooleanField(FIELD_ISWIKITEMPLATE, FIELDPN_ISWIKITEMPLATE, FIELDDT_ISWIKITEMPLATE);
        needsUpdate |= updateBooleanClassDefaultValue(baseClass, FIELD_ISWIKITEMPLATE, DEFAULT_ISWIKITEMPLATE);

        // Check if the document is hidden
        if (!document.isHidden()) {
            document.setHidden(true);
            needsUpdate = true;
        }

        // Mark this document as Wiki Class.
        if (document.isNew()) {
            needsUpdate |= setClassDocumentFields(document, "Wiki Template Class");
            document.setContent(document.getContent() + "\n\nClass that represents the wiki descriptor property group"
                    + " for the template feature.");
        }

        return needsUpdate;
    }
}
