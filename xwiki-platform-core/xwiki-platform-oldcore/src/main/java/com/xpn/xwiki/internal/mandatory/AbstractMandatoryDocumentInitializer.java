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

import org.apache.commons.lang3.StringUtils;
import org.xwiki.model.EntityType;
import org.xwiki.model.ModelConfiguration;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.sheet.SheetBinder;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.doc.MandatoryDocumentInitializer;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.objects.classes.BooleanClass;
import com.xpn.xwiki.user.api.XWikiRightService;

/**
 * Base class for standard class providers.
 *
 * @version $Id$
 * @since 4.3M1
 */
public abstract class AbstractMandatoryDocumentInitializer implements MandatoryDocumentInitializer
{
    /**
     * Used to associate a document with a document sheet.
     */
    @Inject
    @Named("document")
    protected SheetBinder documentSheetBinder;

    /**
     * Used to get the main wiki.
     */
    @Inject
    protected WikiDescriptorManager wikiDescriptorManager;

    /**
     * Used to get the default document name.
     */
    @Inject
    protected ModelConfiguration modelConfiguration;

    /**
     * @see #getDocumentReference()
     */
    private EntityReference reference;

    /**
     * @param reference the reference of the document to update. Can be either local or absolute depending if the
     *            document is associated to a specific wiki or not
     */
    public AbstractMandatoryDocumentInitializer(EntityReference reference)
    {
        this.reference = reference;
    }

    /**
     * @param spaceName the space name of the document
     * @param documentName the document name of the document
     */
    public AbstractMandatoryDocumentInitializer(String spaceName, String documentName)
    {
        this(new EntityReference(documentName, EntityType.DOCUMENT, new EntityReference(spaceName, EntityType.SPACE)));
    }

    @Override
    public EntityReference getDocumentReference()
    {
        // If a local reference was specified but isMainWikiOnly() is true, then convert to a main wiki reference.
        if (this.reference != null && this.reference.extractReference(EntityType.WIKI) == null && isMainWikiOnly()) {
            synchronized (this) {
                if (this.reference.extractReference(EntityType.WIKI) == null) {
                    // Extract the information and use it to create a new main wiki document reference.
                    String mainWikiId = this.wikiDescriptorManager.getMainWikiId();
                    String spaceName = this.reference.extractReference(EntityType.SPACE).getName();
                    String documentName = this.reference.extractReference(EntityType.DOCUMENT).getName();

                    EntityReference mainWikiEntityReference =
                        new DocumentReference(mainWikiId, spaceName, documentName);

                    this.reference = mainWikiEntityReference;
                }
            }
        }

        return this.reference;
    }

    /**
     * @return true if the passed reference should be resolved to the main wiki instead of the local one. The default is
     *         {@code false}. This is ignored if the passed reference already contains the wiki information.
     */
    protected boolean isMainWikiOnly()
    {
        return false;
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
        boolean needsUpdate = false;

        // Set the parent since it is different from the current document's space homepage.
        if (document.getParentReference() == null) {
            needsUpdate = true;
            document.setParentReference(new LocalDocumentReference(XWiki.SYSTEM_SPACE, "XWikiClasses"));
        }

        needsUpdate |= setDocumentFields(document, title);

        // Use ClassSheet to display the class document if no other sheet is explicitly specified.
        if (this.documentSheetBinder.getSheets(document).isEmpty()) {
            String wikiName = document.getDocumentReference().getWikiReference().getName();
            DocumentReference sheet = new DocumentReference(wikiName, XWiki.SYSTEM_SPACE, "ClassSheet");
            needsUpdate |= this.documentSheetBinder.bind(document, sheet);
        }

        return needsUpdate;
    }

    /**
     * Set the fields of the document passed as parameter. Can generate content for both XWiki Syntax 1.0 and XWiki
     * Syntax 2.0. If new documents are set to be created in XWiki Syntax 1.0 then generate XWiki 1.0 Syntax otherwise
     * generate XWiki Syntax 2.0.
     *
     * @param document the document
     * @param title the page title to set
     * @return true if the document has been modified, false otherwise
     */
    protected boolean setDocumentFields(XWikiDocument document, String title)
    {
        boolean needsUpdate = false;

        if (document.getCreatorReference() == null) {
            needsUpdate = true;
            document.setCreator(XWikiRightService.SUPERADMIN_USER);
        }
        if (document.getAuthorReference() == null) {
            needsUpdate = true;
            document.setAuthorReference(document.getCreatorReference());
        }

        if (document.getParentReference() == null) {
            needsUpdate = true;
            // Use the current document's space homepage.
            EntityReference spaceReference = getDocumentReference().extractReference(EntityType.SPACE);
            String spaceHomepageDocName = modelConfiguration.getDefaultReferenceValue(EntityType.DOCUMENT);
            EntityReference parentReference =
                new EntityReference(spaceHomepageDocName, EntityType.DOCUMENT, spaceReference);
            document.setParentReference(parentReference);
        }

        if (StringUtils.isBlank(document.getTitle())) {
            needsUpdate = true;
            document.setTitle(title);
        }

        if (!document.isHidden()) {
            needsUpdate = true;
            document.setHidden(true);
        }

        return needsUpdate;
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
     * @param value the {@link Boolean} value to convert.
     * @return the converted <code>int</code> value.
     */
    protected int intFromBoolean(Boolean value)
    {
        return value == null ? -1 : (value.booleanValue() ? 1 : 0);
    }
}
