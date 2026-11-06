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
import java.util.Optional;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.model.reference.SpaceReferenceResolver;
import org.xwiki.pagetemplate.TemplateProvider;
import org.xwiki.pagetemplate.TemplateProviderException;
import org.xwiki.pagetemplate.TemplateProviderManager;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

import static org.xwiki.pagetemplate.internal.TemplateProviderClassDocumentInitializer.ACTION;
import static org.xwiki.pagetemplate.internal.TemplateProviderClassDocumentInitializer.CLASS_REFERENCE;
import static org.xwiki.pagetemplate.internal.TemplateProviderClassDocumentInitializer.CREATION_RESTRICTIONS;
import static org.xwiki.pagetemplate.internal.TemplateProviderClassDocumentInitializer.CREATION_RESTRICTIONS_ARE_SUGGESTIONS;
import static org.xwiki.pagetemplate.internal.TemplateProviderClassDocumentInitializer.DESCRIPTION;
import static org.xwiki.pagetemplate.internal.TemplateProviderClassDocumentInitializer.ICON;
import static org.xwiki.pagetemplate.internal.TemplateProviderClassDocumentInitializer.NAME;
import static org.xwiki.pagetemplate.internal.TemplateProviderClassDocumentInitializer.SPACES;
import static org.xwiki.pagetemplate.internal.TemplateProviderClassDocumentInitializer.TEMPLATE;
import static org.xwiki.pagetemplate.internal.TemplateProviderClassDocumentInitializer.TERMINAL;
import static org.xwiki.pagetemplate.internal.TemplateProviderClassDocumentInitializer.VISIBILITY_RESTRICTIONS;

/**
 * Default implementation of {@link TemplateProviderManager}, reading {@code XWiki.TemplateProviderClass} objects.
 *
 * @version $Id$
 * @since 18.10.0RC1
 */
@Component
@Singleton
public class DefaultTemplateProviderManager implements TemplateProviderManager
{
    /**
     * Legacy property deciding whether the created pages are terminal, used when {@code terminal} is not set.
     */
    private static final String TYPE = "type";

    private static final String TYPE_SPACE = "space";

    private static final String CURRENT = "current";

    @Inject
    private Provider<XWikiContext> contextProvider;

    @Inject
    @Named(CURRENT)
    private DocumentReferenceResolver<String> documentReferenceResolver;

    @Inject
    @Named(CURRENT)
    private SpaceReferenceResolver<String> spaceReferenceResolver;

    @Override
    public Optional<TemplateProvider> getTemplateProvider(DocumentReference reference)
        throws TemplateProviderException
    {
        XWikiContext xcontext = this.contextProvider.get();
        XWikiDocument document;
        try {
            document = xcontext.getWiki().getDocument(reference, xcontext);
        } catch (XWikiException e) {
            throw new TemplateProviderException(String.format("Failed to load template provider [%s].", reference),
                e);
        }

        BaseObject object = document.getXObject(CLASS_REFERENCE);
        if (object == null) {
            return Optional.empty();
        }

        TemplateProvider templateProvider = new TemplateProvider(reference);
        templateProvider.setName(getString(object, NAME));
        templateProvider.setDescription(getString(object, DESCRIPTION));
        templateProvider.setIcon(getString(object, ICON));
        templateProvider.setAction(getString(object, ACTION));
        String template = object.getStringValue(TEMPLATE);
        if (StringUtils.isNotBlank(template)) {
            templateProvider.setTemplateReference(this.documentReferenceResolver.resolve(template, reference));
        }
        templateProvider.setTerminal(isTerminal(object));
        templateProvider.setVisibilityRestrictions(getRestrictions(object, VISIBILITY_RESTRICTIONS, reference));
        templateProvider.setCreationRestrictions(getRestrictions(object, CREATION_RESTRICTIONS, reference));
        templateProvider.setCreationRestrictionsAreSuggestions(
            object.getIntValue(CREATION_RESTRICTIONS_ARE_SUGGESTIONS, 0) == 1);

        return Optional.of(templateProvider);
    }

    private String getString(BaseObject object, String property)
    {
        return StringUtils.defaultIfEmpty(object.getStringValue(property), null);
    }

    private boolean isTerminal(BaseObject object)
    {
        int terminal = object.getIntValue(TERMINAL, -1);
        if (terminal == -1) {
            // Providers created before the terminal property relied on the type property, where only "space" meant
            // nested pages.
            return !TYPE_SPACE.equals(object.getStringValue(TYPE));
        }
        return terminal == 1;
    }

    private List<SpaceReference> getRestrictions(BaseObject object, String property, DocumentReference reference)
    {
        List<String> restrictions = object.getListValue(property);
        if (restrictions.isEmpty()) {
            // Providers created before 8.3M2 used a single property for both visibility and creation restrictions.
            restrictions = object.getListValue(SPACES);
        }
        return restrictions.stream()
            .filter(StringUtils::isNotBlank)
            .map(restriction -> this.spaceReferenceResolver.resolve(restriction, reference))
            .toList();
    }
}
