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
package org.xwiki.configuration.internal;

import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;

/**
 * Configuration source taking its data in the User Preferences wiki document (the user profile page) using data from a
 * XWikiUsers object attached to that document.
 * 
 * @version $Id$
 * @since 2.0M2
 */
@Component
@Named("user")
@Singleton
public class UserPreferencesConfigurationSource extends AbstractDocumentConfigurationSource
{
    /**
     * Space name of the user class document.
     */
    private static final String CLASS_SPACE_NAME = "XWiki";

    /**
     * Document name of the user class document.
     */
    private static final String CLASS_PAGE_NAME = "XWikiUsers";

    @Override
    protected DocumentReference getClassReference()
    {
        return new DocumentReference(getCurrentWikiReference().getName(), CLASS_SPACE_NAME, CLASS_PAGE_NAME);
    }

    @Override
    protected DocumentReference getDocumentReference()
    {
        return getDocumentAccessBridge().getCurrentUserReference();
    }
}