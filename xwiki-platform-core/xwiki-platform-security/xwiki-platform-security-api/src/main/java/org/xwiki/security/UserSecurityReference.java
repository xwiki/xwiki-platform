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
package org.xwiki.security;

import org.xwiki.model.reference.DocumentReference;

/**
 * A user is represented internally in the authorization module by a UserSecurityReference corresponding
 * to the DocumentReference of the user's profile document. By inheritance, the null user is represented by
 * the main wiki reference but with a null original reference.
 *
 * @see SecurityReferenceFactory
 * @version $Id$
 * @since 4.0M2
 */
public class UserSecurityReference extends SecurityReference
{
    /** Serialization identifier. */
    private static final long serialVersionUID = 1L;

    /**
     * @param reference the reference to user.
     * @param mainWiki the reference to the main wiki.
     */
    UserSecurityReference(DocumentReference reference, SecurityReference mainWiki)
    {
        super(reference, mainWiki);
        // TODO: really check that we have a real user document
    }

    @Override
    public DocumentReference getOriginalReference()
    {
        return super.getOriginalDocumentReference();
    }

    /**
     * @return true for global user
     * @since 5.0M2
     */
    public boolean isGlobal()
    {
        DocumentReference ref = this.getOriginalReference();
        return (ref == null) || ref.getWikiReference().equals(mainWikiReference.getOriginalWikiReference());
    }
}
