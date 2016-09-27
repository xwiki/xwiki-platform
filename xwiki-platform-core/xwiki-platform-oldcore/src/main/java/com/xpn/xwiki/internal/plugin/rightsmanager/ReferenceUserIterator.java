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

import java.util.List;

import org.xwiki.context.Execution;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;

import com.xpn.xwiki.XWikiContext;

/**
 * Iterates over all the users found in the passed references (which can be references to groups or to users) and
 * return {@link DocumentReference} for each found user.
 *
 * @version $Id$
 * @since 6.4.2
 * @since 7.0M2
 */
public class ReferenceUserIterator extends UserIterator<DocumentReference>
{
    /**
     * Recommended if this iterator is called from code using components.
     *
     * @param userAndGroupReferences the list of references (users or groups) to iterate over
     * @param excludedUserAndGroupReferences the list of references (users or groups) to exclude. Can be null.
     * @param explicitDocumentReferenceResolver the resolver to use for transforming group member strings into
     *        {@link DocumentReference}
     * @param execution the component used to access the {@link XWikiContext} we use to call oldcore APIs
     */
    public ReferenceUserIterator(List<DocumentReference> userAndGroupReferences,
        List<DocumentReference> excludedUserAndGroupReferences,
        DocumentReferenceResolver<String> explicitDocumentReferenceResolver, Execution execution)
    {
        super(userAndGroupReferences, excludedUserAndGroupReferences, new ReferenceUserDataExtractor(),
            explicitDocumentReferenceResolver, execution);
    }

    /**
     * Recommended if this iterator is called from code using components.
     *
     * @param userOrGroupReference the reference (user or group) to iterate over
     * @param explicitDocumentReferenceResolver the resolver to use for transforming group member strings into
     *        {@link DocumentReference}
     * @param execution the component used to access the {@link XWikiContext} we use to call oldcore APIs
     */
    public ReferenceUserIterator(DocumentReference userOrGroupReference,
        DocumentReferenceResolver<String> explicitDocumentReferenceResolver, Execution execution)
    {
        super(userOrGroupReference, new ReferenceUserDataExtractor(), explicitDocumentReferenceResolver, execution);
    }

    /**
     * Recommended if this iterator is called from old code not using components.
     *
     * @param userAndGroupReferences the list of references (users or groups) to iterate over
     * @param excludedUserAndGroupReferences the list of references (users or groups) to exclude. Can be null.
     * @param explicitDocumentReferenceResolver the resolver to use for transforming group member strings into
     *        {@link DocumentReference}
     * @param xwikiContext the {@link XWikiContext} we use to call oldcore APIs
     */
    public ReferenceUserIterator(List<DocumentReference> userAndGroupReferences,
        List<DocumentReference> excludedUserAndGroupReferences,
        DocumentReferenceResolver<String> explicitDocumentReferenceResolver, XWikiContext xwikiContext)
    {
        super(userAndGroupReferences, excludedUserAndGroupReferences, new ReferenceUserDataExtractor(),
            explicitDocumentReferenceResolver, xwikiContext);
    }

    /**
     * Recommended if this iterator is called from old code not using components.
     *
     * @param userOrGroupReference the reference (user or group) to iterate over
     * @param explicitDocumentReferenceResolver the resolver to use for transforming group member strings into
     *        {@link DocumentReference}
     * @param xwikiContext the {@link XWikiContext} we use to call oldcore APIs
     */
    public ReferenceUserIterator(DocumentReference userOrGroupReference,
        DocumentReferenceResolver<String> explicitDocumentReferenceResolver, XWikiContext xwikiContext)
    {
        super(userOrGroupReference, new ReferenceUserDataExtractor(), explicitDocumentReferenceResolver,
            xwikiContext);
    }
}
