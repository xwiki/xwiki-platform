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
import javax.inject.Provider;

import org.slf4j.Logger;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
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
public class XWikiAllGroupDocumentInitializer implements MandatoryDocumentInitializer
{
    private static final String DOCUMENT_NAME = "XWikiAllGroup";
    
    @Inject
    private WikiDescriptorManager wikiDescriptorManager;
    
    @Inject
    private Provider<XWikiContext> xcontextProvider;
    
    @Inject
    private Logger logger;
    
    @Override
    public EntityReference getDocumentReference()
    {
        return new EntityReference(DOCUMENT_NAME, EntityType.DOCUMENT,
                new EntityReference(XWiki.SYSTEM_SPACE, EntityType.SPACE));
    }

    @Override
    public boolean updateDocument(XWikiDocument document)
    {
        boolean needsUpdate = false;
        
        // Ensure the document is hidden, like every technical document
        if (!document.isHidden()) {
            document.setHidden(true);
            needsUpdate = true;
        }
        
        // Ensure the document has a XWikiGroups object
        DocumentReference classReference =
                new DocumentReference(wikiDescriptorManager.getCurrentWikiId(), XWiki.SYSTEM_SPACE, "XWikiGroups");
        if (document.getXObject(classReference) == null) {
            try {
                BaseObject obj = document.newXObject(classReference, xcontextProvider.get());
                obj.setStringValue("member", "");
                needsUpdate = true;
            } catch (XWikiException e) {
                logger.error(
                    String.format("Impossible to add an object to the document XWiki.XWikiAllGroups in the wiki [%s].",
                        wikiDescriptorManager.getCurrentWikiId()), e);
            }
        }
        
        return needsUpdate;
    }
}
