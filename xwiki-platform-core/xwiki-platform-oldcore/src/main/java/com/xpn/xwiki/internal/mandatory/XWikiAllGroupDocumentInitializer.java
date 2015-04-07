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
import javax.inject.Provider;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.MandatoryDocumentInitializer;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

/**
 * Create the mandatory XWikiAllGroup document in the current wiki.
 *  
 * @version $Id$
 * @since 6.4.2 
 */
@Component
@Singleton
@Named("XWiki.XWikiAllGroup")
public class XWikiAllGroupDocumentInitializer implements MandatoryDocumentInitializer
{
    private static final String DOCUMENT_NAME = "XWikiAllGroup";

    private static final String CLASS_NAME = "XWikiGroups";
    
    @Inject
    private WikiDescriptorManager wikiDescriptorManager;
    
    @Inject
    private Provider<XWikiContext> xcontextProvider;
    
    @Inject
    private Logger logger;
    
    @Override
    public EntityReference getDocumentReference()
    {
        return new LocalDocumentReference(XWiki.SYSTEM_SPACE, DOCUMENT_NAME);
    }

    @Override
    public boolean updateDocument(XWikiDocument document)
    {
        boolean needsUpdate = false;

        // Create the reference to the parent of the current document which is also the class of the object to create
        LocalDocumentReference classReference = new LocalDocumentReference(XWiki.SYSTEM_SPACE, CLASS_NAME);

        // Ensure the document has a creator
        if (document.getCreatorReference() == null) {
            document.setCreatorReference(new DocumentReference(wikiDescriptorManager.getMainWikiId(),
                    XWiki.SYSTEM_SPACE, "superadmin"));
            needsUpdate = true;
        }
        
        // Ensure the document has an author
        if (document.getAuthorReference() == null) {
            document.setAuthorReference(document.getCreatorReference());
            needsUpdate = true;
        }

        // Ensure the document has a parent
        if (document.getParentReference() == null) {
            document.setParentReference(classReference);
            needsUpdate = true;
        }

        // Ensure the document is hidden, like every technical document
        if (!document.isHidden()) {
            document.setHidden(true);
            needsUpdate = true;
        }
        
        // Ensure the document has an XWikiGroups object
        if (document.getXObject(classReference) == null) {
            try {
                BaseObject obj = document.newXObject(classReference, xcontextProvider.get());
                obj.setStringValue("member", "");
                needsUpdate = true;
            } catch (XWikiException e) {
                logger.error(
                    String.format("Impossible to add an object to the document XWiki.XWikiAllGroups in the wiki [%s].",
                        document.getDocumentReference().getWikiReference().getName()), e);
            }
        }
        
        return needsUpdate;
    }
}
