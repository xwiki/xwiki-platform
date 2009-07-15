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

import org.xwiki.component.annotation.Requirement;
import org.xwiki.component.logging.AbstractLogEnabled;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.properties.BeanManager;
import org.xwiki.rendering.macro.descriptor.ContentDescriptor;
import org.xwiki.rendering.macro.descriptor.DefaultContentDescriptor;
import org.xwiki.rendering.macro.descriptor.DefaultMacroDescriptor;
import org.xwiki.rendering.macro.descriptor.MacroDescriptor;

/**
 * @param <P> the type of the macro parameters bean
 * @version $Id$
 * @since 1.5M2
 */
public abstract class AbstractMacro<P> extends AbstractLogEnabled implements Macro<P>, Initializable
{
    /**
     * The {@link BeanManager} component.
     */
    @Requirement
    protected BeanManager beanManager;

    /**
     * Macro description used to generate the macro descriptor.
     */
    private String description;

    /**
     * Content descriptor used to generate the macro descriptor.
     */
    private ContentDescriptor contentDescriptor;

    /**
     * Parameter bean class used to generate the macro descriptor.
     */
    private Class< ? > parametersBeanClass;

    /**
     * The descriptor of the macro.
     */
    private MacroDescriptor macroDescriptor;

    /**
     * @see Macro#getPriority()
     */
    private int priority = 1000;

    /**
     * Creates a new {@link Macro} instance.
     * 
     * @param description a string describing this macro.
     */
    public AbstractMacro(String description)
    {
        this.description = description;
        this.contentDescriptor = new DefaultContentDescriptor();
        this.parametersBeanClass = Object.class;
    }
    
    /**
     * Creates a new {@link Macro} instance.
     * 
     * @param description a string describing this macro.
     * @param contentDescriptor {@link ContentDescriptor} for this macro.
     */
    public AbstractMacro(String description, ContentDescriptor contentDescriptor)
    {
        this(description);
        this.contentDescriptor = contentDescriptor;
    }
    
    /**
     * Creates a new {@link Macro} instance.
     * 
     * @param description a string describing this macro.
     * @param parametersBeanClass class of the parameters bean of this macro.
     */
    public AbstractMacro(String description,  Class< ? > parametersBeanClass)
    {
        this(description);
        this.parametersBeanClass = parametersBeanClass;
    }

    /**
     * Creates a new {@link Macro} instance.
     * 
     * @param description string describing this macro.
     * @param contentDescriptor the {@link ContentDescriptor} describing the content of this macro.
     * @param parametersBeanClass class of the parameters bean.
     */
    public AbstractMacro(String description, ContentDescriptor contentDescriptor, Class< ? > parametersBeanClass)
    {
        this(description, contentDescriptor);
        this.parametersBeanClass = parametersBeanClass;
    }

    /**
     * {@inheritDoc}
     */
    public void initialize() throws InitializationException
    {
        setDescriptor(new DefaultMacroDescriptor(description, contentDescriptor, beanManager
            .getBeanDescriptor(parametersBeanClass)));
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

    /**
     * Allows macro classes extending other macro classes to override the macro descriptor with their own.
     * 
     * @param descriptor the overriding descriptor to set
     */
    protected void setDescriptor(MacroDescriptor descriptor)
    {
        this.macroDescriptor = descriptor;
    }
}
