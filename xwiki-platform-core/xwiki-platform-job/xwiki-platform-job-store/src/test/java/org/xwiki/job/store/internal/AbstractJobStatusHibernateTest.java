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
package org.xwiki.job.store.internal;

import java.io.File;
import java.io.InputStream;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.xwiki.test.annotation.BeforeComponent;
import org.xwiki.test.junit5.XWikiTempDir;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;

import com.xpn.xwiki.internal.store.hibernate.HibernateConfiguration;
import com.xpn.xwiki.internal.store.hibernate.HibernateStore;
import com.xpn.xwiki.store.DatabaseProduct;

import static org.mockito.Mockito.when;

/**
 * Abstract base class for Hibernate-backed job status component integration tests.
 * <p>
 * Provides the shared component registration (via {@link JobStatusHibernateComponentList}), mock fields, Hibernate
 * configuration setup and a {@link #configureAdditionalComponents()} hook for subclass-specific setup.
 *
 * @version $Id$
 */
@JobStatusHibernateComponentList
abstract class AbstractJobStatusHibernateTest
{
    @MockComponent
    protected HibernateStore hibernateStore;

    @MockComponent
    protected WikiDescriptorManager wikiDescriptorManager;

    @MockComponent
    protected HibernateConfiguration hibernateConfiguration;

    @XWikiTempDir
    protected File tmpDir;

    @BeforeComponent
    void configureHibernate() throws Exception
    {
        when(this.hibernateConfiguration.getPath()).thenReturn(createHibernateConfigurationFile().toString());
        when(this.wikiDescriptorManager.getMainWikiId()).thenReturn("xwiki");
        when(this.hibernateStore.getDatabaseProductName()).thenReturn(DatabaseProduct.HSQLDB);
        configureAdditionalComponents();
    }

    /**
     * Hook for subclasses to perform additional mock/component setup before the component manager is initialized.
     * Called from within the shared {@link BeforeComponent} method.
     *
     * @throws Exception on error
     */
    protected void configureAdditionalComponents() throws Exception
    {
    }

    private Path createHibernateConfigurationFile() throws Exception
    {
        Path target = this.tmpDir.toPath().resolve("hibernate-test.cfg.xml");
        Files.createDirectories(target.getParent());

        String jdbcUrl = "jdbc:hsqldb:mem:jobstore_" + UUID.randomUUID();

        VelocityContext context = new VelocityContext();
        context.put("xwikiDbConnectionUrl", jdbcUrl);
        context.put("xwikiDbConnectionUsername", "sa");
        context.put("xwikiDbConnectionPassword", "");
        context.put("xwikiDbDbcpMaxTotal", "");
        // We need to provide existing mapping files as otherwise Hibernate fails to parse the configuration.
        // However, these mapping files won't be used by the job status Hibernate store.
        context.put("xwikiDbHbmCommonExtraMappings", "xwiki.hbm.xml");
        context.put("xwikiDbHbmDefaultExtraMappings", "xwiki.hbm.xml");
        context.put("xwikiDbHbmXwiki", "xwiki.hbm.xml");
        context.put("xwikiDbHbmFeeds", "feeds.hbm.xml");

        Velocity.init();
        try (InputStream stream = getClass().getClassLoader().getResourceAsStream("hibernate.cfg.xml.vm");
            Writer writer = Files.newBufferedWriter(target, StandardCharsets.UTF_8))
        {
            String template = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            Velocity.evaluate(context, writer, "hibernate.cfg.xml.vm", template);
        }

        return target;
    }
}
