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

import org.xwiki.bridge.event.WikiDeletedEvent;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.localization.message.TranslationMessageParser;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.observation.event.Event;

/**
 * On demand wiki document based implementation of Bundle.
 * 
 * @see AbstractDocumentTranslationBundle
 * @version $Id$
 * @since 6.2C1
 */
class OnDemandDocumentTranslationBundle extends AbstractDocumentTranslationBundle
{
    private DocumentTranslationBundleFactory factory;

    private String uid;

    /**
     * @param idPrefix the prefix to use when generating the bundle unique identifier
     * @param documentReference the document reference
     * @param componentManager used to lookup components needed to manipulate wiki documents
     * @param translationMessageParser the parser to use for each message
     * @param factory the factory
     * @param uid the uid of the component in the cache
     * @throws ComponentLookupException failed to lookup some required components
     */
    OnDemandDocumentTranslationBundle(String idPrefix, DocumentReference documentReference,
        ComponentManager componentManager, TranslationMessageParser translationMessageParser,
        DocumentTranslationBundleFactory factory, String uid) throws ComponentLookupException
    {
        super(idPrefix, documentReference, componentManager, translationMessageParser);

        this.factory = factory;
        this.uid = uid;
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        super.onEvent(event, source, data);

        if (this.factory != null) {
            if (event instanceof WikiDeletedEvent) {
                // Clean on demand bundles cache here because DocumentTranslationBundleFactory won't (it does not
                // receive document deleted event when a wiki is deleted)
                this.factory.clear(this.uid);
            }
        }
    }
}
