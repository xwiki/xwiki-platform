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
package org.xwiki.localization.internal;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.LocaleUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.localization.LocalizationContext;
import org.xwiki.localization.LocalizationManager;
import org.xwiki.localization.Translation;
import org.xwiki.localization.rest.TranslationsResource;
import org.xwiki.localization.rest.model.jaxb.ObjectFactory;
import org.xwiki.localization.rest.model.jaxb.Translations;
import org.xwiki.model.ModelContext;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.rest.XWikiRestComponent;

/**
 * Default implementation of {@link TranslationsResource}.
 *
 * @version $Id$
 * @since 13.3RC1
 */
@Component
@Named("org.xwiki.localization.internal.DefaultTranslationsResource")
@Singleton
public class DefaultTranslationsResource implements TranslationsResource, XWikiRestComponent
{
    @Inject
    private LocalizationContext localizationContext;

    @Inject
    private LocalizationManager localizationManager;

    @Inject
    private ModelContext modelContext;

    @Inject
    private Logger logger;

    @Override
    public Translations getTranslations(String wikiId, String localeString, String prefix, List<String> keys)
    {
        // Save the current entity reference. It will be restored on the method is finished.
        EntityReference oldEntityReference = this.modelContext.getCurrentEntityReference();
        try {
            // Set the requested wiki.
            this.modelContext.setCurrentEntityReference(new WikiReference(wikiId));

            // Resolve the locale.
            Locale locale;
            if (localeString != null) {
                locale = LocaleUtils.toLocale(localeString);
            } else {
                locale = this.localizationContext.getCurrentLocale();
            }

            // Computes the prefix.
            String cleanPrefix = Objects.toString(prefix, "");

            ObjectFactory objectFactory = new ObjectFactory();
            Translations result = objectFactory.createTranslations();

            // Resolve the raw translation of the requested keys.
            for (String key : keys) {
                // When no key parameter is passed to the request, the list of keys is initialized with a single null 
                // value. See https://github.com/restlet/restlet-framework-java/issues/922
                if (key != null) {
                    String fullKey = cleanPrefix + key;
                    Translation translation = this.localizationManager.getTranslation(fullKey, locale);
                    if (translation != null) {
                        String rawSource = (String) translation.getRawSource();
                        result.getTranslations().add(createTranslation(objectFactory, fullKey, rawSource));
                    } else {
                        // When a translation key is not found, it is still added to the result object, associated with
                        // a null value.
                        result.getTranslations().add(createTranslation(objectFactory, fullKey, null));
                        this.logger
                            .warn("Translation key [{}] not found for locale [{}] in wiki [{}].", fullKey, locale,
                                wikiId);
                    }
                }
            }

            return result;
        } finally {
            // Restore the old entity reference in the context.
            this.modelContext.setCurrentEntityReference(oldEntityReference);
        }
    }

    private org.xwiki.localization.rest.model.jaxb.Translation createTranslation(ObjectFactory objectFactory,
        String key, String rawSource)
    {
        org.xwiki.localization.rest.model.jaxb.Translation translation = objectFactory.createTranslation();
        translation.setKey(key);
        translation.setRawSource(rawSource);
        return translation;
    }
}
