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
package com.xpn.xwiki.internal.sheet;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.bridge.DocumentModelBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.sheet.internal.ModelBridge;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;

/**
 * Bridge between the sheet module and the old XWiki model.
 *
 * @version $Id$
 * @since 4.1M1
 */
@Component
@Singleton
public class DefaultModelBridge implements ModelBridge
{
    /** Logging helper object. */
    @Inject
    private Logger logger;

    /**
     * The component used to serialize entity references.
     */
    @Inject
    private EntityReferenceSerializer<String> defaultEntityReferenceSerializer;

    /**
     * Used to access the XWiki context.
     */
    @Inject
    private Provider<XWikiContext> xcontextProvider;

    @Override
    public String getDefaultEditMode(DocumentModelBridge document)
    {
        try {
            return ((XWikiDocument) document).getDefaultEditMode(this.xcontextProvider.get());
        } catch (XWikiException e) {
            this.logger.warn("Failed to get the default edit mode for [{}].",
                this.defaultEntityReferenceSerializer.serialize(document.getDocumentReference()));
            return null;
        }
    }

    @Override
    public DocumentModelBridge getDefaultTranslation(DocumentModelBridge document)
    {
        // Check if the given document is a translation (i.e. if it's not the default translation).
        if (((XWikiDocument) document).getTranslation() != 0) {
            try {
                // Load the default document translation.
                XWikiContext xcontext = this.xcontextProvider.get();
                return xcontext.getWiki().getDocument(document.getDocumentReference(), xcontext);
            } catch (XWikiException e) {
                String stringReference =
                    this.defaultEntityReferenceSerializer.serialize(document.getDocumentReference());
                this.logger.warn("Failed to load the default translation of [{}].", stringReference, e);
            }
        }
        return document;
    }

    @Override
    public String getCurrentAction()
    {
        return this.xcontextProvider.get().getAction();
    }

    @Override
    public boolean isCurrentDocument(DocumentModelBridge document)
    {
        if (((XWikiDocument) document).getTranslation() == 0) {
            return document == this.xcontextProvider.get().getDoc();
        } else {
            return document == this.xcontextProvider.get().get("tdoc");
        }
    }

    @Override
    public Map<String, Object> pushDocumentInContext(DocumentModelBridge document)
    {
        Map<String, Object> backupObjects = new HashMap<String, Object>();

        // Backup the current context state.
        XWikiDocument.backupContext(backupObjects, this.xcontextProvider.get());

        // Change the context document, using the XWikiContext from the cloned ExecutionContext.
        ((XWikiDocument) document).setAsContextDoc(this.xcontextProvider.get());

        return backupObjects;
    }

    @Override
    public Set<DocumentReference> getXObjectClassReferences(DocumentModelBridge document)
    {
        return ((XWikiDocument) document).getXObjects().keySet();
    }

    @Override
    public DocumentModelBridge setSecurityDocument(DocumentModelBridge document)
    {
        return (DocumentModelBridge) this.xcontextProvider.get().put(XWikiDocument.CKEY_SDOC, document);
    }
}
