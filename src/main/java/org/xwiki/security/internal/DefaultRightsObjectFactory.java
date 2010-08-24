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

import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.component.logging.AbstractLogEnabled;

import org.xwiki.security.RightsObject;
import org.xwiki.security.RightsObjectFactory;
import org.xwiki.security.RightServiceException;

import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;

import org.xwiki.context.Execution;

import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.DocumentReference;

import java.util.Collection;
import java.util.List;
import java.util.LinkedList;

/**
 * Implementation of the rights object factory interface.
 * @version $Id$
 */
@Component
public class DefaultRightsObjectFactory extends AbstractLogEnabled implements RightsObjectFactory
{
    /** Resolver for user and group names. */
    @Requirement("user") private DocumentReferenceResolver<String> resolver;

    /** Execution object. */
    @Requirement private Execution execution;

    @Override
    public Collection<RightsObject> getInstances(DocumentReference docRef,  boolean global)
        throws RightServiceException
    {
        XWikiContext context = (XWikiContext) execution.getContext()
            .getProperty(XWikiContext.EXECUTIONCONTEXT_KEY);
        List<BaseObject> baseObjs;
        List<RightsObject> rightsObjs = new LinkedList();

        /*
         * The users and groups listed by the rights object, inherit
         * the wiki from the document, unless explicitly given.
         */
        String wikiName = docRef.getWikiReference().getName();
        String className = global
            ? AbstractRightsObject.GLOBAL_RIGHTS_CLASS
            : AbstractRightsObject.LOCAL_RIGHTS_CLASS;

        DocumentReference classRef = resolver.resolve(className, wikiName);

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
                    getLogger().error("There was a null object!");
                } else {
                    rightsObjs.add(global
                                   ? new GlobalRightsObject(obj, resolver, wikiName)
                                   :  new LocalRightsObject(obj, resolver, wikiName));
                }
            }
        }

        return rightsObjs;
    }
}

