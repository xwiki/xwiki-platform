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
package org.xwiki.extension.index.internal;

import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.extension.Extension;
import org.xwiki.extension.RemoteExtension;
import org.xwiki.search.solr.AbstractSolrCoreInitializer;
import org.xwiki.search.solr.SolrException;

/**
 * Initialize the Solr core dedicated to events storage.
 * 
 * @version $Id$
 * @since 12.9RC1
 */
@Component
@Named(ExtensionIndexSolrCoreInitializer.NAME)
@Singleton
public class ExtensionIndexSolrCoreInitializer extends AbstractSolrCoreInitializer
{
    /**
     * The name of the core.
     */
    public static final String NAME = "extension_index";

    /**
     * Suffix used for indexed version of standard extensions fields.
     */
    public static final String INDEX_SUFFIX = "_index";

    /**
     * We can't use {@link Extension#FIELD_ID} because it collide with {@link #SOLR_FIELD_ID}.
     */
    public static final String SOLR_FIELD_EXTENSIONID = "extensionId";

    /**
     * Indicate on the available versions for this extension (to avoid indexing all versions).
     */
    public static final String SOLR_FIELD_VERSIONS = "versions";

    /**
     * Indicate the list of features in a search friendly manner.
     */
    public static final String SOLR_FIELD_EXTENSIONFEATURES_INDEX = Extension.FIELD_EXTENSIONFEATURES + INDEX_SUFFIX;

    /**
     * Indicate the list of authors in a search friendly manner.
     */
    public static final String SOLR_FIELD_AUTHORS_INDEX = Extension.FIELD_AUTHORS + INDEX_SUFFIX;

    /**
     * The name of the field indicating if an extension is compatible with the current instance.
     */
    public static final String SOLR_FIELD_COMPATIBLE = "compatible";

    /**
     * The name of the field containing the date at which the extension was indexed.
     */
    public static final String SOLR_FIELD_INDEX_DATE = "indexDate";

    private static final long SCHEMA_VERSION_12_9 = 120900000;

    @Override
    protected long getVersion()
    {
        return SCHEMA_VERSION_12_9;
    }

    @Override
    protected void createSchema() throws SolrException
    {
        setStringField(SOLR_FIELD_EXTENSIONID, false, false);
        setStringField(Extension.FIELD_VERSION, false, false);

        setStringField(Extension.FIELD_TYPE, false, false);
        setStringField(Extension.FIELD_REPOSITORY, false, false);
        setStringField(Extension.FIELD_ALLOWEDNAMESPACES, true, false);
        setStringField(Extension.FIELD_SUMMARY, false, false);
        setStringField(Extension.FIELD_WEBSITE, false, false);
        setStringField(Extension.FIELD_CATEGORY, false, false);

        setBooleanField(RemoteExtension.FIELD_RECOMMENDED, false, false);

        setStringField(Extension.FIELD_AUTHORS, true, false);
        setStringField(SOLR_FIELD_AUTHORS_INDEX, true, false, SOLR_FIELD_STORED, false);

        setStringField(Extension.FIELD_EXTENSIONFEATURES, true, false);
        setStringField(SOLR_FIELD_EXTENSIONFEATURES_INDEX, true, false, SOLR_FIELD_STORED, false);

        // TODO: add dependencies
        // TODO: add managed dependencies

        setPDateField(SOLR_FIELD_INDEX_DATE, false, false);
        setBooleanField(SOLR_FIELD_COMPATIBLE, false, false);

        setStringField(SOLR_FIELD_VERSIONS, true, false);
    }

    @Override
    protected void migrateSchema(long cversion) throws SolrException
    {
        // No change yet
    }
}
