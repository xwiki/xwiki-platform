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
package org.xwiki.job.store.internal.hibernate;

import java.net.URL;
import java.util.EnumSet;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.sql.DataSource;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataBuilder;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.cfgxml.spi.LoadedConfig;
import org.hibernate.boot.registry.BootstrapServiceRegistry;
import org.hibernate.boot.registry.BootstrapServiceRegistryBuilder;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.tool.hbm2ddl.SchemaUpdate;
import org.hibernate.tool.schema.TargetType;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLifecycleException;
import org.xwiki.component.phase.Disposable;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.store.hibernate.HibernateDataSourceProvider;
import org.xwiki.store.hibernate.internal.HibernateCfgXmlLoader;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.internal.store.hibernate.HibernateStore;
import com.xpn.xwiki.store.DatabaseProduct;

/**
 * Minimal Hibernate bootstrap dedicated to job status / logs.
 * <p>
 * It builds its own {@link SessionFactory} with only the {@code jobstatus*.hbm.xml} mappings and ensures that the
 * required tables exist as early as possible (before the main XWiki Hibernate store is initialized).
 * <p>
 * It also explicitly targets the main wiki database/schema (main wiki id is always {@code xwiki}).
 *
 * @version $Id$
 * @since 18.2.0RC1
 */
@Component(roles = JobStatusHibernateStore.class)
@Singleton
public class JobStatusHibernateStore implements Initializable, Disposable
{
    private static final String HBM_JOBSTATUS_DEFAULT = "jobstatus.hbm.xml";

    private static final String HBM_JOBSTATUS_ORACLE = "jobstatus.oracle.hbm.xml";

    private static final String UPDATE_SCHEMA_FAILED_MESSAGE = "Failed to update job status database schema";

    @Inject
    private Logger logger;

    @Inject
    private HibernateDataSourceProvider dataSourceProvider;

    @Inject
    private HibernateStore hibernateStore;

    @Inject
    private HibernateCfgXmlLoader cfgXmlLoader;

    @Inject
    private WikiDescriptorManager wikis;

    private BootstrapServiceRegistry bootstrapServiceRegistry;

    private StandardServiceRegistry standardServiceRegistry;

    private SessionFactory sessionFactory;

    @Override
    public void initialize() throws InitializationException
    {
        try {
            buildSessionFactory();
        } catch (Exception e) {
            throw new InitializationException("Failed to initialize JobStatus Hibernate store", e);
        }
    }

    /**
     * @return the dedicated Hibernate {@link SessionFactory} for job status / logs
     */
    public SessionFactory getSessionFactory()
    {
        return this.sessionFactory;
    }

    /**
     * Explicitly switch the passed {@link Session} to the main wiki database/schema/catalog.
     *
     * @param session the Hibernate session
     */
    public void setMainWiki(Session session) throws XWikiException
    {
        String mainWikiId = this.wikis.getMainWikiId();

        this.hibernateStore.setWiki(session, mainWikiId);
    }

    @Override
    public void dispose() throws ComponentLifecycleException
    {
        if (this.sessionFactory != null) {
            this.sessionFactory.close();
            this.sessionFactory = null;
        }

        if (this.standardServiceRegistry != null) {
            this.standardServiceRegistry.close();
            this.standardServiceRegistry = null;
        }

        if (this.bootstrapServiceRegistry != null) {
            this.bootstrapServiceRegistry.close();
            this.bootstrapServiceRegistry = null;
        }
    }

    private void buildSessionFactory() throws Exception
    {
        URL configurationURL = this.cfgXmlLoader.getConfigurationURL();
        if (configurationURL == null) {
            throw new IllegalStateException("Failed to locate hibernate configuration file");
        }

        this.bootstrapServiceRegistry = new BootstrapServiceRegistryBuilder().build();

        LoadedConfig baseConfiguration =
            this.cfgXmlLoader.loadConfig(this.bootstrapServiceRegistry, configurationURL);

        // Re-use the same DataSource as the main Hibernate store and apply any relevant overrides to the
        // configuration (e.g. connection URL) so that we target the same database/schema.
        DataSource dataSource = this.dataSourceProvider.getDataSource();
        @SuppressWarnings("unchecked")
        Map<String, Object> values = baseConfiguration.getConfigurationValues();
        this.cfgXmlLoader.applySharedDataSourceOverrides(values, dataSource);

        StandardServiceRegistryBuilder registryBuilder =
            new StandardServiceRegistryBuilder(this.bootstrapServiceRegistry);
        registryBuilder.configure(baseConfiguration);

        // Explicitly avoid creating/updating the main mappings; we add only jobstatus mapping resources below.
        this.standardServiceRegistry = registryBuilder.build();

        MetadataSources metadataSources = new MetadataSources(this.standardServiceRegistry);
        metadataSources.addResource(selectJobStatusMapping());

        MetadataBuilder metadataBuilder = metadataSources.getMetadataBuilder();
        this.hibernateStore.setWiki(metadataBuilder, this.wikis.getMainWikiId());

        Metadata metadata = metadataBuilder.build();
        updateDatabase(metadata);

        this.sessionFactory = metadata.getSessionFactoryBuilder().build();
    }

    private void updateDatabase(Metadata metadata)
    {
        SchemaUpdate updater = new SchemaUpdate();
        updater.execute(EnumSet.of(TargetType.DATABASE), metadata);

        if (!updater.getExceptions().isEmpty()) {
            for (Object exception : updater.getExceptions()) {
                if (exception instanceof Exception e) {
                    this.logger.error(e.getMessage(), e);
                } else {
                    this.logger.error("{}", exception);
                }
            }

            Object first = updater.getExceptions().getFirst();
            if (first instanceof Throwable throwable) {
                throw new IllegalStateException(UPDATE_SCHEMA_FAILED_MESSAGE, throwable);
            }

            throw new IllegalStateException(UPDATE_SCHEMA_FAILED_MESSAGE);
        }
    }

    private String selectJobStatusMapping()
    {
        return this.hibernateStore.getDatabaseProductName() == DatabaseProduct.ORACLE ? HBM_JOBSTATUS_ORACLE
            : HBM_JOBSTATUS_DEFAULT;
    }
}
