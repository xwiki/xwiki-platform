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
package com.xpn.xwiki.internal.store.hibernate;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import org.apache.commons.io.IOUtils;
import org.hibernate.MappingException;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.jaxb.spi.Binding;
import org.hibernate.cfg.Configuration;

/**
 * Extends {@link Configuration} to catch the injected mapping to be able to "replay" it.
 * 
 * @version $Id$
 * @since 14.0RC1
 * @since 13.10.3
 */
public class HibernateStoreConfiguration extends Configuration
{
    private final MetadataSources metadataSources = new MetadataSources();

    /**
     * @param url the URL of the Hibernate configuration file
     * @since 15.3RC1
     */
    public HibernateStoreConfiguration(URL url)
    {
        if (url != null) {
            configure(url);
        }
    }

    @Override
    public Configuration addURL(URL url) throws MappingException
    {
        // Remember the configuration
        this.metadataSources.addURL(url);

        return super.addURL(url);
    }

    @Override
    public Configuration addInputStream(InputStream xmlInputStream) throws MappingException
    {
        // Remember the configuration
        byte[] bytes;
        try {
            bytes = IOUtils.toByteArray(xmlInputStream);
        } catch (IOException e) {
            throw new MappingException("Failed to read the configuration file", e);
        }

        // Load the configuration
        return addBytes(bytes);
    }

    @Override
    public Configuration addResource(String resourceName) throws MappingException
    {
        // Remember the configuration
        this.metadataSources.addResource(resourceName);

        return super.addResource(resourceName);
    }

    private Configuration addBytes(byte[] bytes)
    {
        // Remember the configuration
        this.metadataSources.addInputStream(new ByteArrayInputStream(bytes));

        return super.addInputStream(new ByteArrayInputStream(bytes));
    }

    /**
     * @param metadataSource the source to inject with the current source content
     * @since 15.3RC1
     */
    public void copy(MetadataSources metadataSource)
    {
        for (Binding<?> binding : this.metadataSources.getXmlBindings()) {
            metadataSource.addXmlBinding(binding);
        }
    }
}
