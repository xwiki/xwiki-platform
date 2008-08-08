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
package org.xwiki.rendering.macro.parameter;

import java.util.HashMap;
import java.util.Map;

import org.xwiki.rendering.macro.parameter.classes.MacroParameterClass;
import org.xwiki.rendering.macro.parameter.instances.MacroParameter;

/**
 * Base class to parse and convert macro parameters values into more readable java values (like boolean, int etc.).
 * 
 * @version $Id: $
 */
public abstract class AbstractMacroParameterCollection implements MacroParameterCollection
{
    /**
     * The list of parameters objects containing parameters values.
     */
    private Map<String, MacroParameter< ? >> parameters = new HashMap<String, MacroParameter< ? >>();

    /**
     * The list of parameters descriptors of the macro.
     */
    private Map<String, MacroParameterClass< ? >> parametersClasses = new HashMap<String, MacroParameterClass< ? >>();

    /**
     * @param parametersClasses the list of parameters descriptors of the macro.
     */
    public AbstractMacroParameterCollection(Map<String, MacroParameterClass< ? >> parametersClasses)
    {
        for (Map.Entry<String, MacroParameterClass< ? >> entry : parametersClasses.entrySet()) {
            this.parametersClasses.put(entry.getKey().toLowerCase(), entry.getValue());
        }
    }

    /**
     * @param parametersClasses the list of parameters descriptors of the macro.
     * @param parameters the list of macro parameters to convert to parameters objects.
     */
    public AbstractMacroParameterCollection(Map<String, MacroParameterClass< ? >> parametersClasses,
        Map<String, String> parameters)
    {
        this(parametersClasses);

        load(parameters);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.macro.parameter.MacroParameterCollection#getParametersClasses()
     */
    public Map<String, MacroParameterClass< ? >> getParametersClasses()
    {
        return new HashMap<String, MacroParameterClass< ? >>(this.parametersClasses);
    }

    /**
     * @param parameters load parameters from parser as parameters objects list.
     */
    public void load(Map<String, String> parameters)
    {
        this.parameters.clear();

        for (Map.Entry<String, String> entry : parameters.entrySet()) {
            MacroParameterClass< ? > parameterClass = this.parametersClasses.get(entry.getKey());

            if (parameterClass != null) {
                this.parameters.put(entry.getKey().toLowerCase(), parameterClass.newInstance(entry.getValue()));
            } else {
                // TODO: add debug log here
            }
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.macro.parameter.MacroParameterCollection#getParameter(java.lang.String)
     */
    public <P extends MacroParameter< ? >> P getParameter(String name) throws MacroParameterException
    {
        return (P) this.parameters.get(name.toLowerCase());
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.macro.parameter.MacroParameterCollection#getParameterClass(java.lang.String)
     */
    public <C extends MacroParameterClass< ? >> C getParameterClass(String name) throws MacroParameterException
    {
        return (C) this.parametersClasses.get(name.toLowerCase());
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.macro.parameter.MacroParameterCollection#getParameterValue(java.lang.String)
     */
    public <T> T getParameterValue(String name) throws MacroParameterException
    {
        T value;

        MacroParameter<T> parameter = getParameter(name);

        if (parameter == null) {
            MacroParameterClass<T> pclass = getParameterClass(name);

            if (pclass != null) {
                if (pclass.isRequired()) {
                    throw new MacroParameterRequiredException("A \"" + name + "\" parameter must be specified.");
                } else {
                    value = pclass.getDefaultValue();
                }
            } else {
                throw new MacroParameterNotSupportedException("The parameter \"" + name
                    + "\" is not supported by this macro.");
            }
        } else {
            value = parameter.getValue();
        }

        return value;
    }
}
