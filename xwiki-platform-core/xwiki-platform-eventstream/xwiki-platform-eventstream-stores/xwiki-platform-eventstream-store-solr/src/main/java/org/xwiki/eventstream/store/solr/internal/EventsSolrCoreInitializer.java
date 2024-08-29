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
     * The name of the field containing the list of users who should get this event by mail.
     * 
     * @since 12.6
     */
    public static final String SOLR_FIELD_MAILLISTENERS = "mailListeners";

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

    @Override
    protected long getVersion()
    {
        return SCHEMA_VERSION_14_7;
    }

    @Override
    protected void createSchema() throws SolrException
    {
        setStringField(Event.FIELD_GROUPID, false, false);
        setPDateField(Event.FIELD_DATE, false, false);
        setStringField(Event.FIELD_IMPORTANCE, false, false);
        setStringField(Event.FIELD_TITLE, false, false);
        setStringField(Event.FIELD_BODY, false, false);
        setStringField(Event.FIELD_APPLICATION, false, false);
        setStringField(Event.FIELD_STREAM, false, false);
        setStringField(Event.FIELD_TYPE, false, false);
        setStringField(Event.FIELD_WIKI, false, false);
        setStringField(Event.FIELD_SPACE, false, false);
        setStringField(Event.FIELD_DOCUMENT, false, false);
        setStringField(Event.FIELD_DOCUMENTVERSION, false, false);
        setStringField(Event.FIELD_RELATEDENTITY, false, false);
        setStringField(Event.FIELD_USER, false, false);
        setStringField(Event.FIELD_URL, false, false);
        setStringField(Event.FIELD_DOCUMENTTITLE, false, false);
        setStringField(Event.FIELD_TARGET, true, false);
        setBooleanField(Event.FIELD_HIDDEN, false, false);
        setBooleanField(Event.FIELD_PREFILTERED, false, false);

        setStringField(SOLR_FIELD_READLISTENERS, true, false);
        setStringField(SOLR_FIELD_UNREADLISTENERS, true, false);

        setMapField(SOLR_FIELD_PROPERTIES);

        // Add support for searching various forms of references
        setStringField(FIELD_SPACE_INDEX, true, false, SOLR_FIELD_STORED, false);
        setStringField(FIELD_DOCUMENT_INDEX, true, false, SOLR_FIELD_STORED, false);

        migrateSchema(SCHEMA_VERSION_12_5);
    }

    @Override
    protected void migrateSchema(long cversion) throws SolrException
    {
        if (cversion < SCHEMA_VERSION_12_6) {
            setStringField(SOLR_FIELD_MAILLISTENERS, true, false);
        }
        if (cversion < SCHEMA_VERSION_14_7) {
            setStringField(Event.FIELD_REMOTE_OBSERVATION_ID, false, false);
        }
    }

    @Override
    protected int getMigrationBatchRows()
    {
        return 1000;
    }
}
