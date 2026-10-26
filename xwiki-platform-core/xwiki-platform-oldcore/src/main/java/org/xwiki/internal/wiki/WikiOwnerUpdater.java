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
package org.xwiki.internal.wiki;

import java.util.Collection;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.user.CurrentUserReference;
import org.xwiki.user.UserReferenceResolver;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;
import org.xwiki.wiki.manager.WikiManagerException;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

/**
 * Update the owner of the wikis owned by a user or group which is renamed or deleted.
 *
 * @version $Id$
 * @since 16.10.20
 * @since 17.10.16
 * @since 18.4.9
 * @since 18.10.0RC1
 */
@Component(roles = WikiOwnerUpdater.class)
@Singleton
public class WikiOwnerUpdater
{
    private static final LocalDocumentReference WIKI_DESCRIPTOR_CLASS_REFERENCE =
        new LocalDocumentReference(XWiki.SYSTEM_SPACE, "XWikiServerClass");

    private static final String OWNER_FIELD = "owner";

    @Inject
    private Provider<XWikiContext> contextProvider;

    @Inject
    private WikiDescriptorManager wikiDescriptorManager;

    @Inject
    @Named("explicit")
    private DocumentReferenceResolver<String> explicitDocumentReferenceResolver;

    @Inject
    @Named("user")
    private DocumentReferenceResolver<String> userAndGroupReferenceResolver;

    @Inject
    private EntityReferenceSerializer<String> serializer;

    @Inject
    private UserReferenceResolver<CurrentUserReference> currentUserReferenceResolver;

    @Inject
    private Logger logger;

    /**
     * Replace the owner of every wiki owned by the provided user or group. A failure to update a wiki is logged, and
     * the other wikis are still updated.
     *
     * @param owner the reference of the user or group which is renamed or deleted
     * @param newOwner the new reference of the user or group, or {@code null} to remove the owner
     */
    public void replaceOwner(DocumentReference owner, DocumentReference newOwner)
    {
        Collection<String> wikiIds;
        try {
            wikiIds = this.wikiDescriptorManager.getAllIds();
        } catch (WikiManagerException e) {
            this.logger.warn("Failed to get the wikis to update the owner [{}].", owner, e);
            return;
        }

        XWikiContext context = this.contextProvider.get();
        for (String wikiId : wikiIds) {
            try {
                replaceOwner(wikiId, owner, newOwner, context);
            } catch (XWikiException e) {
                this.logger.warn("Failed to update the owner [{}] of the wiki [{}].", owner, wikiId, e);
            }
        }
    }

    private void replaceOwner(String wikiId, DocumentReference owner, DocumentReference newOwner,
        XWikiContext context) throws XWikiException
    {
        // The wiki descriptors are stored in the main wiki, and an owner without wiki is a main wiki user (see
        // XWiki#getWikiOwner).
        WikiReference mainWikiReference = new WikiReference(context.getMainXWiki());
        XWikiDocument descriptorDocument = context.getWiki().getDocument(
            this.explicitDocumentReferenceResolver.resolve(XWiki.getServerWikiPage(wikiId), mainWikiReference),
            context);
        BaseObject descriptorObject = descriptorDocument.getXObject(WIKI_DESCRIPTOR_CLASS_REFERENCE);
        if (descriptorObject == null) {
            return;
        }

        String currentOwner = descriptorObject.getStringValue(OWNER_FIELD);
        if (StringUtils.isNotBlank(currentOwner)
            && owner.equals(this.userAndGroupReferenceResolver.resolve(currentOwner, mainWikiReference))) {
            // Avoid modifying the cached document
            XWikiDocument clonedDocument = descriptorDocument.clone();
            clonedDocument.getXObject(WIKI_DESCRIPTOR_CLASS_REFERENCE).setStringValue(OWNER_FIELD,
                newOwner != null ? this.serializer.serialize(newOwner) : "");
            clonedDocument.getAuthors()
                .setOriginalMetadataAuthor(this.currentUserReferenceResolver.resolve(CurrentUserReference.INSTANCE));
            context.getWiki().saveDocument(clonedDocument, "Updated the owner", true, context);
        }
    }
}
