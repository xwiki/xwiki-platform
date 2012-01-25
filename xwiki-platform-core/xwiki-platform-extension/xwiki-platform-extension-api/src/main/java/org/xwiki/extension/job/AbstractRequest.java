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
package org.xwiki.extension.job;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Base class for {@link Request} implementations.
 * 
 * @version $Id$
 */
public abstract class AbstractRequest implements Request
{
    /**
     * The properties.
     */
    private Map<String, Object> properties = new HashMap<String, Object>();

    /**
     * Default constructor.
     */
    public AbstractRequest()
    {

    }

    /**
     * @param request the request to copy
     */
    public AbstractRequest(Request request)
    {
        for (String key : request.getPropertyNames()) {
            setProperty(key, request.getProperty(key));
        }
    }

    @Override
    public boolean isRemote()
    {
        return this.<Boolean> getProperty(PROPERTY_REMOTE, false);
    }

    /**
     * @param remote indicate if the job has been triggered by a remote event
     */
    public void setRemote(boolean remote)
    {
        setProperty(PROPERTY_REMOTE, remote);
    }

    /**
     * @param key the name of the property
     * @param value the value of the property
     */
    public void setProperty(String key, Object value)
    {
        this.properties.put(key, value);
    }

    @Override
    public <T> T getProperty(String key)
    {
        return getProperty(key, null);
    }

    @Override
    public <T> T getProperty(String key, T def)
    {
        Object value = this.properties.get(key);

        return value != null ? (T) value : def;
    }

    @Override
    public Collection<String> getPropertyNames()
    {
        return this.properties.keySet();
    }
}
