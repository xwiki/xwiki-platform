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
package org.xwiki.model.validation.internal;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.bridge.event.DocumentCreatedEvent;
import org.xwiki.bridge.event.DocumentDeletedEvent;
import org.xwiki.bridge.event.DocumentUpdatedEvent;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.model.validation.EntityNameValidation;
import org.xwiki.model.validation.EntityNameValidationManager;
import org.xwiki.observation.EventListener;
import org.xwiki.observation.event.Event;
import org.xwiki.observation.event.filter.RegexEventFilter;

import com.xpn.xwiki.doc.XWikiDocument;

/**
 * Listen for create/update/delete events on the entity name validation classes and objects. When their values change,
 * clears the entity name validators caches (by calling {@link EntityNameValidation#cleanConfigurationCache(String)} on
 * the registered {@link EntityNameValidation} components). The cache is managed per wiki, and only the cache of the
 * current wiki is cleared.
 *
 * @version $Id$
 * @since 13.4
 * @since 12.10.8
 */
@Component
@Named(EntityNameValidationConfigurationListener.NAME)
@Singleton
public class EntityNameValidationConfigurationListener implements EventListener
{
    /**
     * The name of the event listener.
     */
    static final String NAME = "EntityNameValidationConfigurationListener";

    @Inject
    private EntityNameValidationManager entityNameValidationManager;

    @Inject
    @Named("local")
    private EntityReferenceSerializer<String> entityReferenceSerializer;

    @Override
    public String getName()
    {
        return NAME;
    }

    @Override
    public List<Event> getEvents()
    {
        LocalDocumentReference classReference = EntityNameValidationConfigurationSource.CLASS_REFERENCE;
        LocalDocumentReference documentReference = EntityNameValidationConfigurationSource.DOC_REFERENCE;
        RegexEventFilter configurationClassEventFilter = createRegexEventFilter(classReference);
        RegexEventFilter configurationObjectEventFilter = createRegexEventFilter(documentReference);
        return Arrays.asList(
            new DocumentCreatedEvent(configurationClassEventFilter),
            new DocumentUpdatedEvent(configurationClassEventFilter),
            new DocumentDeletedEvent(configurationClassEventFilter),
            new DocumentCreatedEvent(configurationObjectEventFilter),
            new DocumentUpdatedEvent(configurationObjectEventFilter),
            new DocumentDeletedEvent(configurationObjectEventFilter)
        );
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        // Clear the cache of all the validation components for the wiki of the updated configuration.
        String wikiName = ((XWikiDocument) source).getDocumentReference().getWikiReference().getName();
        this.entityNameValidationManager.getAvailableEntityNameValidationsComponents()
            .forEach(it -> it.cleanConfigurationCache(wikiName));
    }

    /**
     * Creates a {@link RegexEventFilter} matching a local reference on any wiki. Schematically, for a local reference
     * {@code XWiki.Doc}, this method created a {@link RegexEventFilter} with the following regex: {@code
     * ".*:XWiki\.Doc"}.
     *
     * @param localDocumentReference the local document reference to filter on
     * @return the even filter matching the local document reference events
     */
    private RegexEventFilter createRegexEventFilter(LocalDocumentReference localDocumentReference)
    {
        String serializedReference = this.entityReferenceSerializer.serialize(localDocumentReference);
        // Uses quote on the serialized reference to escape special regex characters. For instance, to avoid treating
        // '.' as any character.
        String regexFilter = String.format(".*:%s", Pattern.quote(serializedReference));
        return new RegexEventFilter(regexFilter);
    }
}
