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
import javax.inject.Provider;
import javax.inject.Singleton;
import javax.ws.rs.core.Response;

import org.apache.commons.lang3.LocaleUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.localization.LocalizationContext;
import org.xwiki.localization.LocalizationManager;
import org.xwiki.localization.Translation;
import org.xwiki.localization.rest.LocalizationSource;
import org.xwiki.rest.XWikiRestComponent;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.xpn.xwiki.XWikiContext;

/**
 * Default implementation of {@link LocalizationSource}.
 *
 * @version $Id$
 * @since 13.3RC1
 */
@Component
@Named("org.xwiki.localization.internal.DefaultLocalizationSource")
@Singleton
public class DefaultLocalizationSource implements LocalizationSource, XWikiRestComponent
{
    @Inject
    private LocalizationContext localizationContext;

    @Inject
    private LocalizationManager localizationManager;

    @Inject
    private Provider<XWikiContext> contextProvider;

    @Inject
    private Logger logger;

    @Override
    public Response translations(String wikiId, String localeString, String prefix, List<String> keys)
    {
        XWikiContext xWikiContext = this.contextProvider.get();

        // Save the current context wiki id. It will be restored on the method is finished.
        String oldWikiId = xWikiContext.getWikiId();
        try {
            // Set the requested wiki id.
            xWikiContext.setWikiId(wikiId);

            // Resolve the local.
            Locale locale;
            if (localeString != null) {
                locale = LocaleUtils.toLocale(localeString);
            } else {
                locale = this.localizationContext.getCurrentLocale();
            }

            // Computes the prefix.
            String cleanPrefix = Objects.toString(prefix, "");

            ObjectNode result = JsonNodeFactory.instance.objectNode();

            // Resolve the raw translation of the requested keys.
            for (String key : keys) {
                if (key != null) {
                    String fullKey = cleanPrefix + key;
                    Translation translation = this.localizationManager.getTranslation(fullKey, locale);
                    if (translation != null) {
                        Object rawSource = translation.getRawSource();
                        result.put(fullKey, String.valueOf(rawSource));
                    } else {
                        // When a translation key is not found, it is still added to the result object, associated with
                        // a null value.
                        result.putNull(fullKey);
                        this.logger.warn("Key [{}] not found for local [{}] in wiki [{}].", key, locale, wikiId);
                    }
                }
            }

            return Response.ok(result).build();
        } finally {
            // Restore the old wiki id in the context.
            xWikiContext.setWikiId(oldWikiId);
        }
    }
}
