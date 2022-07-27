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
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.boot.MetadataSources;
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
    private List<URL> configureURLs = new ArrayList<>();

    private List<URL> addURLs = new ArrayList<>();

    private List<String> addResources = new ArrayList<>();

    private List<byte[]> addBytes = new ArrayList<>();

    /**
     * @param metadataSources the custom {@link MetadataSources} instance
     * @param previousConfiguration the configuration to recreate
     */
    public HibernateStoreConfiguration(MetadataSources metadataSources,
        HibernateStoreConfiguration previousConfiguration)
    {
        super(metadataSources);

        // Copy previous configuration
        if (previousConfiguration != null) {
            this.configureURLs = previousConfiguration.configureURLs;
            this.addURLs = previousConfiguration.addURLs;
            this.addBytes.addAll(previousConfiguration.addBytes);

            // Reload previous configuration
            for (URL url : this.configureURLs) {
                super.configure(url);
            }
            for (URL url : this.addURLs) {
                super.addURL(url);
            }
            for (String resource : this.addResources) {
                super.addResource(resource);
            }
            for (byte[] bytes : this.addBytes) {
                addBytes(bytes);
            }
        }
    }

    @Override
    public Configuration configure(URL url) throws HibernateException
    {
        // Remember the configuration
        this.configureURLs.add(url);

        return super.configure(url);
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
        this.addBytes.add(bytes);

        // Load the configuration
        return addBytes(bytes);
    }

    @Override
    public Configuration addResource(String resourceName) throws MappingException
    {
        // Remember the configuration
        this.addResources.add(resourceName);

        return super.addResource(resourceName);
    }

    private Configuration addBytes(byte[] bytes)
    {
        return super.addInputStream(new ByteArrayInputStream(bytes));
    }
}
