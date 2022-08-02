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
package org.xwiki.livedata.internal.livetable;

import java.util.Collections;
import java.util.Enumeration;
import java.util.Map;

import com.xpn.xwiki.web.WrappingXWikiRequest;
import com.xpn.xwiki.web.XWikiRequest;

/**
 * Wraps an XWiki request in order to allow overwriting its parameters.
 * 
 * @version $Id$
 * @since 12.10
 */
class LiveTableRequest extends WrappingXWikiRequest
{
    private final Map<String, String[]> parameters;

    LiveTableRequest(XWikiRequest request, Map<String, String[]> parameters)
    {
        super(request);
        this.parameters = parameters;
    }

    @Override
    public Map<String, String[]> getParameterMap()
    {
        return this.parameters;
    }

    @Override
    public String getParameter(String name)
    {
        return this.parameters.getOrDefault(name, new String[] {null})[0];
    }

    @Override
    public String get(String name)
    {
        return this.getParameter(name);
    }

    @Override
    public String[] getParameterValues(String name)
    {
        return this.parameters.get(name);
    }

    @Override
    public Enumeration<String> getParameterNames()
    {
        return Collections.enumeration(this.parameters.keySet());
    }
}
