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
import org.apache.commons.lang3.Strings;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.solr.client.api.util.SolrVersion;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.request.schema.FieldTypeDefinition;
import org.apache.solr.client.solrj.response.schema.FieldTypeRepresentation;
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
            return getCoreInternal(coreName);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private synchronized XWikiSolrCore getCoreInternal(String xwikiCoreName)
        throws IOException, SolrException
    {
        // Get the existing client designed for the current Solr version
        XWikiSolrCore solrCore = getCore(xwikiCoreName, getCoreMajorVersion());

        // If we cannot create a core, try to find one designed for the previous major version of Solr
        if (solrCore == null && !canCreateCore()) {
            solrCore = getCore(xwikiCoreName, getCoreMajorVersion() - 1);

            if (solrCore == null) {
                // No core exist for the current Solr version and we cannot create a new one
                return null;
            }
        }

        // Initialize the core
        if (this.componentManager.hasComponent(SolrCoreInitializer.class, xwikiCoreName)) {
            SolrCoreInitializer initializer;
            try {
                initializer = this.componentManager.getInstance(SolrCoreInitializer.class, xwikiCoreName);
            } catch (ComponentLookupException e) {
                throw new SolrException("Failed to get the SolrCoreInitializer for core name [{}]", e);
            }

            // If no core already exist, create a new core
            if (solrCore == null) {
                solrCore = createCore(xwikiCoreName, initializer.isCache());
            }

            if (solrCore != null) {
                // Check if the core needs to be migrated from a previous major version of Solr
                migrateCore(xwikiCoreName, solrCore, initializer);

                // Custom initialization of the core
                initializer.initialize(solrCore);

                return solrCore;
            }
        }

        return solrCore;
    }

    protected void migrateCore(String xwikiCoreName, XWikiSolrCore newCore, SolrCoreInitializer initializer)
        throws SolrException, IOException
    {
        // Get the version of Solr the cores are currently designed for
        int coreMajorVersion = getCoreMajorVersion();

        // There is nothing to migrate if the only core found is designed from the previous version of Solr
        if (newCore.getSolrMajorVersion() < coreMajorVersion) {
            return;
        }

        // Migrate the core, if needed
        Integer sVersion = getSVersion(newCore);
        if ((sVersion == null || sVersion < coreMajorVersion)
            && migrateFromPreviousCore(xwikiCoreName, newCore, initializer)) {
            // Mark the core as fully migrated (when the migration is not possible, the core is not marked so that the
            // migration is attempted again at the next initialization, once the administrator made it possible)
            setSVersion(newCore, coreMajorVersion, sVersion == null);
        }
    }

    private boolean migrateFromPreviousCore(String xwikiCoreName, XWikiSolrCore newCore,
        SolrCoreInitializer initializer) throws SolrException, IOException
    {
        for (int previousVersion = getCoreMajorVersion() - 1; previousVersion >= 8; --previousVersion) {
            // Check if a core for previous version of Solr exist
            XWikiSolrCore previousCore = getCore(xwikiCoreName, previousVersion);

            if (previousCore != null) {
                this.logger.debug("A previous core was found for name [{}] ([{}])", xwikiCoreName,
                    previousCore.getSolrName());

                // Copy the previous core
                initializer.migrate(previousCore, newCore);

                // Close the previous client
                previousCore.getClient().close();

                return true;
            }

            // The core might exist but fail to be loaded by Solr (in which case Solr does not list it among the
            // available cores)
            String loadFailure = getCoreLoadFailure(xwikiCoreName, previousVersion);
            if (loadFailure != null) {
                String previousSolrCoreName = toSolrCoreName(xwikiCoreName, previousVersion);

                // Not being able to migrate a core which can be filled again is not a problem
                if (initializer.isCache()) {
                    this.logger.debug("Skipping the migration of the cache core [{}] which failed to be loaded: {}",
                        previousSolrCoreName, loadFailure);

                    return true;
                }

                logImpossibleMigration(previousSolrCoreName, previousVersion, newCore, loadFailure);

                return false;
            }

            this.logger.debug("Not previous core could be found for name [{}]", xwikiCoreName);
        }

        return true;
    }

    private void logImpossibleMigration(String previousSolrCoreName, int previousVersion, XWikiSolrCore newCore,
        String loadFailure)
    {
        int serverMajorVersion = getSolrMajorVersion();

        String message = "The data of the Solr core [{}] (designed for Solr {}) cannot be migrated to the Solr core"
            + " [{}]: it failed to be loaded by the Solr server ({}).";
        // Lucene can only read an index created with the previous major version at most (even if all its segments
        // were rewritten since by a more recent version)
        if (previousVersion < serverMajorVersion - 1) {
            message += String.format(" This is most probably because Solr %d can only read the data of cores created"
                + " with Solr %d or more recent. %s", serverMajorVersion, serverMajorVersion - 1,
                getImpossibleMigrationHint(serverMajorVersion - 1));
        }
        message += " In the meantime, the Solr core [{}] is used without the previous data, and the migration will"
            + " be attempted again at the next startup.";
        this.logger.error(message, previousSolrCoreName, previousVersion, newCore.getSolrName(), loadFailure,
            newCore.getSolrName());
    }

    /**
     * @param intermediateVersion the Solr major version through which the data needs to be migrated first
     * @return what the administrator should do to make possible the migration of a core designed for a too old Solr
     *         major version
     * @since 18.10.0RC1
     */
    protected String getImpossibleMigrationHint(int intermediateVersion)
    {
        return String.format("To keep the data, upgrade first to an XWiki version bundling Solr %d.",
            intermediateVersion);
    }

    /**
     * @param xwikiCoreName the name of the core in XWiki
     * @param solrMajorVersion the Solr major version the core is designed for
     * @return the reason why the Solr server failed to load the core, or {@code null} if it did not fail (or the
     *         information is not available)
     * @throws SolrException when failing to get the information
     * @since 18.10.0RC1
     */
    protected String getCoreLoadFailure(String xwikiCoreName, int solrMajorVersion) throws SolrException
    {
        return null;
    }

    protected Integer getSVersion(XWikiSolrCore core) throws SolrException
    {
        FieldTypeRepresentation fieldType = this.solrSchemaUtils.getFieldTypes(core, false).get(SOLR_TYPENAME_SVERSION);

        if (fieldType == null) {
            return null;
        }

        String value = (String) fieldType.getAttributes().get(SolrSchemaUtils.SOLR_VERSIONFIELDTYPE_VALUE);
        if (value == null) {
            value = (String) fieldType.getAttributes().get(SolrSchemaUtils.SOLR_VERSIONFIELDTYPE_VALUE_LEGACY);
        }

        return NumberUtils.createInteger(value);
    }

    protected void setSVersion(XWikiSolrCore core, long version, boolean add) throws SolrException
    {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put(SolrSchemaUtils.SOLR_FIELD_NAME, SOLR_TYPENAME_SVERSION);
        attributes.put(SolrSchemaUtils.SOLR_FIELD_CLASS, SolrSchemaUtils.SOLR_VERSIONFIELDTYPE_CLASS);
        attributes.put(SolrSchemaUtils.SOLR_VERSIONFIELDTYPE_VALUE, String.valueOf(version));

        FieldTypeDefinition definition = new FieldTypeDefinition();
        definition.setAttributes(attributes);

        this.solrSchemaUtils.setFieldType(core, definition, add);

        // Make sure it's applied
        this.solrSchemaUtils.commit(core);
    }

    /**
     * @return the major version of Solr the cores are designed for, which is the version of Solr bundled with XWiki
     *         (it can be different from the version of the Solr server they are running on)
     * @since 18.10.0RC1
     */
    protected int getCoreMajorVersion()
    {
        return SolrVersion.LATEST.getMajorVersion();
    }

    protected String getSolrCoreSuffix()
    {
        int majorVersion = getCoreMajorVersion();

        return getSolrCoreSuffix(majorVersion);
    }

    protected String getSolrCoreSuffix(int majorVersion)
    {
        // The Solr version was not part of the core name before XWiki Solr 9 support
        if (majorVersion < 9) {
            return "";
        }

        return "_" + majorVersion;
    }

    protected String toSolrCoreName(String xwikiCoreName)
    {
        return toSolrCoreName(xwikiCoreName, getCoreMajorVersion());
    }

    protected String toSolrCoreName(String xwikiCoreName, int majorVersion)
    {
        return xwikiCoreName + getSolrCoreSuffix(majorVersion);
    }

    protected String toXWikiCoreName(String solrCoreName)
    {
        return Strings.CS.removeEnd(solrCoreName, getSolrCoreSuffix());
    }

    /**
     * @since 18.5.0RC1
     */
    protected abstract XWikiSolrCore getCore(String xwikiCoreName, int solrMajorVersion) throws SolrException;

    /**
     * @since 18.5.0RC1
     */
    protected XWikiSolrCore createCore(String xwikiCoreName, boolean isCache) throws SolrException
    {
        // By default, we don't support creating new cores
        throw new SolrException("Creating new cores is not supported");
    }

    @Override
    public void deleteIndex(XWikiSolrCore core) throws SolrException
    {
        // Delete all the documents (which Solr optimizes into a deletion of all the segments of the index)
        try {
            core.getClient().deleteByQuery("*:*");
            core.getClient().commit();
        } catch (Exception e) {
            throw new SolrException("Failed to delete the documents of the Solr core [" + core.getSolrName() + "]",
                e);
        }

        // The index writer still remembers the structure of the fields of the deleted documents (which would make
        // fail the indexing of new documents following an incompatible schema), the reload makes the core start again
        // from an index without any segment
        reloadCore(core);
    }

    /**
     * Reload the passed core, to make it start again from the current state of its configuration and index.
     *
     * @param core the core to reload
     * @throws SolrException when failing to reload the core
     * @since 18.10.0RC1
     */
    protected abstract void reloadCore(XWikiSolrCore core) throws SolrException;
}
