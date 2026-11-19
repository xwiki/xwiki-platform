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
package org.xwiki.pagetemplate.internal;

import java.util.List;
import java.util.function.Predicate;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.localization.LocalizationManager;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.model.reference.SpaceReferenceResolver;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryManager;
import org.xwiki.user.CurrentUserReference;
import org.xwiki.user.UserReferenceResolver;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

import static org.xwiki.pagetemplate.internal.TemplateProviderClassDocumentInitializer.CLASS_REFERENCE;
import static org.xwiki.pagetemplate.internal.TemplateProviderClassDocumentInitializer.CREATION_RESTRICTIONS;
import static org.xwiki.pagetemplate.internal.TemplateProviderClassDocumentInitializer.SPACES;
import static org.xwiki.pagetemplate.internal.TemplateProviderClassDocumentInitializer.VISIBILITY_RESTRICTIONS;

/**
 * Replaces a space in the visibility and creation restrictions of the template providers.
 *
 * @version $Id$
 * @since 19.0.0RC1
 */
@Component(roles = TemplateProviderRestrictionsUpdater.class)
@Singleton
public class TemplateProviderRestrictionsUpdater
{
    private static final List<String> RESTRICTION_PROPERTIES =
        List.of(VISIBILITY_RESTRICTIONS, CREATION_RESTRICTIONS, SPACES);

    private static final String TEMPLATE_PROVIDERS_QUERY =
        "select distinct doc.fullName from Document doc, doc.object(XWiki.TemplateProviderClass) as provider";

    private static final String SAVE_MESSAGE_KEY = "xe.templateprovider.restrictions.renameSaveMessage";

    private static final String CURRENT = "current";

    @Inject
    private Logger logger;

    @Inject
    private Provider<XWikiContext> contextProvider;

    @Inject
    private QueryManager queryManager;

    @Inject
    @Named(CURRENT)
    private DocumentReferenceResolver<String> documentReferenceResolver;

    @Inject
    @Named(CURRENT)
    private SpaceReferenceResolver<String> spaceReferenceResolver;

    @Inject
    @Named("compactwiki")
    private EntityReferenceSerializer<String> compactWikiSerializer;

    @Inject
    private UserReferenceResolver<CurrentUserReference> userReferenceResolver;

    @Inject
    private LocalizationManager localizationManager;

    /**
     * Makes the restrictions targeting a space target another one instead, in the template providers of the wiki
     * holding that space.
     * <p>
     * The restrictions are resolved against the wiki of their template provider, so only the providers of that wiki
     * can target the space.
     *
     * @param oldSpace the space that the restrictions to update target
     * @param newSpace the space that these restrictions should target instead
     * @param canEdit tells whether a template provider can be modified, the others being left unchanged
     * @throws QueryException if the template providers can't be listed
     * @throws XWikiException if a template provider can't be loaded or saved
     */
    public void replaceSpace(SpaceReference oldSpace, SpaceReference newSpace, Predicate<DocumentReference> canEdit)
        throws QueryException, XWikiException
    {
        WikiReference wikiReference = oldSpace.getWikiReference();
        List<String> providerNames = this.queryManager.createQuery(TEMPLATE_PROVIDERS_QUERY, Query.XWQL)
            .setWiki(wikiReference.getName())
            .execute();

        XWikiContext xcontext = this.contextProvider.get();
        for (String providerName : providerNames) {
            DocumentReference providerReference = this.documentReferenceResolver.resolve(providerName, wikiReference);
            if (canEdit.test(providerReference)) {
                XWikiDocument provider = xcontext.getWiki().getDocument(providerReference, xcontext).clone();
                if (replaceSpace(provider, oldSpace, newSpace, xcontext)) {
                    saveDocumentPreservingAuthors(provider, xcontext);
                    this.logger.info("The restrictions of the template provider [{}] targeting [{}] have been "
                        + "updated to target [{}].", providerReference, oldSpace, newSpace);
                }
            }
        }
    }

    private boolean replaceSpace(XWikiDocument provider, SpaceReference oldSpace, SpaceReference newSpace,
        XWikiContext xcontext) throws XWikiException
    {
        boolean modified = false;
        DocumentReference providerReference = provider.getDocumentReference();
        for (BaseObject object : provider.getXObjects(CLASS_REFERENCE)) {
            if (object == null) {
                continue;
            }
            for (String property : RESTRICTION_PROPERTIES) {
                List<String> restrictions = object.getListValue(property);
                List<String> updatedRestrictions = restrictions.stream()
                    .map(restriction -> replaceSpace(restriction, providerReference, oldSpace, newSpace))
                    .toList();
                if (!updatedRestrictions.equals(restrictions)) {
                    // Set through the class so that the value keeps the storage type of the property.
                    object.set(property, updatedRestrictions, xcontext);
                    modified = true;
                }
            }
        }
        return modified;
    }

    private String replaceSpace(String restriction, DocumentReference providerReference, SpaceReference oldSpace,
        SpaceReference newSpace)
    {
        if (StringUtils.isNotBlank(restriction)
            && oldSpace.equals(this.spaceReferenceResolver.resolve(restriction, providerReference)))
        {
            return this.compactWikiSerializer.serialize(newSpace, providerReference);
        }
        return restriction;
    }

    private void saveDocumentPreservingAuthors(XWikiDocument document, XWikiContext xcontext) throws XWikiException
    {
        // Only the xobjects change, so keep the content author, which is the one that matters for scripts.
        document.setContentDirty(false);
        // Make sure the version is incremented.
        document.setMetaDataDirty(true);
        document.getAuthors().setOriginalMetadataAuthor(
            this.userReferenceResolver.resolve(CurrentUserReference.INSTANCE));
        String comment = this.localizationManager.getTranslationPlain(SAVE_MESSAGE_KEY,
            this.localizationManager.getDefaultLocale());
        xcontext.getWiki().saveDocument(document, comment, true, xcontext);
    }
}
