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
package org.xwiki.rest.internal.resources.user;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.xwiki.component.annotation.Component;
import org.xwiki.context.Execution;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.rest.XWikiResource;
import org.xwiki.rest.XWikiRestException;
import org.xwiki.rest.internal.ModelFactory;
import org.xwiki.rest.model.jaxb.Object;
import org.xwiki.rest.model.jaxb.Property;
import org.xwiki.rest.resources.user.CurrentUserPropertyResource;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.security.authorization.Right;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.api.Document;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.classes.PropertyClass;
import com.xpn.xwiki.objects.classes.StaticListClass;

/**
 * Implementation for the REST resource {@link CurrentUserPropertyResource}.
 *
 * @version $Id$
 * @since 9.7RC1
 */
@Component
@Named("org.xwiki.rest.internal.resources.user.CurrentUserPropertyResourceImpl")
public class CurrentUserPropertyResourceImpl extends XWikiResource implements CurrentUserPropertyResource
{
    private static final EntityReference USER_REFERENCE = new EntityReference("XWikiUsers", EntityType.DOCUMENT,
        new EntityReference("XWiki", EntityType.SPACE));

    @Inject
    private ContextualAuthorizationManager authorizationManager;

    @Inject
    private Execution execution;

    @Inject
    private ModelFactory factory;

    @Override
    public Response setNextPropertyValue(String propertyName) throws XWikiRestException
    {
        XWikiContext xcontext = (XWikiContext) this.execution.getContext().getProperty(
            XWikiContext.EXECUTIONCONTEXT_KEY);

        try {
            // Find the current user and get its XWiki.XWikiUsers object number.
            // For Guest users, raise an error
            if (xcontext.getUserReference() == null) {
                throw new XWikiRestException(
                    String.format("Cannot change property [%s] since the current user is guest", propertyName));
            }

            XWikiDocument userDocument = xcontext.getWiki().getDocument(xcontext.getUserReference(), xcontext);

            if (!this.authorizationManager.hasAccess(Right.EDIT, userDocument.getDocumentReference())) {
                throw new WebApplicationException(Status.UNAUTHORIZED);
            }

            BaseObject object = userDocument.getXObject(USER_REFERENCE);
            if (object == null) {
                throw new WebApplicationException(Status.NOT_FOUND);
            }

            // Find the value to replace:
            // - for a boolean type, if true then the new value is false, if false then then new value is true
            // - for a static list type, find the next value in the list. Note that multiselect lists are not handled
            java.lang.Object newValue = computeNewValue(object, propertyName, xcontext);

            if (newValue != null) {
                object.set(propertyName, newValue, xcontext);
                xcontext.getWiki().saveDocument(userDocument, "Setting next value", true, xcontext);
                return buildResponse(userDocument, propertyName, xcontext);
            } else {
                return Response.status(Status.NOT_MODIFIED).build();
            }
        } catch (XWikiException e) {
            throw new XWikiRestException(String.format("Failed to change property [%s] for user [%s]", propertyName,
                xcontext.getUserReference()), e);
        }
    }

    private java.lang.Object computeNewValue(BaseObject object, String propertyName, XWikiContext xcontext)
    {
        java.lang.Object newValue = null;
        PropertyClass propertyClass = (PropertyClass) object.getXClass(xcontext).get(propertyName);
        if (propertyClass.getClassType().equals("Boolean")) {
            // Note: if not defined, then set it to true
            if (object.getIntValue(propertyName) == 1) {
                newValue = 0;
            } else  {
                newValue = 1;
            }
        } else if (propertyClass.getClassType().equals("StaticList")) {
            newValue = computeNewStaticListValue((StaticListClass) propertyClass, object, propertyName, xcontext);
        }
        return newValue;
    }

    private java.lang.Object computeNewStaticListValue(StaticListClass listClass, BaseObject object, String
        propertyName, XWikiContext xcontext)
    {
        java.lang.Object newValue = null;
        if (!listClass.isMultiSelect()) {
            List<String> items = listClass.getList(xcontext);
            int pos = items.indexOf(object.getStringValue(propertyName));

            if (pos != -1) {
                if (pos == items.size() - 1) {
                    newValue = items.get(0);
                } else {
                    newValue = items.get(pos + 1);
                }
            } else {
                // If no item is already selected in the static list, we assume that the default item used is the
                // first in the list.
                if (items.size() <= 2) {
                    newValue = items.get(items.size() - 1);
                } else {
                    newValue = items.get(1);
                }
            }
        }
        return newValue;
    }

    private Response buildResponse(XWikiDocument document, String propertyName, XWikiContext xcontext)
    {
        BaseObject baseObject = document.getXObject(USER_REFERENCE);
        Object object = this.factory.toRestObject(this.uriInfo.getBaseUri(), new Document(document, xcontext),
            baseObject, false, false);

        for (Property p : object.getProperties()) {
            if (p.getName().equals(propertyName)) {
                return Response.status(Status.ACCEPTED).entity(p).build();
            }
        }

        throw new WebApplicationException(Status.INTERNAL_SERVER_ERROR);
    }
}
