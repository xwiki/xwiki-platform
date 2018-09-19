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

import java.net.URI;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response.Status;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.ClassPropertyReference;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.rest.Relations;
import org.xwiki.rest.XWikiResource;
import org.xwiki.rest.XWikiRestException;
import org.xwiki.rest.internal.Utils;
import org.xwiki.rest.model.jaxb.Link;
import org.xwiki.rest.model.jaxb.PropertyValues;
import org.xwiki.rest.resources.classes.ClassPropertyResource;
import org.xwiki.rest.resources.classes.ClassPropertyValuesProvider;
import org.xwiki.rest.resources.classes.ClassPropertyValuesResource;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.security.authorization.Right;

import com.xpn.xwiki.XWikiException;

/**
 * Implements {@link ClassPropertyValuesResource}.
 * 
 * @version $Id$
 * @since 9.8RC1
 */
@Component
@Named("org.xwiki.rest.internal.resources.classes.ClassPropertyValuesResourceImpl")
public class ClassPropertyValuesResourceImpl extends XWikiResource implements ClassPropertyValuesResource
{
    @Inject
    private DocumentReferenceResolver<String> resolver;

    @Inject
    private ContextualAuthorizationManager authorization;

    @Inject
    private ClassPropertyValuesProvider propertyValuesProvider;

    @Override
    public PropertyValues getClassPropertyValues(String wikiName, String className, String propertyName, Integer limit,
        List<String> filterParameters, Boolean isExactMatch) throws XWikiRestException
    {
        DocumentReference classReference = this.resolver.resolve(className, new WikiReference(wikiName));
        ClassPropertyReference classPropertyReference = new ClassPropertyReference(propertyName, classReference);
        if (!this.authorization.hasAccess(Right.VIEW, classPropertyReference)) {
            throw new WebApplicationException(Status.UNAUTHORIZED);
        }
        if (!exists(classPropertyReference)) {
            throw new WebApplicationException(Status.NOT_FOUND);
        }

        URI propertyURI =
            Utils.createURI(this.uriInfo.getBaseUri(), ClassPropertyResource.class, wikiName, className, propertyName);
        Link propertyLink = this.objectFactory.createLink();
        propertyLink.setHref(propertyURI.toString());
        propertyLink.setRel(Relations.PROPERTY);

        PropertyValues propertyValues;
        if (isExactMatch) {
            propertyValues = this.propertyValuesProvider.getValue(classPropertyReference, filterParameters.toArray());
        } else {
            propertyValues = this.propertyValuesProvider
                .getValues(classPropertyReference, limit, filterParameters.toArray());
        }
        propertyValues.getLinks().add(propertyLink);

        return propertyValues;
    }

    private boolean exists(ClassPropertyReference classPropertyReference) throws XWikiRestException
    {
        try {
            return getXWikiContext().getWiki().getDocument(classPropertyReference, getXWikiContext()).getXClass()
                .get(classPropertyReference.getName()) != null;
        } catch (XWikiException e) {
            throw new XWikiRestException(e);
        }
    }
}
