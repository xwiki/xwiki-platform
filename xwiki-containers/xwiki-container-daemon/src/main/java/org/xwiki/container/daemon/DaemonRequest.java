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
 *
 */
package org.xwiki.container.daemon;

import java.util.HashMap;
import java.util.Map;

import org.xwiki.container.Request;
import org.xwiki.url.XWikiURL;

/**
 * Even though there's no real user request for in a Daemon we still consider there's a request
 * which corresponds to what triggered the Daemon. Also we need the request so that we can 
 * carry objects in it for the duration of the Daemon's execution. For example we need to store
 * the Velocity Context in the request so that Velocity templates executed during the Daemon's
 * existence can share it.
 */
public class DaemonRequest implements Request
{
    private Map<String, Object> properties = new HashMap<String, Object>();
    
    public Object getProperty(String key)
    {
        return this.properties.get(key);
    }

    public XWikiURL getURL()
    {
        // Since there's no real request for a daemon there's no associated URL either and
        // thus we return null.
        return null;
    }

    public void removeProperty(String key)
    {
        this.properties.remove(key);
    }

    public void setProperty(String key, Object value)
    {
        this.properties.put(key, value);
    }
}
