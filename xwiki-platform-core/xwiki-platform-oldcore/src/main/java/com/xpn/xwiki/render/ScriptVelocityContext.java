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
package com.xpn.xwiki.render;

import java.util.Map;
import java.util.Set;

import javax.script.Bindings;
import javax.script.ScriptContext;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.runtime.directive.Scope;

/**
 * Maintains the current ScriptContext in sync with any modification of the VelocityContext.
 * 
 * @version $Id$
 * @since 8.3M1
 */
public class ScriptVelocityContext extends VelocityContext
{
    private final Set<String> reservedBindings;

    private ScriptContext scriptContext;

    /**
     * @param parent the initial Velocity context
     * @param reservedBindings the binding that should not be synchronized
     */
    public ScriptVelocityContext(VelocityContext parent, Set<String> reservedBindings)
    {
        super(parent);

        this.reservedBindings = reservedBindings;
    }

    /**
     * @return the current script context
     */
    public ScriptContext getScriptContext()
    {
        return this.scriptContext;
    }

    /**
     * @param scriptContext the current script context
     */
    public void setScriptContext(ScriptContext scriptContext)
    {
        this.scriptContext = scriptContext;

        copyScriptContext(ScriptContext.GLOBAL_SCOPE);
        copyScriptContext(ScriptContext.ENGINE_SCOPE);
    }

    private void copyScriptContext(int scope)
    {
        Bindings bindings = this.scriptContext.getBindings(scope);
        if (bindings != null) {
            for (Map.Entry<String, Object> entry : bindings.entrySet()) {
                if (!this.reservedBindings.contains(entry.getKey())) {
                    Object currentValue = get(entry.getKey());
                    // Don't replace internal Velocity bindings
                    if (!(currentValue instanceof Scope)) {
                        put(entry.getKey(), entry.getValue());
                    }
                }
            }
        }
    }

    // VelocityContext

    @Override
    public Object internalPut(String key, Object value)
    {
        try {
            return super.internalPut(key, value);
        } finally {
            if (!this.reservedBindings.contains(key)) {
                this.scriptContext.setAttribute(key, value, ScriptContext.ENGINE_SCOPE);
            }
        }
    }

    @Override
    public Object internalRemove(String key)
    {
        try {
            return super.internalRemove(key);
        } finally {
            if (key instanceof String && !this.reservedBindings.contains(key)) {
                this.scriptContext.removeAttribute((String) key, ScriptContext.ENGINE_SCOPE);
            }
        }
    }
}
