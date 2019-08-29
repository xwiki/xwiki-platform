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
package org.xwiki.rest.internal.resources.classes;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response.Status;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.rest.XWikiResource;
import org.xwiki.rest.XWikiRestException;
import org.xwiki.rest.internal.ModelFactory;
import org.xwiki.rest.internal.Utils;
import org.xwiki.rest.model.jaxb.Class;
import org.xwiki.rest.resources.classes.ClassResource;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.security.authorization.Right;

import com.xpn.xwiki.XWikiException;

/**
 * @version $Id$
 */
@Component
@Named("org.xwiki.rest.internal.resources.classes.ClassResourceImpl")
public class ClassResourceImpl extends XWikiResource implements ClassResource
{
    @Inject
    private ModelFactory utils;

    @Inject
    private ContextualAuthorizationManager authorization;

    @Inject
    private DocumentReferenceResolver<String> resolver;

    @Override
    public Class getClass(String wikiName, String className) throws XWikiRestException
    {
        String database = Utils.getXWikiContext(componentManager).getWikiId();

        try {
            Utils.getXWikiContext(componentManager).setWikiId(wikiName);

            DocumentReference classReference = this.resolver.resolve(className, new WikiReference(wikiName));
            if (!this.authorization.hasAccess(Right.VIEW, classReference)) {
                throw new WebApplicationException(Status.UNAUTHORIZED);
            }
            com.xpn.xwiki.api.Class xwikiClass = Utils.getXWikiApi(componentManager).getClass(className);
            if (xwikiClass == null) {
                throw new WebApplicationException(Status.NOT_FOUND);
            }

            return this.utils.toRestClass(uriInfo.getBaseUri(), xwikiClass);
        } catch (XWikiException e) {
            throw new XWikiRestException(e);
        } finally {
            Utils.getXWikiContext(componentManager).setWikiId(database);
        }
    }
}
