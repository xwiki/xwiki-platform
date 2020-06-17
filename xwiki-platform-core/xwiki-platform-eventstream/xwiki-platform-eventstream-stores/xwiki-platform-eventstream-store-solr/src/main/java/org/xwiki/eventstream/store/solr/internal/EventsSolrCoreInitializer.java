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
package org.xwiki.eventstream.store.solr.internal;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.eventstream.Event;
import org.xwiki.search.solr.AbstractSolrCoreInitializer;
import org.xwiki.search.solr.SolrException;

/**
 * Initialize the Solr core dedicated to events storage.
 * 
 * @version $Id$
 * @since 12.4RC1
 */
@Component
@Named(EventsSolrCoreInitializer.NAME)
@Singleton
public class EventsSolrCoreInitializer extends AbstractSolrCoreInitializer
{
    /**
     * The name of the core.
     */
    public static final String NAME = "events";

    /**
     * @see org.xwiki.eventstream.Event#getParameters()
     */
    public static final String SOLR_FIELD_PROPERTIES = "properties";

    /**
     * The name of the field containing the list of users having this event in its inbox in read state.
     */
    public static final String SOLR_FIELD_READLISTENERS = "readListeners";

    /**
     * The name of the field containing the list of users having this event in its inbox in unread state.
     */
    public static final String SOLR_FIELD_UNREADLISTENERS = "unreadListeners";

    /**
     * The name of the field containing the different variation of space reference.
     * 
     * @since 12.5RC1
     */
    public static final String FIELD_SPACE_INDEX = "space_index";

    /**
     * The name of the field containing the different variations of document reference.
     * 
     * @since 12.5RC1
     */
    public static final String FIELD_DOCUMENT_INDEX = "document_index";

    /**
     * The known fields.
     */
    public static final Set<String> KNOWN_FIELDS = new HashSet<>(Arrays.asList(SOLR_FIELD_ID, Event.FIELD_GROUPID,
        Event.FIELD_DATE, Event.FIELD_IMPORTANCE, Event.FIELD_TITLE, Event.FIELD_BODY, Event.FIELD_APPLICATION,
        Event.FIELD_STREAM, Event.FIELD_TYPE, Event.FIELD_WIKI, Event.FIELD_SPACE, Event.FIELD_DOCUMENT,
        Event.FIELD_DOCUMENTVERSION, Event.FIELD_RELATEDENTITY, Event.FIELD_USER, Event.FIELD_URL,
        Event.FIELD_DOCUMENTTITLE, Event.FIELD_TARGET, Event.FIELD_HIDDEN, Event.FIELD_PREFILTERED));

    @Override
    protected long getVersion()
    {
        return SCHEMA_VERSION_12_5;
    }

    @Override
    protected void createSchema() throws SolrException
    {
        addStringField(Event.FIELD_GROUPID, false, false);
        addPDateField(Event.FIELD_DATE, false, false);
        addStringField(Event.FIELD_IMPORTANCE, false, false);
        addStringField(Event.FIELD_TITLE, false, false);
        addStringField(Event.FIELD_BODY, false, false);
        addStringField(Event.FIELD_APPLICATION, false, false);
        addStringField(Event.FIELD_STREAM, false, false);
        addStringField(Event.FIELD_TYPE, false, false);
        addStringField(Event.FIELD_WIKI, false, false);
        addStringField(Event.FIELD_SPACE, false, false);
        addStringField(Event.FIELD_DOCUMENT, false, false);
        addStringField(Event.FIELD_DOCUMENTVERSION, false, false);
        addStringField(Event.FIELD_RELATEDENTITY, false, false);
        addStringField(Event.FIELD_USER, false, false);
        addStringField(Event.FIELD_URL, false, false);
        addStringField(Event.FIELD_DOCUMENTTITLE, false, false);
        addStringField(Event.FIELD_TARGET, true, false);
        addBooleanField(Event.FIELD_HIDDEN, false, false);
        addBooleanField(Event.FIELD_PREFILTERED, false, false);

        addStringField(SOLR_FIELD_READLISTENERS, true, false);
        addStringField(SOLR_FIELD_UNREADLISTENERS, true, false);

        addMapField(SOLR_FIELD_PROPERTIES);

        migrateSchema(SCHEMA_VERSION_12_3);
    }

    @Override
    protected void migrateSchema(long cversion) throws SolrException
    {
        if (cversion < SCHEMA_VERSION_12_5) {
            // Add support for searching various forms of references
            //setStringField(FIELD_SPACE_INDEX, true, false, SOLR_FIELD_STORED, false);
            //setStringField(FIELD_DOCUMENT_INDEX, true, false, SOLR_FIELD_STORED, false);
            setStringField(FIELD_SPACE_INDEX, true, false);
            setStringField(FIELD_DOCUMENT_INDEX, true, false);
        }
    }
}
