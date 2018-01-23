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
package org.xwiki.notifications.notifiers.internal;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.notifications.NotificationException;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.BaseObjectReference;

/**
 * This is the default implementation of {@link ModelBridge}.
 *
 * @version $Id$
 * @since 9.7RC1
 */
@Component
@Singleton
public class DefaultModelBridge implements ModelBridge
{
    @Inject
    private Provider<XWikiContext> contextProvider;

    @Inject
    private EntityReferenceSerializer<String> entityReferenceSerializer;

    @Override
    public void savePropertyInHiddenDocument(BaseObjectReference objectReference, String property, Object value)
            throws NotificationException
    {
        try {
            XWikiContext xcontext = contextProvider.get();
            DocumentReference documentReference = (DocumentReference) objectReference.getParent();
            XWikiDocument doc = xcontext.getWiki().getDocument(documentReference, xcontext);
            doc.setHidden(true);
            BaseObject obj = doc.getObject(entityReferenceSerializer.serialize(objectReference.getXClassReference()),
                    true, xcontext);
            if (obj != null) {
                // Set the value
                obj.set(property, value, xcontext);

                // Prevent version changes
                doc.setMetaDataDirty(false);
                doc.setContentDirty(false);

                // Save
                xcontext.getWiki().saveDocument(doc, String.format("Property [%s] set.", property), xcontext);
            }
        } catch (XWikiException e) {
            throw new NotificationException(String.format("Failed to update the object [%s].", objectReference), e);
        }
    }

    @Override
    public String getDocumentURL(DocumentReference documentReference, String action, String parameters) {
        XWikiContext context = contextProvider.get();
        return context.getWiki().getExternalURL(documentReference, action, parameters, null, context);
    }
}
