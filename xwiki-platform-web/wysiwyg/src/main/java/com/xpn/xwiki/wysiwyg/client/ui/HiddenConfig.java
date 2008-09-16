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
package com.xpn.xwiki.wysiwyg.client.ui;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Hidden;
import com.xpn.xwiki.wysiwyg.client.plugin.Config;

public class HiddenConfig extends Composite implements Config
{
    private String nameSpace;

    private final FlowPanel container = new FlowPanel();

    private final Map<String, Hidden> params = new HashMap<String, Hidden>();

    private final Map<String, Hidden> flags = new HashMap<String, Hidden>();

    public HiddenConfig()
    {
        this(null);
    }

    public HiddenConfig(String nameSpace)
    {
        this.nameSpace = nameSpace;
        initWidget(container);
    }

    /**
     * {@inheritDoc}
     * 
     * @see Config#getParameter(String)
     */
    public String getParameter(String paramName)
    {
        return getParameter(paramName, null);
    }

    /**
     * {@inheritDoc}
     * 
     * @see Config#getParameter(String, String)
     */
    public String getParameter(String paramName, String defaultValue)
    {
        Hidden hidden = params.get(paramName);
        return hidden == null ? defaultValue : hidden.getValue();
    }

    /**
     * {@inheritDoc}
     * 
     * @see Config#getParameterNames()
     */
    public Set<String> getParameterNames()
    {
        return params.keySet();
    }

    public String setParameter(String paramName, String paramValue)
    {
        Hidden hidden = params.get(paramName);
        if (hidden != null) {
            String previousValue = hidden.getValue();
            hidden.setValue(paramValue);
            return previousValue;
        } else {
            hidden = new Hidden(getQualifiedName(paramName), paramValue);
            params.put(paramName, hidden);
            container.add(hidden);
            return null;
        }
    }

    public void removeParameter(String paramName)
    {
        Hidden hidden = params.get(paramName);
        if (hidden != null) {
            params.remove(paramName);
            container.remove(hidden);
        }
    }

    public void addFlag(String flagName)
    {
        if (!hasFlag(flagName)) {
            Hidden hidden = new Hidden(flagName, nameSpace);
            flags.put(flagName, hidden);
            container.add(hidden);
        }
    }

    public boolean hasFlag(String flagName)
    {
        return flags.containsKey(flagName);
    }

    public void removeFlag(String flagName)
    {
        Hidden hidden = flags.get(flagName);
        if (hidden != null) {
            flags.remove(flagName);
            container.remove(hidden);
        }
    }

    public Set<String> getFlagNames()
    {
        return flags.keySet();
    }

    public String getQualifiedName(String paramName)
    {
        if (nameSpace != null) {
            return nameSpace + "_" + paramName;
        } else {
            return paramName;
        }
    }

    public String getNameSpace()
    {
        return nameSpace;
    }

    public void setNameSpace(String nameSpace)
    {
        if (nameSpace == this.nameSpace || (nameSpace != null && nameSpace.equals(this.nameSpace))) {
            return;
        }
        this.nameSpace = nameSpace;
        // Update parameters
        for (Entry<String, Hidden> entry : params.entrySet()) {
            entry.getValue().setName(getQualifiedName(entry.getKey()));
        }
        // Update flags
        for (Entry<String, Hidden> entry : flags.entrySet()) {
            entry.getValue().setValue(nameSpace);
        }
    }
}
