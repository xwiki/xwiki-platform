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

import org.xwiki.component.annotation.Role;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.notifications.NotificationException;

import com.xpn.xwiki.objects.BaseObjectReference;

/**
 * Internal role that make requests to the model and avoid a direct dependency to oldcore.
 *
 * @version $Id$
 * @since 9.7RC1
 */
@Role
public interface ModelBridge
{
    /**
     * Save an object's property in an hidden document.
     *
     * @param objectReference reference of the object to save
     * @param property the name of the property to set
     * @param value the value of the property to set
     * @throws NotificationException if error happens
     */
    void savePropertyInHiddenDocument(BaseObjectReference objectReference, String property, Object value)
            throws NotificationException;

    /**
     * Return the URL of the given {@link DocumentReference} for the given action.
     *
     * @param documentReference the reference
     * @param action the request action
     * @param parameters the request parameters
     * @return the URL of the given reference
     */
    String getDocumentURL(DocumentReference documentReference, String action, String parameters);
}
