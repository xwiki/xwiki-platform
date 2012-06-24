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
package com.xpn.xwiki.store.hibernate;

import java.io.File;
import java.io.InputStream;
import java.net.URL;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.tools.ant.util.StringUtils;
import org.hibernate.HibernateException;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;

import com.xpn.xwiki.util.Util;

/**
 * Default implementation for {@link HibernateSessionFactory}.
 * 
 * @version $Id$
 * @since 2.0M1
 */
// TODO: This was coded by Artem. Find out why we need this as a component.
@Component
@Singleton
public class DefaultHibernateSessionFactory implements HibernateSessionFactory
{
    /**
     * The logger to log.
     */
    @Inject
    private Logger logger;

    /**
     * Used to get Environment permanent directory to evaluate Hibernate properties.
     */
    @Inject
    private org.xwiki.environment.Environment environment;

    /**
     * Hibernate configuration object.
     */
    private Configuration configuration = new Configuration()
    {
        /**
         * The name of the property for configuring the environment permanent directory.
         */
        private static final String PROPERTY_PERMANENTDIRECTORY = "environment.permanentDirectory";

        private static final long serialVersionUID = 1L;

        /**
         * Whether the Hibernate Configuration has already been initialized or not. We do this so that the Hibernate
         * {@link org.hibernate.cfg.Configuration#configure()} methods can be called several times in a row without
         * causing some Duplicate Mapping errors, see our overridden {@link #getConfigurationInputStream(String)} below.
         */
        private boolean isConfigurationInitialized;

        @Override
        public Configuration configure() throws HibernateException
        {
            Configuration configuration;
            if (this.isConfigurationInitialized) {
                configuration = this;
            } else {
                configuration = super.configure();
                this.isConfigurationInitialized = true;
            }
            replaceVariables(configuration);
            return configuration;
        }

        @Override
        public Configuration configure(String resource) throws HibernateException
        {
            Configuration configuration;
            if (this.isConfigurationInitialized) {
                configuration = this;
            } else {
                configuration = super.configure(resource);
                this.isConfigurationInitialized = true;
            }
            replaceVariables(configuration);
            return configuration;
        }

        @Override
        public Configuration configure(URL url) throws HibernateException
        {
            Configuration configuration;
            if (this.isConfigurationInitialized) {
                configuration = this;
            } else {
                configuration = super.configure(url);
                this.isConfigurationInitialized = true;
            }
            replaceVariables(configuration);
            return configuration;
        }

        @Override
        public Configuration configure(File configFile) throws HibernateException
        {
            Configuration configuration;
            if (this.isConfigurationInitialized) {
                configuration = this;
            } else {
                configuration = super.configure(configFile);
                this.isConfigurationInitialized = true;
            }
            replaceVariables(configuration);
            return configuration;
        }

        // There is no #configure(InputStream) so we use #configure(String) and override #getConfigurationInputStream
        @Override
        protected InputStream getConfigurationInputStream(String resource) throws HibernateException
        {
            InputStream stream = Util.getResourceAsStream(resource);
            if (stream == null) {
                throw new HibernateException(String.format("Can't find [%s] for hibernate configuration", resource));
            }
            return stream;
        }

        /**
         * Replace variables defined in Hibernate properties using the {@code ${variable}} notation. Note that right
         * now the only variable being replaced is {@link #PROPERTY_PERMANENTDIRECTORY} and replaced with the value
         * coming from the XWiki configuration.
         *
         * @param hibernateConfiguration the Hibernate Configuration object that we're evaluating
         */
        private void replaceVariables(Configuration hibernateConfiguration)
        {
            String url = hibernateConfiguration.getProperty(Environment.URL);

            // Replace variables
            if (url.matches(".*\\$\\{.*\\}.*")) {
                String newURL = StringUtils.replace(url, String.format("${%s}", PROPERTY_PERMANENTDIRECTORY),
                    environment.getPermanentDirectory().getAbsolutePath());

                // Set the new URL
                hibernateConfiguration.setProperty(Environment.URL, newURL);
                logger.debug("Resolved Hibernate URL [{}] to [{}]", url, newURL);
            }
        }
    };

    /**
     * Real Hibernate session factory.
     */
    private SessionFactory sessionFactory;

    @Override
    public Configuration getConfiguration()
    {
        return this.configuration;
    }

    @Override
    public SessionFactory getSessionFactory()
    {
        return this.sessionFactory;
    }

    @Override
    public void setSessionFactory(SessionFactory sessionFactory)
    {
        this.sessionFactory = sessionFactory;
    }
}
