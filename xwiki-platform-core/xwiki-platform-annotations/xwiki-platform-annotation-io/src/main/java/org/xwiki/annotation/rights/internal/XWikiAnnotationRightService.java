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
package org.xwiki.annotation.rights.internal;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.annotation.Annotation;
import org.xwiki.annotation.io.IOService;
import org.xwiki.annotation.reference.TypedStringEntityReferenceResolver;
import org.xwiki.annotation.rights.AnnotationRightService;
import org.xwiki.component.annotation.Component;
import org.xwiki.context.Execution;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceSerializer;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;

/**
 * Implementation of the rights service based on the XWiki access rights.
 * 
 * @version $Id$
 * @since 2.3M1
 */
@Component
@Singleton
public class XWikiAnnotationRightService implements AnnotationRightService
{
    /**
     * The execution used to get the deprecated XWikiContext.
     */
    @Inject
    private Execution execution;

    /**
     * Entity reference handler to resolve the reference target. <br />
     * TODO: should be a current reference resolver, to be fully correct, but for the moment it will be a default one,
     * since current resolver does not exist in 2.1.1 and a current typed resolver would fail. Plus, all references
     * passed to this service should be absolute.
     */
    @Inject
    private TypedStringEntityReferenceResolver referenceResolver;

    /**
     * The annotations storage service, used to retrieve information about annotations to check the rights on it.
     */
    @Inject
    private IOService annotationsStorageService;

    /**
     * Entity reference serializer, to create references to the documents to which annotation targets refer.
     */
    @Inject
    private EntityReferenceSerializer<String> serializer;

    /**
     * The logger to log.
     */
    @Inject
    private Logger logger;

    @Override
    public boolean canAddAnnotation(String target, String userName)
    {
        // if the user has comment right on the document represented by the target
        XWikiContext xwikiContext = getXWikiContext();
        try {
            return xwikiContext.getWiki().getRightService().hasAccessLevel("comment", userName,
                getDocumentReference(target), xwikiContext);
        } catch (XWikiException e) {
            logException(e, target, userName);
            return false;
        }
    }

    @Override
    public boolean canEditAnnotation(String annotationId, String target, String userName)
    {
        // if the user has edit right on the document represented by the target, or is the author of the annotation
        XWikiContext xwikiContext = getXWikiContext();
        try {
            boolean hasEditRight =
                xwikiContext.getWiki().getRightService().hasAccessLevel("edit", userName, getDocumentReference(target),
                    xwikiContext);
            if (hasEditRight) {
                return true;
            }
            // check if it's the author of the annotation
            Annotation ann = annotationsStorageService.getAnnotation(target, annotationId);
            return ann != null && ann.getAuthor().equals(userName);
        } catch (Exception e) {
            logException(e, target, userName);
            return false;
        }
    }

    @Override
    public boolean canViewAnnotatedTarget(String target, String userName)
    {
        return canViewAnnotations(target, userName);
    }

    @Override
    public boolean canViewAnnotations(String target, String userName)
    {
        // if user can view the target, it should be able to view annotations on it
        XWikiContext xwikiContext = getXWikiContext();
        try {
            return xwikiContext.getWiki().getRightService().hasAccessLevel("view", userName,
                getDocumentReference(target), xwikiContext);
        } catch (XWikiException e) {
            logException(e, target, userName);
            return false;
        }
    }

    /**
     * Helper method to parse the target as a reference and extract a serialized document reference from it: the
     * document reference serialized if the target can be parsed as a typed reference, or the initial string itself
     * otherwise.
     * 
     * @param target the serialized reference to target to extract the document reference from
     * @return the serialized reference to the document to which the target refers
     */
    private String getDocumentReference(String target)
    {
        EntityReference ref = referenceResolver.resolve(target, EntityType.DOCUMENT);
        // get the document name from the parsed reference
        String docName = target;
        if (ref.getType() == EntityType.DOCUMENT || ref.getType() == EntityType.OBJECT_PROPERTY) {
            docName = serializer.serialize(ref.extractReference(EntityType.DOCUMENT));
        }

        return docName;
    }

    /**
     * Helper method to log an xwiki exception during rights checking process.
     * 
     * @param e exception to log
     * @param target the annotation target for which exception has occurred
     * @param user the user name for which exception occurred on verification
     */
    private void logException(Exception e, String target, String user)
    {
        this.logger.warn("Couldn't get access rights for the target [" + target + "] for user [" + user + "]", e);
    }

    /**
     * Helper function to get the xwiki context from the execution context.
     * 
     * @return the xwiki context
     */
    private XWikiContext getXWikiContext()
    {
        return (XWikiContext) execution.getContext().getProperty("xwikicontext");
    }
}
