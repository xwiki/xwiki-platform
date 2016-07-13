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
package com.xpn.xwiki.script.parentchild;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.script.service.ScriptService;
import org.xwiki.stability.Unstable;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;

/**
 * This script service give an access to the configuration of the parent/child mechanism, which has been deprecated
 * but can still be enabled for retro-compatibility issues. 
 *  
 * @version $Id$
 * @since 7.2M2 
 */
@Component
@Named("parentchild")
@Singleton
@Unstable
public class ParentChildScriptService implements ScriptService
{
    @Inject
    private ConfigurationSource configurationSource;
    
    @Inject
    private DocumentReferenceResolver<EntityReference> documentReferenceResolver;
    
    @Inject
    private Provider<XWikiContext> contextProvider;
    
    @Inject
    private Logger logger;

    /** 
     * @return whether or not legacy parent/child mechanism is enabled for the hierarchy handling
     */
    public boolean isParentChildMechanismEnabled()
    {
        return "parentchild".equals(
                configurationSource.getProperty("core.hierarchyMode", "reference"));
    }

    /**
     * @param docRef a reference of the document
     * @return the list of parents of the document
     */
    public List<DocumentReference> getParents(DocumentReference docRef)
    {
        if (isParentChildMechanismEnabled()) {
            return getParentsBasedOnParentChildRelationship(docRef);
        } else {
            return getParentsBasedOnReference(docRef);
        }
    }

    /**
     * Returns the parents of a document, based on the nested document paradigm (described in the document reference).
     * @param docRef a reference of the document
     * @return the list of parents of the document
     */
    public List<DocumentReference> getParentsBasedOnReference(DocumentReference docRef)
    {
        List<DocumentReference> parents = new ArrayList<>();
        
        for (SpaceReference spaceReference : docRef.getSpaceReferences()) {
            parents.add(documentReferenceResolver.resolve(spaceReference));
        }
        
        return parents;
    }

    /**
     * Returns the parents of a document, based on the parent/child relationship.
     * @param docRef a reference of the document
     * @return the list of parents of the document
     */
    public List<DocumentReference> getParentsBasedOnParentChildRelationship(DocumentReference docRef)
    {
        XWikiContext context = contextProvider.get();
        XWiki xwiki = context.getWiki();

        List<DocumentReference> parents = new ArrayList<>();
        try {
            XWikiDocument document = xwiki.getDocument(docRef, context);
            while (document.getParentReference() != null) {
                DocumentReference parentReference = document.getParentReference();
                if (parents.contains(parentReference)) {
                    throw new Exception("Cyclic references of parent documents");
                }
                parents.add(document.getParentReference());
                document = xwiki.getDocument(document.getParentReference(), context);
            }
        } catch (Exception e) {
            logger.error("Failed to get the parents of [{}].", docRef, e);
        }

        Collections.reverse(parents);

        return parents;
    }
}
