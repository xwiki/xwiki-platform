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
package org.xwiki.search.solr.internal;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.request.schema.FieldTypeDefinition;
import org.apache.solr.client.solrj.response.schema.FieldTypeRepresentation;
import org.apache.solr.schema.FieldType;
import org.slf4j.Logger;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.phase.Disposable;
import org.xwiki.search.solr.Solr;
import org.xwiki.search.solr.SolrCoreInitializer;
import org.xwiki.search.solr.SolrException;
import org.xwiki.search.solr.XWikiSolrCore;

/**
 * The entry point of access Solr cores.
 * 
 * @version $Id$
 * @since 12.3RC1
 */
public abstract class AbstractSolr implements Solr, Disposable
{
    private static final String SOLR_TYPENAME_SVERSION = "__sversion";

    private static final String SOLR_VERSIONFIELDTYPE_VALUE = "defVal";

    @Inject
    protected ComponentManager componentManager;

    @Inject
    protected SolrSchemaUtils solrSchemaUtils;

    @Inject
    protected Logger logger;

    protected final Map<String, XWikiSolrCore> cores = new ConcurrentHashMap<>();

    @Override
    public void dispose()
    {
        for (XWikiSolrCore core : this.cores.values()) {
            try {
                core.getClient().close();
            } catch (IOException e) {
                this.logger.error("Failed to close Solr client", e);
            }
        }
    }

    @Override
    @Deprecated
    public SolrClient getClient(String name) throws SolrException
    {
        XWikiSolrCore core = getCore(name);

        return core != null ? core.getClient() : null;
    }

    @Override
    public XWikiSolrCore getCore(String name) throws SolrException
    {
        String id = StringUtils.defaultString(name);

        try {
            return this.cores.computeIfAbsent(id, this::getRuntimeCore);
        } catch (RuntimeException e) {
            throw new SolrException("Failed to get client", e);
        }
    }

    private XWikiSolrCore getRuntimeCore(String coreName)
    {
        try {
            return getCore(coreName, getSolrMajorVersion(), true);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private synchronized XWikiSolrCore getCore(String xwikiCoreName, int majorVersion, boolean createAndMigrate)
        throws SolrServerException, IOException, SolrException
    {
        // Resolve the real Solr core name
        String solrCoreName = toSolrCoreName(xwikiCoreName, majorVersion);

        // Get the existing client
        SolrClient solrClient = getInternalSolrClient(solrCoreName);

        // Initialize the client
        if (this.componentManager.hasComponent(SolrCoreInitializer.class, xwikiCoreName)) {
            SolrCoreInitializer initializer;
            try {
                initializer = this.componentManager.getInstance(SolrCoreInitializer.class, xwikiCoreName);
            } catch (ComponentLookupException e) {
                throw new SolrException("Failed to get the SolrCoreInitializer for core name [{}]", e);
            }

            // If no core already exist create a new core
            if (solrClient == null && createAndMigrate) {
                solrClient = createSolrClient(solrCoreName, initializer.isCache());
            }

            if (solrClient != null) {
                // Create the XWikiSolrCore
                XWikiSolrCore solrCore = new DefaultXWikiSolrCore(xwikiCoreName, solrCoreName, solrClient);

                if (createAndMigrate) {
                    // Check if the core needs to be migrated from a previous major version of Solr
                    migrateCore(xwikiCoreName, solrCore, initializer);

                    // Custom initialization of the core
                    initializer.initialize(solrCore);
                }

                return solrCore;
            }
        } else {
            return new DefaultXWikiSolrCore(xwikiCoreName, solrCoreName, solrClient);
        }

        return null;
    }

    protected void migrateCore(String xwikiCoreName, XWikiSolrCore newCore, SolrCoreInitializer initializer)
        throws SolrException, SolrServerException, IOException
    {
        Integer sVersion = getSVersion(newCore);
        int solrVersion = getSolrMajorVersion();
        if (sVersion == null || sVersion < getSolrMajorVersion()) {
            for (int previousVersion = solrVersion - 1; previousVersion >= 8; --previousVersion) {
                // Check if a core for this version of Solr exist
                XWikiSolrCore previousCore = getCore(xwikiCoreName, previousVersion, false);

                if (previousCore != null) {
                    this.logger.debug("A previous core was found for name [{}] ([{}])", xwikiCoreName,
                        previousCore.getSolrName());

                    // Copy the previous core
                    initializer.migrate(previousCore, newCore);

                    // Close the previous client
                    previousCore.getClient().close();

                    break;
                } else {
                    this.logger.debug("Not previous core could be found for name [{}]", xwikiCoreName);
                }
            }

            // Mark the core as fully migrated
            setSVersion(newCore, solrVersion, sVersion == null);
        }
    }

    protected Integer getSVersion(XWikiSolrCore core) throws SolrException
    {
        FieldTypeRepresentation fieldType = this.solrSchemaUtils.getFieldTypes(core, false).get(SOLR_TYPENAME_SVERSION);

        if (fieldType == null) {
            return null;
        }

        String value = (String) fieldType.getAttributes().get(SOLR_VERSIONFIELDTYPE_VALUE);

        return NumberUtils.createInteger(value);
    }

    protected void setSVersion(XWikiSolrCore core, long version, boolean add) throws SolrException
    {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put(FieldType.TYPE_NAME, SOLR_TYPENAME_SVERSION);
        attributes.put(FieldType.CLASS_NAME, "solr.ExternalFileField");
        attributes.put(SOLR_VERSIONFIELDTYPE_VALUE, String.valueOf(version));

        FieldTypeDefinition definition = new FieldTypeDefinition();
        definition.setAttributes(attributes);

        this.solrSchemaUtils.setFieldType(core, definition, add);

        // Make sure it's applied
        this.solrSchemaUtils.commit(core);
    }

    protected abstract int getSolrMajorVersion();

    protected String getSolrCoreSuffix()
    {
        int majorVersion = getSolrMajorVersion();

        return getSolrCoreSuffix(majorVersion);
    }

    protected String getSolrCoreSuffix(int majorVersion)
    {
        // The solr version was not part of the core name before XWiki Solr 9 support
        return majorVersion < 9 ? "" : "_" + getSolrMajorVersion();
    }

    protected String toSolrCoreName(String xwikiCoreName)
    {
        return toSolrCoreName(xwikiCoreName, getSolrMajorVersion());
    }

    protected String toSolrCoreName(String xwikiCoreName, int majorVersion)
    {
        return xwikiCoreName + getSolrCoreSuffix(majorVersion);
    }

    protected String toXWikiCoreName(String solrCoreName)
    {
        return StringUtils.removeEnd(solrCoreName, getSolrCoreSuffix());
    }

    /**
     * @param solrCoreName the real solr core name
     * @return the core or null if no core exist by this name
     * @throws SolrException when failing to create the solr client
     */
    protected abstract SolrClient getInternalSolrClient(String solrCoreName) throws SolrException;

    /**
     * @since 16.2.0RC1
     */
    protected abstract SolrClient createSolrClient(String solrCoreName, boolean isCache) throws SolrException;
}
