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
package org.xwiki.index.tree.internal.nestedpages.pinned;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.localization.ContextualLocalizationManager;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceProvider;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.user.CurrentUserReference;
import org.xwiki.user.UserReferenceResolver;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;

/**
 * Used to set and retrieve the pinned child pages.
 *
 * @version $Id$
 * @since 16.4.0RC1
 */
@Component(roles = PinnedChildPagesManager.class)
@Singleton
public class PinnedChildPagesManager
{
    /**
     * The suffix added to the pinned child page name to indicate that it's a nested page.
     */
    private static final String NESTED_PAGE_MARKER = "/";

    /**
     * The name of the wiki preferences document.
     */
    private static final String WIKI_PREFERENCES = "XWikiPreferences";

    @Inject
    private Logger logger;

    @Inject
    private Provider<XWikiContext> xcontextProvider;

    @Inject
    private EntityReferenceProvider defaultEntityReferenceProvider;

    @Inject
    private ContextualLocalizationManager contextLocalization;

    @Inject
    private UserReferenceResolver<CurrentUserReference> currentUserReferenceResolver;

    /**
     * @param childReference the child document for which to retrieve the parent entity
     * @return the parent entity where the specified child document can be pinned.
     */
    public EntityReference getParent(DocumentReference childReference)
    {
        if (isTerminalPage(childReference)) {
            // For terminal pages the parent is the space containing the page.
            return childReference.getParent();
        } else {
            // For nested pages use the parent space or wiki.
            return childReference.getParent().getParent();
        }
    }

    /**
     * @param parentReference the parent entity for which to retrieve the pinned child pages
     * @return the list of pinned child pages for the specified parent entity
     */
    public List<DocumentReference> getPinnedChildPages(EntityReference parentReference)
    {
        return getPinnedChildPagesStorage(parentReference).map(this::getPinnedChildPages)
            .orElse(Collections.emptyList());
    }

    /**
     * Sets the list of pinned child pages for the specified parent entity.
     *
     * @param parentReference the parent entity for which to set the pinned child pages
     * @param pagesToPin the list of child pages to pin
     */
    public void setPinnedChildPages(EntityReference parentReference, List<DocumentReference> pagesToPin)
    {
        final EntityReference parentEntityReference;
        if (parentReference instanceof DocumentReference) {
            if (parentReference.getName().equals(getDefaultDocumentName())) {
                parentEntityReference = parentReference.getParent();
            } else {
                throw new IllegalArgumentException(String.format("Invalid parent reference [%s], only nested document "
                    + "reference are allowed.", parentReference));
            }
        } else {
            parentEntityReference = parentReference;
        }
        // Make sure we pin only the pages that are children of the specified parent.
        List<DocumentReference> pinnedChildPages = pagesToPin
            .stream()
            .filter(childReference -> {
                if (!Objects.equals(getParent(childReference), parentEntityReference)) {
                    this.logger.warn("Page [{}] is not a child of [{}] so it won't be pinned.", childReference,
                        parentEntityReference);
                    return false;
                } else {
                    return true;
                }
            })
            .toList();
        getPinnedChildPagesStorage(parentReference)
            .ifPresent(storageReference -> setPinnedChildPages(storageReference, pinnedChildPages));
    }

    private boolean isTerminalPage(DocumentReference documentReference)
    {
        return !getDefaultDocumentName().equals(documentReference.getName());
    }

    private String getDefaultDocumentName()
    {
        return this.defaultEntityReferenceProvider.getDefaultReference(EntityType.DOCUMENT).getName();
    }

    private List<DocumentReference> getPinnedChildPages(DocumentReference storageReference)
    {
        try {
            XWikiContext xcontext = this.xcontextProvider.get();
            XWikiDocument storageDocument = xcontext.getWiki().getDocument(storageReference, xcontext);
            DocumentReference classReference = new DocumentReference(PinnedChildPagesClassInitializer.CLASS_REFERENCE,
                storageReference.getWikiReference());
            @SuppressWarnings("unchecked")
            List<String> value =
                storageDocument.getListValue(classReference, PinnedChildPagesClassInitializer.PROPERTY_NAME);
            return resolvePinnedChildPages(storageReference, value);
        } catch (Exception e) {
            this.logger.warn("Failed to get the list of pinned child pages from [{}]. Root cause is [{}].",
                storageReference, ExceptionUtils.getRootCauseMessage(e));
            return Collections.emptyList();
        }
    }

    private void setPinnedChildPages(DocumentReference storageReference, List<DocumentReference> pinnedChildPages)
    {
        try {
            XWikiContext xcontext = this.xcontextProvider.get();
            // Clone the document to avoid modifying the cached instance.
            XWikiDocument storageDocument = xcontext.getWiki().getDocument(storageReference, xcontext).clone();
            if (storageDocument.isNew()) {
                storageDocument.setHidden(true);
            }
            storageDocument.getAuthors().setOriginalMetadataAuthor(
                this.currentUserReferenceResolver.resolve(CurrentUserReference.INSTANCE));
            List<String> value = serializePinnedChildPages(pinnedChildPages);
            storageDocument.setStringListValue(PinnedChildPagesClassInitializer.CLASS_REFERENCE,
                PinnedChildPagesClassInitializer.PROPERTY_NAME, value);
            String saveComment =
                this.contextLocalization.getTranslationPlain("index.tree.pinnedChildPages.saveComment");
            xcontext.getWiki().saveDocument(storageDocument, saveComment, true, xcontext);
        } catch (Exception e) {
            this.logger.warn("Failed to update the list of pinned child pages to [{}]. Root cause is [{}].",
                storageReference, ExceptionUtils.getRootCauseMessage(e));
        }
    }

    private Optional<DocumentReference> getPinnedChildPagesStorage(EntityReference parentReference)
    {
        if (parentReference == null) {
            return Optional.empty();
        } else if (parentReference.getType() == EntityType.WIKI) {
            // Top level pinned pages are stored in the wiki preferences.
            return Optional.of(new DocumentReference(parentReference.getName(), XWiki.SYSTEM_SPACE, WIKI_PREFERENCES));
        } else if (parentReference.getType() == EntityType.SPACE) {
            // Pinned pages for a space are stored in the space preferences.
            return Optional.of(new DocumentReference("WebPreferences", new SpaceReference(parentReference)));
        } else if (parentReference.getType() == EntityType.DOCUMENT
            && getDefaultDocumentName().equals(parentReference.getName())) {
            // Pinned pages for a nested page are stored in the parent space preferences.
            return getPinnedChildPagesStorage(new DocumentReference(parentReference).getLastSpaceReference());
        } else {
            // Unsupported parent type.
            return Optional.empty();
        }
    }

    private List<String> serializePinnedChildPages(List<DocumentReference> pinnedChildPages)
    {
        return pinnedChildPages.stream().map(this::serializePinnedChildPage).toList();
    }

    private String serializePinnedChildPage(DocumentReference pinnedChildPage)
    {
        boolean isTerminalPage = isTerminalPage(pinnedChildPage);
        String name = isTerminalPage ? pinnedChildPage.getName() : pinnedChildPage.getLastSpaceReference().getName();
        // We're going to add a slash at the end of the page name for nested child pages in order to distinguish them
        // from terminal child pages. This means we need to escape the slash that may appear in the page name. We chose
        // to use a partial URL escaping because it's easy to decode.
        name = name.replace("%", "%25").replace(NESTED_PAGE_MARKER, "%2F").replace("+", "%2B");
        if (!isTerminalPage) {
            name += '/';
        }
        return name;
    }

    private List<DocumentReference> resolvePinnedChildPages(DocumentReference storageReference,
        List<String> pinnedChildPages)
    {
        return pinnedChildPages.stream()
            .map(pinnedChildPage -> resolvePinnedChildPage(storageReference, pinnedChildPage))
            .toList();
    }

    private DocumentReference resolvePinnedChildPage(DocumentReference storageReference, String pinnedChildPage)
    {
        EntityReference parentReference = storageReference.getLastSpaceReference();
        DocumentReference wikiPreferencesReference =
            new DocumentReference(storageReference.getWikiReference().getName(), XWiki.SYSTEM_SPACE, WIKI_PREFERENCES);
        if (wikiPreferencesReference.equals(storageReference)) {
            parentReference = storageReference.getWikiReference();
        }

        boolean isTerminal = parentReference instanceof SpaceReference && !pinnedChildPage.endsWith(NESTED_PAGE_MARKER);
        String childPageName =
            URLDecoder.decode(StringUtils.removeEnd(pinnedChildPage, NESTED_PAGE_MARKER), StandardCharsets.UTF_8);
        if (isTerminal) {
            return new DocumentReference(childPageName, (SpaceReference) parentReference);
        } else {
            return new DocumentReference(getDefaultDocumentName(), new SpaceReference(childPageName, parentReference));
        }
    }
}
