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

import org.hibernate.HibernateException;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
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
public class DefaultHibernateSessionFactory implements HibernateSessionFactory
{
    /**
     * Hibernate configuration object.
     */
    private Configuration configuration = new Configuration()
    {
        private static final long serialVersionUID = 1L;

        /**
         * Whether the Hibernate Configuration has alreayd been initialized or not. We do this so that the
         * Hibernate {@link org.hibernate.cfg.Configuration#configure()} methods can be called several times in a
         * row without causing some Duplicate Mapping errors, see our overridden
         * {@link #getConfigurationInputStream(String)} below.
         */
        private boolean isConfigurationInitialized;

        /**
         * {@inheritDoc}
         * @see org.hibernate.cfg.Configuration#configure()
         */
        @Override public Configuration configure() throws HibernateException
        {
            Configuration configuration;
            if (this.isConfigurationInitialized) {
                configuration = this;
            } else {
                configuration = super.configure();
                this.isConfigurationInitialized = true;
            }
            return configuration;
        }

        /**
         * {@inheritDoc}
         * @see org.hibernate.cfg.Configuration#configure(String) 
         */
        @Override public Configuration configure(String resource) throws HibernateException
        {
            Configuration configuration;
            if (this.isConfigurationInitialized) {
                configuration = this;
            } else {
                configuration = super.configure(resource);
                this.isConfigurationInitialized = true;
            }
            return configuration;
        }

        /**
         * {@inheritDoc}
         * @see org.hibernate.cfg.Configuration#configure(java.net.URL)
         */
        @Override public Configuration configure(URL url) throws HibernateException
        {
            Configuration configuration;
            if (this.isConfigurationInitialized) {
                configuration = this;
            } else {
                configuration = super.configure(url);
                this.isConfigurationInitialized = true;
            }
            return configuration;
        }

        /**
         * {@inheritDoc}
         * @see org.hibernate.cfg.Configuration#configure(java.io.File) 
         */
        @Override public Configuration configure(File configFile) throws HibernateException
        {
            Configuration configuration;
            if (this.isConfigurationInitialized) {
                configuration = this;
            } else {
                configuration = super.configure(configFile);
                this.isConfigurationInitialized = true;
            }
            return configuration;
        }

        // there is no #configure(InputStream) so we use #configure(String) and override #getConfigurationInputStream
        @Override
        protected InputStream getConfigurationInputStream(String resource) throws HibernateException
        {
            InputStream stream = Util.getResourceAsStream(resource);
            if (stream == null) {
                throw new HibernateException("Can't find [" + resource + "] for hibernate configuration");
            }
            return stream;
        }
    };

    /**
     * Real Hibernate session factory.
     */
    private SessionFactory sessionFactory;

    /**
     * {@inheritDoc}
     * @see HibernateSessionFactory#getConfiguration()
     */
    public Configuration getConfiguration()
    {
        return this.configuration;
    }

    /**
     * {@inheritDoc}
     * @see HibernateSessionFactory#getSessionFactory()
     */
    public SessionFactory getSessionFactory()
    {
        return this.sessionFactory;
    }

    /**
     * {@inheritDoc}
     * @see HibernateSessionFactory#setSessionFactory(SessionFactory)
     */
    public void setSessionFactory(SessionFactory sessionFactory)
    {
        this.sessionFactory = sessionFactory;
    }
}
