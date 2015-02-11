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
package com.xpn.xwiki.internal.plugin.rightsmanager;

import org.xwiki.model.reference.DocumentReference;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

/**
 * Extracts user profile data.
 *
 * @param <T> the type of data extracted from the user profile
 * @version $Id$
 * @since 6.4.2, 7.0M2
 */
public interface UserDataExtractor<T>
{
    /**
     * Since the superadmin user is a virtual user, allow handling what to return when extracting data for it.
     *
     * @param reference the reference to the superadmin user
     * @return the extracted data and if null then the iterator will not return it and look for the next non-null value
     */
    T extractFromSuperadmin(DocumentReference reference);

    /**
     * Since the guest user is a virtual user, allow handling what to return when extracting data for it.
     *
     * @param reference the reference to the guest user
     * @return the extracted data and if null then the iterator will not return it and look for the next non-null value
     */
    T extractFromGuest(DocumentReference reference);

    /**
     * @param reference the reference to the user
     * @param document the user document, corresponding to the passed reference
     * @param userObject the XObject representing the user
     * @return the extracted data and if null then the iterator will not return it and look for the next non-null value
     */
    T extract(DocumentReference reference, XWikiDocument document, BaseObject userObject);
}
