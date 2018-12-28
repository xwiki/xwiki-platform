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

import org.apache.commons.lang3.StringUtils;
import org.dom4j.Attribute;
import org.dom4j.Element;
import org.hibernate.HibernateException;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.util.xml.XmlDocument;
import org.hibernate.service.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.service.spi.Stoppable;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.DisposePriority;
import org.xwiki.component.manager.ComponentLifecycleException;
import org.xwiki.component.phase.Disposable;

import com.xpn.xwiki.util.Util;

/**
 * Default implementation for {@link HibernateSessionFactory}.
 *
 * @version $Id$
 * @since 2.0M1
 */
@Component
@Singleton
@DisposePriority(10000)
public class DefaultHibernateSessionFactory implements HibernateSessionFactory, Disposable
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

    @Override
    public void dispose() throws ComponentLifecycleException
    {
        // TODO: See https://jira.xwiki.org/browse/XWIKI-471. Note that this code currently duplicates
        // XWikiHibernateBaseStore.shutdownHibernate() which is not public and getting a Store implementation from
        // this component is very difficult since there's no XWikiContext and the store used is defined in xwiki.cfg
        SessionFactory sessionFactory = getSessionFactory();
        if (sessionFactory != null) {
            // Close all connections in the Connection Pool.
            // Note that we need to do the cast because this is how Hibernate suggests to get the Connection Provider.
            // See http://bit.ly/QAJXlr
            ConnectionProvider provider = ((SessionFactoryImplementor) sessionFactory).getConnectionProvider();
            // If the user has specified a Data Source we shouldn't close it. Fortunately the way Hibernate works is
            // the following: if the user has configured Hibernate to use a Data Source then Hibernate will use
            // the DatasourceConnectionProvider class which has a close() method that doesn't do anything...
            if (provider instanceof Stoppable) {
                ((Stoppable) provider).stop();
            }
        }
    }

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

        @Override
        public void add(XmlDocument metadataXml)
        {
            Element basePropertyElement = selectChildClassMappingElement(metadataXml.getDocumentTree().getRootElement(),
                "class", "com.xpn.xwiki.objects.BaseProperty");
            if (basePropertyElement != null) {
                decorateDBStringListMapping(basePropertyElement);
            }

            super.add(metadataXml);
        }

        /**
         * Select the class definition element that is an immediate child element of the given element.
         *
         * @param parentElement The parent element.
         * @param elementName The element name for the given class definition ("class" or "joined-subclass").
         * @param className The qualified class name to match.
         */
        private Element selectChildClassMappingElement(Element parentElement, String elementName, String className)
        {
            for (Object elementObj : parentElement.elements(elementName)) {
                if (elementObj instanceof Element) {
                    Element element = (Element) elementObj;
                    Attribute attribute = element.attribute("name");
                    if (attribute != null && className.equals(attribute.getValue())) {
                        return element;
                    }
                }
            }
            return null;
        }

        /**
         * Decorate the hibernate mapping for the class DBStringListProperty with a collection-type attribute.
         *
         * @param basePropertyElement The element of the base property class mapping.
         */
        private void decorateDBStringListMapping(Element basePropertyElement)
        {
            final String className = "com.xpn.xwiki.objects.DBStringListProperty";
            final String collectionType = "com.xpn.xwiki.internal.objects.ListPropertyCollectionType";

            Element listClassElement = selectChildClassMappingElement(basePropertyElement, "joined-subclass",
                className);

            if (listClassElement != null) {
                Element listElement = listClassElement.element("list");
                if (listElement != null) {
                    listElement.addAttribute("collection-type",
                        collectionType);
                    DefaultHibernateSessionFactory.this.logger.debug(
                        "Added collection-type attribute [{}] to hibernate mapping for [{}].", collectionType,
                        className);
                }
            }
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
         * Replace variables defined in Hibernate properties using the <code>${variable}</code> notation. Note that
         * right now the only variable being replaced is {@link #PROPERTY_PERMANENTDIRECTORY} and replaced with the
         * value coming from the XWiki configuration.
         *
         * @param hibernateConfiguration the Hibernate Configuration object that we're evaluating
         */
        private void replaceVariables(Configuration hibernateConfiguration)
        {
            String url = hibernateConfiguration.getProperty(Environment.URL);
            if (StringUtils.isEmpty(url)) {
                return;
            }

            // Replace variables
            if (url.matches(".*\\$\\{.*\\}.*")) {
                String newURL = StringUtils.replace(url, String.format("${%s}", PROPERTY_PERMANENTDIRECTORY),
                    DefaultHibernateSessionFactory.this.environment.getPermanentDirectory().getAbsolutePath());

                // Set the new URL
                hibernateConfiguration.setProperty(Environment.URL, newURL);
                DefaultHibernateSessionFactory.this.logger.debug("Resolved Hibernate URL [{}] to [{}]", url, newURL);
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
