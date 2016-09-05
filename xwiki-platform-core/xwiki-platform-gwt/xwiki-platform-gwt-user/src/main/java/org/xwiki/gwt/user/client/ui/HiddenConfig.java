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
package org.xwiki.gwt.user.client.ui;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.xwiki.gwt.user.client.Config;

import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Hidden;

/**
 * A group of {@link Hidden} widgets that can be used to send data to the server when the HTML form is submitted. An
 * {@link Hidden} widget can serve two roles. It's either a flag or a parameter.
 * <p>
 * Flags are global with respect to the host page. Another {@link HiddenConfig} can have the same flag. As a consequence
 * the request will have a parameter with the name of the flag. The value of this request parameter will be the list of
 * {@link #nameSpace} of {@link HiddenConfig} that had the flag set when the form was submitted.
 * <p>
 * Parameters are local with respect to a {@link HiddenConfig}. Their names are prefixed by the {@link #nameSpace}. For
 * each parameter set on a {@link HiddenConfig} there will be a request parameter with the same name and the same value.
 * 
 * @version $Id$
 */
public class HiddenConfig extends Composite implements Config
{
    /**
     * The name of this {@link HiddenConfig}, used for prefixing the names of all parameters.
     */
    private String nameSpace;

    /**
     * The panel that contains all the {@link Hidden} widgets.
     */
    private final FlowPanel container = new FlowPanel();

    /**
     * The map of parameters. The key is the unqualified name of the parameter.
     */
    private final Map<String, Hidden> params = new HashMap<String, Hidden>();

    /**
     * The map of flags. The key is the name of the flag.
     */
    private final Map<String, Hidden> flags = new HashMap<String, Hidden>();

    /**
     * Creates a new group of {@link Hidden} widgets without specifying the {@link #nameSpace}.
     */
    public HiddenConfig()
    {
        this(null);
    }

    /**
     * Creates a new group of {@link Hidden} widgets.
     * 
     * @param nameSpace The name of this group.
     */
    public HiddenConfig(String nameSpace)
    {
        this.nameSpace = nameSpace;
        initWidget(container);
    }

    @Override
    public String getParameter(String paramName)
    {
        return getParameter(paramName, null);
    }

    @Override
    public String getParameter(String paramName, String defaultValue)
    {
        Hidden hidden = params.get(paramName);
        return hidden == null ? defaultValue : hidden.getValue();
    }

    @Override
    public Set<String> getParameterNames()
    {
        return params.keySet();
    }

    /**
     * Sets the value of the specified parameter. If the parameter doesn't exists it is added.
     * 
     * @param paramName The name of the parameter.
     * @param paramValue The value of the parameter.
     * @return The previous value of the given parameter, or null if this parameter is new.
     */
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

    /**
     * Removes the specified parameter. It does nothing if the given parameter doesn't exist.
     * 
     * @param paramName The name of the parameter to be removed.
     */
    public void removeParameter(String paramName)
    {
        Hidden hidden = params.get(paramName);
        if (hidden != null) {
            params.remove(paramName);
            container.remove(hidden);
        }
    }

    /**
     * Sets the specified flag. If the flag doesn't exist then it is added.
     * 
     * @param flagName The name of the flag to be set.
     */
    public void addFlag(String flagName)
    {
        if (!hasFlag(flagName)) {
            Hidden hidden = new Hidden(flagName, nameSpace);
            flags.put(flagName, hidden);
            container.add(hidden);
        }
    }

    /**
     * Tests of the specified flag has been set.
     * 
     * @param flagName The name of the flag to test.
     * @return true if the given flag has been set.
     */
    public boolean hasFlag(String flagName)
    {
        return flags.containsKey(flagName);
    }

    /**
     * Removes the specified flag. It does nothing if the flag doesn't exist.
     * 
     * @param flagName The name of the flag to be removed.
     */
    public void removeFlag(String flagName)
    {
        Hidden hidden = flags.get(flagName);
        if (hidden != null) {
            flags.remove(flagName);
            container.remove(hidden);
        }
    }

    /**
     * @return The set of all flag names set on this object.
     */
    public Set<String> getFlagNames()
    {
        return flags.keySet();
    }

    /**
     * The qualified form of a parameter's name is obtained by prefixing the name of the parameter with
     * {@link #nameSpace}.
     * 
     * @param paramName The name of a parameter.
     * @return The qualified form of the given parameter name.
     */
    public String getQualifiedName(String paramName)
    {
        if (nameSpace != null) {
            return nameSpace + "_" + paramName;
        } else {
            return paramName;
        }
    }

    /**
     * @return The name of this group of {@link Hidden}.
     * @see #nameSpace
     */
    public String getNameSpace()
    {
        return nameSpace;
    }

    /**
     * Sets the name of this group of {@link Hidden}. As a consequence all flags and parameters are updated.
     * 
     * @param nameSpace The new name for this object.
     */
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
