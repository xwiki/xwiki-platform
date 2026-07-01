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
package org.xwiki.configuration.internal;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.commons.configuration2.Configuration;
import org.xwiki.component.phase.Initializable;
import org.xwiki.configuration.ConfigurationSource;

/**
 * Wrap a Commons Configuration instance into a XWiki {@link ConfigurationSource}. This allows us to reuse the
 * <a href= "http://commons.apache.org/configuration/">numerous types of Configuration<a/> provided by Commons
 * Configuration (properties file, XML files, databases, etc).
 * 
 * @version $Id$
 * @since 1.6M1
 */
public abstract class AbstractCommonsConfigurationSource extends AbstractPropertiesConfigurationSource
    implements Initializable
{
    protected final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    private Configuration configuration;

    protected AbstractCommonsConfigurationSource()
    {
        // Enable system overwrite
        this.systemOverwriteEnabled = true;
    }

    protected Configuration getConfiguration()
    {
        return this.configuration;
    }

    protected void setConfiguration(Configuration configuration)
    {
        this.configuration = configuration;
    }

    @Override
    @SuppressWarnings("unchecked")
    protected <T> T getPropertyInternal(String key)
    {
        this.lock.readLock().lock();

        try {
            return (T) getConfiguration().getProperty(key);
        } finally {
            this.lock.readLock().unlock();
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getPropertyInternal(String key, T defaultValue)
    {
        T result;
        if (containsKey(key)) {
            if (defaultValue != null) {
                return getProperty(key, (Class<T>) defaultValue.getClass());
            } else {
                return getProperty(key);
            }
        } else {
            result = defaultValue;
        }

        return result;
    }

    @Override
    @SuppressWarnings("unchecked")
    protected <T> T getPropertyInternal(String key, Class<T> valueClass)
    {
        T result;

        try {
            if (String.class == valueClass) {
                result = (T) getString(key);
            } else if (List.class.isAssignableFrom(valueClass)) {
                result = (T) getList(key);
            } else if (Properties.class.isAssignableFrom(valueClass)) {
                result = (T) getProperties(key);
            } else {
                result = getConvertedProperty(key, valueClass, null);
            }
        } catch (org.apache.commons.configuration2.ex.ConversionException
            | org.xwiki.properties.converter.ConversionException e) {
            throw new org.xwiki.configuration.ConversionException(
                "Key [" + key + "] is not compatible with type [" + valueClass.getName() + "]", e);
        }

        return result;
    }

    private String getString(String key)
    {
        this.lock.readLock().lock();

        try {
            return getConfiguration().getString(key);
        } finally {
            this.lock.readLock().unlock();
        }
    }

    private List<Object> getList(String key)
    {
        this.lock.readLock().lock();

        try {
            return getConfiguration().getList(key);
        } finally {
            this.lock.readLock().unlock();
        }
    }

    private Properties getProperties(String key)
    {
        this.lock.readLock().lock();

        try {
            return getConfiguration().getProperties(key);
        } finally {
            this.lock.readLock().unlock();
        }
    }

    @Override
    protected boolean containsKeyInternal(String key)
    {
        this.lock.readLock().lock();

        try {
            return getConfiguration().containsKey(key);
        } finally {
            this.lock.readLock().unlock();
        }
    }

    @Override
    protected List<String> getKeysInternal()
    {
        return getKeysInternal("");
    }

    @Override
    protected List<String> getKeysInternal(String prefix)
    {
        this.lock.readLock().lock();

        try {
            List<String> keysList = new ArrayList<>();
            for (Iterator<String> keys = getConfiguration().getKeys(); keys.hasNext();) {
                String key = keys.next();
                if (key.startsWith(prefix)) {
                    keysList.add(key);
                }
            }

            return keysList;
        } finally {
            this.lock.readLock().unlock();
        }
    }

    @Override
    protected boolean isEmptyInternal()
    {
        this.lock.readLock().lock();

        try {
            return getConfiguration().isEmpty();
        } finally {
            this.lock.readLock().unlock();
        }
    }

    @Override
    protected boolean isEmptyInternal(String prefix)
    {
        this.lock.readLock().lock();

        try {
            for (Iterator<String> keys = getConfiguration().getKeys(); keys.hasNext();) {
                String key = keys.next();
                if (key.startsWith(prefix)) {
                    return false;
                }
            }

            return true;
        } finally {
            this.lock.readLock().unlock();
        }
    }
}
