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
package com.xpn.xwiki.internal;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.configuration.ConfigurationSource;

import com.xpn.xwiki.XWikiConfig;

/**
 * Delegate all {@link XWikiConfig} methods to xwiki.cfg {@link ConfigurationSource}.
 *
 * @version $Id$
 */
public class XWikiConfigDelegate extends XWikiConfig
{
    private ConfigurationSource source;

    /**
     * @param source the {@link ConfigurationSource} to delegate to
     */
    public XWikiConfigDelegate(ConfigurationSource source)
    {
        this.source = source;

        if (this.source instanceof XWikiCfgConfigurationSource) {
            this.defaults = ((XWikiCfgConfigurationSource) this.source).getProperties();
        }
    }

    @Override
    public String getProperty(String key)
    {
        return StringUtils.trim(this.source.getProperty(key, String.class));
    }

    @Override
    public String getProperty(String key, String defaultValue)
    {
        return StringUtils.trim(this.source.getProperty(key, defaultValue));
    }

    @Override
    public synchronized Object put(Object key, Object value)
    {
        if (this.defaults != null) {
            return this.defaults.put(key, value);
        }

        return null;
    }

    @Override
    public synchronized void load(InputStream inStream) throws IOException
    {
        if (this.defaults != null) {
            this.defaults.load(inStream);
        }
    }

    @Override
    public synchronized void load(Reader reader) throws IOException
    {
        if (this.defaults != null) {
            this.defaults.load(reader);
        }
    }

    @Override
    public synchronized void loadFromXML(InputStream in) throws IOException
    {
        if (this.defaults != null) {
            this.defaults.loadFromXML(in);
        }
    }
}
