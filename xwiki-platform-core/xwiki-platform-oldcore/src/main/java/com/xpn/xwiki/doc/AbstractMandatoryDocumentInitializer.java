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

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.sheet.SheetBinder;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;

import com.xpn.xwiki.user.api.XWikiRightService;

/**
 * Base class for standard mandatory document initializers.
 *
 * @version $Id$
 * @since 9.0RC1
 */
public abstract class AbstractMandatoryDocumentInitializer implements MandatoryDocumentInitializer, Initializable
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

    @Inject
    protected DocumentReferenceResolver<EntityReference> resolver;

    /**
     * @see #getDocumentReference()
     */
    private EntityReference reference;

    private String title;
    
    /**
     * @param reference the reference of the document to update. Can be either local or absolute depending if the
     *            document is associated to a specific wiki or not
     */
    public AbstractMandatoryDocumentInitializer(EntityReference reference)
    {
        this(reference, null);
    }

    /**
     * @param reference the reference of the document to update. Can be either local or absolute depending if the
     *            document is associated to a specific wiki or not
     * @param title the title of the document
     */
    public AbstractMandatoryDocumentInitializer(EntityReference reference, String title)
    {
        this.reference = reference;
        this.title = title;
    }

    @Override
    public void initialize() throws InitializationException
    {
        // If a local reference was specified but isMainWikiOnly() is true, then convert to a main wiki reference.
        if (this.reference != null && this.reference.extractReference(EntityType.WIKI) == null && isMainWikiOnly()) {
            synchronized (this) {
                if (this.reference.extractReference(EntityType.WIKI) == null) {
                    // Convert to main wiki reference
                    EntityReference mainWikiEntityReference = this.resolver.resolve(this.reference,
                        new WikiReference(this.wikiDescriptorManager.getMainWikiId()));

                    this.reference = mainWikiEntityReference;
                }
            }
        }
    }

    @Override
    public EntityReference getDocumentReference()
    {
        return this.reference;
    }

    // Override

    @Override
    public boolean updateDocument(XWikiDocument document)
    {
        return setDocumentFields(document, getTitle());
    }

    protected boolean updateTitle(XWikiDocument document)
    {
        if (StringUtils.isEmpty(document.getTitle())) {
            String returnedTitle = getTitle();

            if (!StringUtils.isEmpty(returnedTitle)) {
                document.setTitle(getTitle());
            }

            return true;
        }

        return false;
    }

    protected String getTitle()
    {
        return this.title;
    }

    /**
     * @return true if the passed reference should be resolved to the main wiki instead of the local one. The default is
     *         {@code false}. This is ignored if the passed reference already contains the wiki information.
     */
    protected boolean isMainWikiOnly()
    {
        return false;
    }

    // Helpers

    /**
     * Set the fields of the document passed as parameter. Can generate content for both XWiki Syntax 1.0 and XWiki
     * Syntax 2.0. If new documents are set to be created in XWiki Syntax 1.0 then generate XWiki 1.0 Syntax otherwise
     * generate XWiki Syntax 2.0.
     *
     * @param document the document
     * @param title the page title to set (if null or blank the title won't be set)
     * @return true if the document has been modified, false otherwise
     */
    protected boolean setDocumentFields(XWikiDocument document, String title)
    {
        boolean needsUpdate = false;

        if (document.getCreatorReference() == null) {
            document.setCreator(XWikiRightService.SUPERADMIN_USER);
            needsUpdate = true;
        }
        if (document.getAuthorReference() == null) {
            document.setAuthorReference(document.getCreatorReference());
            needsUpdate = true;
        }

        if (document.getParentReference() == null) {
            // Use the current document's space homepage and default document name.
            EntityReference spaceReference = getDocumentReference().extractReference(EntityType.SPACE);
            DocumentReference fullReference = this.resolver.resolve(null, spaceReference);
            EntityReference localReference = new LocalDocumentReference(fullReference);
            document.setParentReference(localReference);
            needsUpdate = true;
        }

        if (StringUtils.isNotEmpty(title) && StringUtils.isBlank(document.getTitle())) {
            document.setTitle(title);
            needsUpdate = true;
        }

        if (!document.isHidden()) {
            document.setHidden(true);
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
