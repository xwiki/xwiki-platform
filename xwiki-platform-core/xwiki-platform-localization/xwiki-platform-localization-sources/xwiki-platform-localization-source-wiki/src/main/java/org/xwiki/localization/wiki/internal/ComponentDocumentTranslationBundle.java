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
package org.xwiki.localization.wiki.internal;

import java.util.Locale;

import org.xwiki.bridge.event.WikiDeletedEvent;
import org.xwiki.component.descriptor.ComponentDescriptor;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.localization.TranslationBundle;
import org.xwiki.localization.message.TranslationMessageParser;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.observation.event.Event;
import org.xwiki.security.authorization.AccessDeniedException;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;

/**
 * Component wiki document based implementation of Bundle.
 * 
 * @see AbstractDocumentTranslationBundle
 * @version $Id$
 * @since 6.2RC1
 */
public class ComponentDocumentTranslationBundle extends AbstractDocumentTranslationBundle
{
    private DocumentTranslationBundleFactory factory;

    private ComponentDescriptor<TranslationBundle> descriptor;

    /**
     * @param idPrefix the prefix to use when generating the bundle unique identifier
     * @param documentReference the document reference
     * @param componentManager used to lookup components needed to manipulate wiki documents
     * @param translationMessageParser the parser to use for each message
     * @param descriptor the component descriptor used to unregister the bundle
     * @param factory the factory
     * @throws ComponentLookupException failed to lookup some required components
     */
    public ComponentDocumentTranslationBundle(String idPrefix, DocumentReference documentReference,
        ComponentManager componentManager, TranslationMessageParser translationMessageParser,
        ComponentDescriptor<TranslationBundle> descriptor, DocumentTranslationBundleFactory factory)
        throws ComponentLookupException
    {
        super(idPrefix, documentReference, componentManager, translationMessageParser);

        this.factory = factory;
        this.descriptor = descriptor;
    }

    /**
     * {@inheritDoc}
     * This overrides the default implementation to first check the author rights for the document.
     *
     * @param locale the requested locale
     * @return the document defining the translation bundle if its author has the necessary rights, the default locale
     *     otherwise, and fallback on the original implementation if the document doesn't exist or could not be fetched
     */
    @Override
    protected XWikiDocument getDocumentLocaleBundle(Locale locale) throws Exception
    {
        XWikiDocument document = super.getDocumentLocaleBundle(locale);

        if (document != null && !document.isNew()) {
            XWikiContext context = this.contextProvider.get();
            XWiki xwiki = context.getWiki();
            XWikiDocument defaultLocaleDocument = xwiki.getDocument(this.documentReference, context);

            if (defaultLocaleDocument != document) {
                // We only need to check rights for non-default locales.
                try {
                    this.factory.checkRegistrationAuthorizationForDocumentLocaleBundle(document, defaultLocaleDocument);
                } catch (AccessDeniedException e) {
                    this.logger.warn("Failed to load and register the translation for locale [{}] from document [{}]. "
                        + "Falling back to default locale.", locale, document.getDocumentReference());
                    // We return the default translation bundle if the requested one has permission issues.
                    return defaultLocaleDocument;
                }
            }
        }

        return document;
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        super.onEvent(event, source, data);

        if (event instanceof WikiDeletedEvent) {
            // Unregister the component here because DocumentTranslationBundleFactory won't (it does not receive
            // document deleted event when a wiki is deleted)
            this.componentManager.unregisterComponent(this.descriptor);
        }
    }
}
