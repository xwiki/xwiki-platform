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
import org.xwiki.search.solr.AbstractSolrCoreInitializer;
import org.xwiki.search.solr.SolrException;

/**
 * Initialize the Solr core dedicated to events storage.
 * 
 * @version $Id$
 * @since 12.3RC1
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
     * @see org.xwiki.eventstream.Event#getGroupId()
     */
    public static final String SOLR_FIELD_GROUPID = "groupId";

    /**
     * @see org.xwiki.eventstream.Event#getDate()
     */
    public static final String SOLR_FIELD_DATE = "date";

    /**
     * @see org.xwiki.eventstream.Event#getImportance()
     */
    public static final String SOLR_FIELD_IMPORTANCE = "importance";

    /**
     * @see org.xwiki.eventstream.Event#getTitle()
     */
    public static final String SOLR_FIELD_TITLE = "title";

    /**
     * @see org.xwiki.eventstream.Event#getBody()
     */
    public static final String SOLR_FIELD_BODY = "body";

    /**
     * @see org.xwiki.eventstream.Event#getApplication()
     */
    public static final String SOLR_FIELD_APPLICATION = "application";

    /**
     * @see org.xwiki.eventstream.Event#getStream()
     */
    public static final String SOLR_FIELD_STREAM = "stream";

    /**
     * @see org.xwiki.eventstream.Event#getType()
     */
    public static final String SOLR_FIELD_TYPE = "type";

    /**
     * @see org.xwiki.eventstream.Event#getWiki()
     */
    public static final String SOLR_FIELD_WIKI = "wiki";

    /**
     * @see org.xwiki.eventstream.Event#getSpace()
     */
    public static final String SOLR_FIELD_SPACE = "space";

    /**
     * @see org.xwiki.eventstream.Event#getDocument()
     */
    public static final String SOLR_FIELD_DOCUMENT = "document";

    /**
     * @see org.xwiki.eventstream.Event#getDocumentVersion()
     */
    public static final String SOLR_FIELD_DOCUMENTVERSION = "documentVersion";

    /**
     * @see org.xwiki.eventstream.Event#getRelatedEntity()
     */
    public static final String SOLR_FIELD_RELATEDENTITY = "relatedEntity";

    /**
     * @see org.xwiki.eventstream.Event#getUser()
     */
    public static final String SOLR_FIELD_USER = "user";

    /**
     * @see org.xwiki.eventstream.Event#getUrl()
     */
    public static final String SOLR_FIELD_URL = "url";

    /**
     * @see org.xwiki.eventstream.Event#getDocumentTitle()
     */
    public static final String SOLR_FIELD_DOCUMENTTITLE = "documentTitle";

    /**
     * @see org.xwiki.eventstream.Event#getTarget()
     */
    public static final String SOLR_FIELD_TARGET = "target";

    /**
     * @see org.xwiki.eventstream.Event#getHidden()
     */
    public static final String SOLR_FIELD_HIDDEN = "hidden";

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
     * The known fields.
     */
    public static final Set<String> KNOWN_FIELDS = new HashSet<>(
        Arrays.asList(SOLR_FIELD_ID, SOLR_FIELD_GROUPID, SOLR_FIELD_DATE, SOLR_FIELD_IMPORTANCE, SOLR_FIELD_TITLE,
            SOLR_FIELD_BODY, SOLR_FIELD_APPLICATION, SOLR_FIELD_STREAM, SOLR_FIELD_TYPE, SOLR_FIELD_WIKI,
            SOLR_FIELD_SPACE, SOLR_FIELD_DOCUMENT, SOLR_FIELD_DOCUMENTVERSION, SOLR_FIELD_RELATEDENTITY,
            SOLR_FIELD_USER, SOLR_FIELD_URL, SOLR_FIELD_DOCUMENTTITLE, SOLR_FIELD_TARGET, SOLR_FIELD_HIDDEN));

    @Override
    protected long getVersion()
    {
        return SCHEMA_VERSION_12_3;
    }

    @Override
    protected void createSchema() throws SolrException
    {
        addStringField(SOLR_FIELD_GROUPID, false, false);
        addPDateField(SOLR_FIELD_DATE, false, false);
        addStringField(SOLR_FIELD_IMPORTANCE, false, false);
        addStringField(SOLR_FIELD_TITLE, false, false);
        addStringField(SOLR_FIELD_BODY, false, false);
        addStringField(SOLR_FIELD_APPLICATION, false, false);
        addStringField(SOLR_FIELD_STREAM, false, false);
        addStringField(SOLR_FIELD_TYPE, false, false);
        addStringField(SOLR_FIELD_WIKI, false, false);
        addStringField(SOLR_FIELD_SPACE, false, false);
        addStringField(SOLR_FIELD_DOCUMENT, false, false);
        addStringField(SOLR_FIELD_DOCUMENTVERSION, false, false);
        addStringField(SOLR_FIELD_RELATEDENTITY, false, false);
        addStringField(SOLR_FIELD_USER, false, false);
        addStringField(SOLR_FIELD_URL, false, false);
        addStringField(SOLR_FIELD_DOCUMENTTITLE, false, false);
        addStringField(SOLR_FIELD_TARGET, true, false);
        addBooleanField(SOLR_FIELD_HIDDEN, false, false);

        addStringField(SOLR_FIELD_READLISTENERS, true, false);
        addStringField(SOLR_FIELD_UNREADLISTENERS, true, false);

        addMapField(SOLR_FIELD_PROPERTIES);
    }

    @Override
    protected void migrateSchema(Long cversion)
    {
        // No migration needed yet
    }
}
