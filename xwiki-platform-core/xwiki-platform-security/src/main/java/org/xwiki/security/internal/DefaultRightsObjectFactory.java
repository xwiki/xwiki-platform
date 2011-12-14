/*
 * Copyright 2010 Andreas Jonsson
 * 
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
 *
 */
package org.xwiki.security.internal;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.context.Execution;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.security.RightServiceException;
import org.xwiki.security.RightsObject;
import org.xwiki.security.RightsObjectFactory;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

/**
 * Implementation of the rights object factory interface.
 * @version $Id$
 */
@Component
@Singleton
public class DefaultRightsObjectFactory implements RightsObjectFactory
{
    /** Logger. **/
    @Inject
    private Logger logger;
    
    /** Resolver for user and group names. */
    @Inject
    @Named("user")
    private DocumentReferenceResolver<String> resolver;

    /** Execution object. */
    @Inject
    private Execution execution;

    @Override
    public Collection<RightsObject> getInstances(DocumentReference docRef,  boolean global)
        throws RightServiceException
    {
        XWikiContext context = (XWikiContext) execution.getContext()
            .getProperty(XWikiContext.EXECUTIONCONTEXT_KEY);
        List<BaseObject> baseObjs;
        List<RightsObject> rightsObjs = new LinkedList<RightsObject>();

        /*
         * The users and groups listed by the rights object, inherit
         * the wiki from the document, unless explicitly given.
         */
        WikiReference wikiReference = docRef.getWikiReference();
        String className = global
            ? AbstractRightsObject.GLOBAL_RIGHTS_CLASS
            : AbstractRightsObject.LOCAL_RIGHTS_CLASS;

        DocumentReference classRef = resolver.resolve(className, wikiReference);

        try {
            XWikiDocument doc = context.getWiki().getDocument(docRef, context);
            if (doc == null) {
                return rightsObjs;
            }
            baseObjs = doc.getXObjects(classRef);
        } catch (XWikiException e) {
            throw new RightServiceException(e);
        }

        if (baseObjs != null) {
            for (BaseObject obj : baseObjs) {
                if (obj == null) {
                    this.logger.error("There was a null object!");
                } else {
                    rightsObjs.add(global
                                   ? new GlobalRightsObject(obj, resolver, wikiReference)
                                   :  new LocalRightsObject(obj, resolver, wikiReference));
                }
            }
        }

        return rightsObjs;
    }
}

