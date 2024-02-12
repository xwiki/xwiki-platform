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
package org.xwiki.user.internal.document;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.configuration.internal.AbstractDocumentConfigurationSource;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.LocalDocumentReference;

import com.xpn.xwiki.internal.mandatory.XWikiUsersDocumentInitializer;

/**
 * Configuration source for normal users (i.e. not superadmin, nor guest users) taking its data in the User Preferences
 * wiki document (the user profile page) using data from a XWikiUsers object attached to that document.
 * 
 * @version $Id$
 * @since 2.0M2
 */
@Component
@Named("normaluser")
@Singleton
public class NormalUserPreferencesConfigurationSource extends AbstractDocumentConfigurationSource
{
    @Inject
    private DocumentAccessBridge documentAccessBridge;

    @Override
    protected String getCacheId()
    {
        return "configuration.document.user";
    }

    @Override
    protected LocalDocumentReference getClassReference()
    {
        return XWikiUsersDocumentInitializer.XWIKI_USERS_DOCUMENT_REFERENCE;
    }

    @Override
    protected DocumentReference getDocumentReference()
    {
        // The current user is the user for which we're getting/setting the properties for since that user is put
        // as the current user in DocumentUserProperties before this CS is used.
        return this.documentAccessBridge.getCurrentUserReference();
    }
}
