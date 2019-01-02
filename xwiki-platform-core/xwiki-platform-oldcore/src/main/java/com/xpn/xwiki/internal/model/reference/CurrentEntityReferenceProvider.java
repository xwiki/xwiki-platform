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
package com.xpn.xwiki.internal.model.reference;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.EntityType;
import org.xwiki.model.internal.reference.DefaultEntityReferenceProvider;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.PageReference;
import org.xwiki.model.reference.SpaceReference;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;

/**
 * The behavior is the following:
 * <ul>
 * <li>The wiki value used is the current wiki if no wiki was specified in the passed reference (or if it was empty).
 * Note that this is different from using the current document's wiki value.</li>
 * <li>The space value used is the space from the current document reference if no space was specified in the passed
 * reference (or if it was empty). If the current document reference is not defined then the default space value is used
 * instead.</li>
 * <li>The page value used is the page from the current document reference if no page was specified in the passed
 * reference (or if it was empty). If the current document reference is not defined then the default page value is used
 * instead.</li>
 * </ul>
 *
 * @version $Id$
 * @since 7.2M1
 */
@Component
@Named("current")
@Singleton
public class CurrentEntityReferenceProvider extends DefaultEntityReferenceProvider
{
    @Inject
    @Named("readonly")
    private Provider<XWikiContext> xcontextProvider;

    @Override
    public EntityReference getDefaultReference(EntityType type)
    {
        EntityReference result = null;

        XWikiContext xcontext = this.xcontextProvider.get();
        if (xcontext != null) {
            if (type == EntityType.WIKI) {
                result = xcontext.getWikiReference();
            } else if (type == EntityType.SPACE) {
                XWikiDocument currentDoc = xcontext.getDoc();
                if (currentDoc != null) {
                    SpaceReference spaceReference = currentDoc.getDocumentReference().getLastSpaceReference();
                    // Keep only the spaces part
                    result = spaceReference.removeParent(spaceReference.getWikiReference());
                }
            } else if (type == EntityType.DOCUMENT) {
                XWikiDocument currentDoc = xcontext.getDoc();
                if (currentDoc != null) {
                    DocumentReference documentReference = currentDoc.getDocumentReference();
                    // Keep only the document part
                    result = documentReference.removeParent(documentReference.getLastSpaceReference());
                }
            } else if (type == EntityType.PAGE) {
                XWikiDocument currentDoc = xcontext.getDoc();
                if (currentDoc != null) {
                    PageReference pageReference = currentDoc.getPageReference();
                    // Keep only the page part
                    result = pageReference.removeParent(pageReference.getWikiReference());
                }
            }
        }

        if (result == null) {
            result = super.getDefaultReference(type);
        }

        return result;
    }
}
