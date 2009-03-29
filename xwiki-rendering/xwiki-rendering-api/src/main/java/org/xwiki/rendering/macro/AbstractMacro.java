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
package org.xwiki.rendering.macro;

import org.apache.commons.beanutils.ConvertUtils;
import org.apache.commons.beanutils.Converter;
import org.xwiki.component.logging.AbstractLogEnabled;
import org.xwiki.rendering.macro.descriptor.MacroDescriptor;

/**
 * @param <P> the type of the macro parameters bean
 * @version $Id$
 * @since 1.5M2
 */
public abstract class AbstractMacro<P> extends AbstractLogEnabled implements Macro<P>
{
    /**
     * The descriptor of the macro.
     */
    private MacroDescriptor macroDescriptor;

    /**
     * @see Macro#getPriority()
     */
    private int priority = 1000;

    /**
     * @param macroDescriptor the {@link MacroDescriptor}.
     */
    public AbstractMacro(MacroDescriptor macroDescriptor)
    {
        this.macroDescriptor = macroDescriptor;
    }

    /**
     * Register a converter for a specific type used by the macro parameters bean.
     * <p>
     * Note: each enum type used has to be registered because BeanUtil does not support generic types.
     * 
     * @param converter the BeanUtil {@link Converter}
     * @param clazz the class for which to assign the {@link Converter}
     */
    protected void registerConverter(Converter converter, Class< ? > clazz)
    {
        ConvertUtils.register(converter, clazz);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.macro.Macro#getPriority()
     */
    public int getPriority()
    {
        return this.priority;
    }

    /**
     * @param priority the macro priority to use (lower means execute before others) 
     */
    public void setPriority(int priority)
    {
        this.priority = priority;
    }
    
    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.macro.Macro#getDescriptor()
     */
    public MacroDescriptor getDescriptor()
    {
        return this.macroDescriptor;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    public int compareTo(Macro< ? > macro)
    {
        if (getPriority() != macro.getPriority()) {
            return getPriority() - macro.getPriority();
        }
        return this.getClass().getSimpleName().compareTo(macro.getClass().getSimpleName());
    }
}
