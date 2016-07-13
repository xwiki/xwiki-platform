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
package org.xwiki.filter.instance.internal.input;

import java.util.List;
import java.util.Locale;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.InstantiationStrategy;
import org.xwiki.component.descriptor.ComponentInstantiationStrategy;
import org.xwiki.filter.FilterEventParameters;
import org.xwiki.filter.FilterException;
import org.xwiki.filter.descriptor.FilterStreamDescriptor;
import org.xwiki.filter.event.model.WikiDocumentFilter;
import org.xwiki.filter.instance.input.AbstractInstanceInputEventGenerator;
import org.xwiki.filter.instance.input.EntityEventGenerator;
import org.xwiki.filter.instance.internal.XWikiDocumentFilter;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.properties.BeanManager;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;

@Component
@Named("documents")
@InstantiationStrategy(ComponentInstantiationStrategy.PER_LOOKUP)
public class DocumentInstanceInputEventGenerator extends AbstractInstanceInputEventGenerator<XWikiDocumentFilter>
{
    /**
     * The {@link BeanManager} component.
     */
    @Inject
    protected BeanManager beanManager;

    @Inject
    private EntityEventGenerator<XWikiDocument> documentLocaleParser;

    @Inject
    private Provider<XWikiContext> xcontextProvider;

    @Override
    public FilterStreamDescriptor getDescriptor()
    {
        return this.documentLocaleParser.getDescriptor();
    }
    
    @Override
    public void setWikiDocumentParameters(String name, FilterEventParameters documentParameters)
        throws FilterException
    {
        DocumentReference reference = new DocumentReference(name, new SpaceReference(this.currentReference));

        XWikiContext xcontext = this.xcontextProvider.get();

        XWikiDocument defaultDocument;
        try {
            defaultDocument = xcontext.getWiki().getDocument(reference, xcontext);
        } catch (XWikiException e) {
            throw new FilterException("Failed to get document [" + reference + "]", e);
        }

        documentParameters.put(WikiDocumentFilter.PARAMETER_LOCALE, defaultDocument.getDefaultLocale());
    }

    @Override
    public void beginWikiDocument(String name, FilterEventParameters parameters) throws FilterException
    {
        super.beginWikiDocument(name, parameters);

        DocumentReference reference = new DocumentReference(this.currentReference);

        XWikiContext xcontext = this.xcontextProvider.get();

        XWikiDocument defaultDocument;
        try {
            defaultDocument = xcontext.getWiki().getDocument(reference, xcontext);
        } catch (XWikiException e) {
            throw new FilterException("Failed to get document [" + reference + "]", e);
        }

        // Default document locale
        this.documentLocaleParser.write(defaultDocument, this.filter, this.properties);

        List<Locale> locales;
        try {
            locales = defaultDocument.getTranslationLocales(xcontext);
        } catch (XWikiException e) {
            throw new FilterException("Failed to get translations of document [" + reference + "]", e);
        }

        // Translations
        for (Locale locale : locales) {
            try {
                XWikiDocument translationDocument = defaultDocument.getTranslatedDocument(locale, xcontext);
                this.documentLocaleParser.write(translationDocument, this.filter, this.properties);
            } catch (XWikiException e) {
                throw new FilterException("Failed to get document [" + reference + "] for locale [" + locale + "]",
                    e);
            }
        }
    }
}
