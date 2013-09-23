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
package org.xwiki.wikistream.instance.internal.input;

import java.util.List;
import java.util.Locale;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.InstantiationStrategy;
import org.xwiki.component.descriptor.ComponentInstantiationStrategy;
import org.xwiki.filter.FilterEventParameters;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.wikistream.WikiStreamException;
import org.xwiki.wikistream.filter.WikiDocumentFilter;
import org.xwiki.wikistream.instance.input.AbstractInstanceInputEventGenerator;
import org.xwiki.wikistream.instance.input.EntityEventGenerator;
import org.xwiki.wikistream.instance.internal.XWikiDocumentFilter;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;

@Component
@Named("documents")
@InstantiationStrategy(ComponentInstantiationStrategy.PER_LOOKUP)
public class DocumentInstanceInputEventGenerator extends AbstractInstanceInputEventGenerator<XWikiDocumentFilter>
{
    @Inject
    private EntityEventGenerator<XWikiDocument> documentParser;

    @Inject
    private Provider<XWikiContext> xcontextProvider;

    @Override
    public void beginWikiDocument(String name, FilterEventParameters parameters) throws WikiStreamException
    {
        super.beginWikiDocument(name, parameters);

        DocumentReference reference = new DocumentReference(this.currentReference);

        XWikiContext xcontext = this.xcontextProvider.get();

        XWikiDocument defaultDocument;
        try {
            defaultDocument = xcontext.getWiki().getDocument(reference, xcontext);
        } catch (XWikiException e) {
            throw new WikiStreamException("Failed to get document [" + reference + "]", e);
        }

        // > WikiDocument

        FilterEventParameters documentParameters = new FilterEventParameters();

        documentParameters.put(WikiDocumentFilter.PARAMETER_LOCALE, defaultDocument.getDefaultLocale());

        this.proxyFilter.beginWikiDocument(name, documentParameters);

        // Default document locale
        this.documentParser.write(defaultDocument, this.filter, this.properties);

        List<Locale> locales;
        try {
            locales = defaultDocument.getTranslationLocales(xcontext);
        } catch (XWikiException e) {
            throw new WikiStreamException("Failed to get translations of document [" + reference + "]", e);
        }

        // Translations
        for (Locale locale : locales) {
            try {
                XWikiDocument translationDocument = defaultDocument.getTranslatedDocument(locale, xcontext);
                this.documentParser.write(translationDocument, this.filter, this.properties);
            } catch (XWikiException e) {
                throw new WikiStreamException("Failed to get document [" + reference + "] for locale [" + locale + "]",
                    e);
            }
        }

        // < WikiDocument

        this.proxyFilter.endWikiDocument(defaultDocument.getDocumentReference().getName(), documentParameters);
    }
}
