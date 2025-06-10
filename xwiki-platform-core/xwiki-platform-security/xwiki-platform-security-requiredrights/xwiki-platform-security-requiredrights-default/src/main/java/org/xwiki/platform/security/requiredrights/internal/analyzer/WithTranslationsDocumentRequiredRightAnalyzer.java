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
package org.xwiki.platform.security.requiredrights.internal.analyzer;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.platform.security.requiredrights.RequiredRightAnalysisResult;
import org.xwiki.platform.security.requiredrights.RequiredRightAnalyzer;
import org.xwiki.platform.security.requiredrights.RequiredRightsException;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;

/**
 * A required rights analyzer that analyzes the passed document including all translations.
 *
 * @version $Id$
 * @since 17.4.0RC1
 */
@Named("withTranslations")
@Component
@Singleton
public class WithTranslationsDocumentRequiredRightAnalyzer implements RequiredRightAnalyzer<DocumentReference>
{
    @Inject
    private RequiredRightAnalyzer<XWikiDocument> requiredRightAnalyzer;

    @Inject
    @Named("content")
    private RequiredRightAnalyzer<XWikiDocument> contentAnalyzer;

    @Inject
    private Provider<XWikiContext> xWikiContextProvider;

    @Override
    public List<RequiredRightAnalysisResult> analyze(DocumentReference documentReference) throws RequiredRightsException
    {
        // TODO: it would be awesome if we could simply add a cache here but the problem is that we include lots of
        // translations in the output and these translations depend on the current context at the moment that could
        // include translations that are only visible to a user or that also simply depend on the current user.
        // Therefore, it isn't that easy to cache the required rights analysis results. We might add a cache
        // depending on the user, locale and document reference but that doesn't seem that effective. A possibility
        // could be to let the object suppliers compute more data at display time and thereby remove the dependency
        // on locale and user. This would really be nice but also quite a change. Another problem is that some macros
        // could only be defined for the current user and therefore the macro analysis also depends on the current user.
        XWikiDocument document;
        XWikiContext context = this.xWikiContextProvider.get();
        try {
            document = context.getWiki().getDocument(documentReference.withoutLocale(), context);
        } catch (XWikiException e) {
            throw new RequiredRightsException("Failed to load document", e);
        }

        List<RequiredRightAnalysisResult> results = new ArrayList<>(this.requiredRightAnalyzer.analyze(document));

        try {
            List<Locale> translationLocales = document.getTranslationLocales(context);

            for (Locale locale : translationLocales) {
                XWikiDocument translation = document.getTranslatedDocument(locale, context);

                // For translations, we only analyze the content and not the XObjects and the XClass as they have
                // already been analyzed for the root locale.
                results.addAll(this.contentAnalyzer.analyze(translation));
            }
        } catch (XWikiException e) {
            throw new RequiredRightsException("Failed to load translations", e);
        }

        return results;
    }
}
