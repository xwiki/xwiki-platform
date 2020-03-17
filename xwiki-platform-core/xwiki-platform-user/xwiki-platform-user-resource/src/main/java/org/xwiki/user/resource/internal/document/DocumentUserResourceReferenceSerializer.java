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
package org.xwiki.user.resource.internal.document;

import java.net.URL;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.ModelContext;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.resource.CreateResourceReferenceException;
import org.xwiki.resource.ResourceReferenceSerializer;
import org.xwiki.resource.SerializeResourceReferenceException;
import org.xwiki.url.ExtendedURL;
import org.xwiki.user.UserReference;
import org.xwiki.user.internal.document.DocumentUserReference;
import org.xwiki.user.resource.internal.UserResourceReference;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.web.XWikiURLFactory;

/**
 * Converts a {@link UserResourceReference} which contains a {@link DocumentUserReference} into a relative
 * {@link ExtendedURL} (with the Context Path added).
 *
 * @version $Id$
 * @since 12.2RC1
 */
@Component
@Named("document")
@Singleton
public class DocumentUserResourceReferenceSerializer
    implements ResourceReferenceSerializer<UserResourceReference, ExtendedURL>
{
    @Inject
    private Provider<XWikiContext> xwikiContextProvider;

    @Inject
    private ModelContext modelContext;

    @Override
    public ExtendedURL serialize(UserResourceReference resourceReference) throws SerializeResourceReferenceException
    {
        UserReference userReference = resourceReference.getUserReference();
        if (!(userReference instanceof DocumentUserReference)) {
            throw new IllegalArgumentException("The passed user resource reference is not pointing to a wiki Document");
        }
        DocumentReference documentReference = ((DocumentUserReference) userReference).getReference();
        XWikiContext xcontext = this.xwikiContextProvider.get();
        XWikiURLFactory urlFactory = xcontext.getURLFactory();
        URL url = createURL(urlFactory, documentReference, xcontext);
        ExtendedURL extendedURL;
        try {
            extendedURL = new ExtendedURL(url, null);
        } catch (CreateResourceReferenceException e) {
            // This should never happen since new ExtendedURL() is parsing a URL that we've constructed ourselves and
            // thus we control it and it shouldn't lead to any exception. If it happens something is really wrong.
            throw new SerializeResourceReferenceException(
                String.format("Failed to serialize user reference [%s] into a [%s]", documentReference,
                    ExtendedURL.class.getName()), e);
        }
        return extendedURL;
    }

    private URL createURL(XWikiURLFactory urlFactory, DocumentReference documentReference, XWikiContext xcontext)
    {
        URL url;
        EntityReference originalWikiReference = this.modelContext.getCurrentEntityReference();
        try {
            this.modelContext.setCurrentEntityReference(documentReference.getWikiReference());
            url = urlFactory.createURL(documentReference.getLastSpaceReference().getName(),
                documentReference.getName(), xcontext);
        } finally {
            this.modelContext.setCurrentEntityReference(originalWikiReference);
        }
        return url;
    }
}
