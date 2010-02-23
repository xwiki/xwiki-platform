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

import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.component.logging.AbstractLogEnabled;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.properties.BeanManager;
import org.xwiki.rendering.macro.descriptor.AbstractMacroDescriptor;
import org.xwiki.rendering.macro.descriptor.ContentDescriptor;
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
     * "Formatting" default macro category.
     */
    public static final String DEFAULT_CATEGORY_FORMATTING = "Formatting";

    /**
     * "Development" default macro category.
     */
    public static final String DEFAULT_CATEGORY_DEVELOPMENT = "Development";

    /**
     * "Content" default macro category.
     */
    public static final String DEFAULT_CATEGORY_CONTENT = "Content";

    /**
     * "Navigation" default macro category.
     */
    public static final String DEFAULT_CATEGORY_NAVIGATION = "Navigation";

    /**
     * The {@link BeanManager} component.
     */
    @Requirement
    protected BeanManager beanManager;

    /**
     * The human-readable macro name (eg "Table of Contents" for the TOC macro).
     */
    private String name;

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
     * The default category under which this macro should be listed.
     */
    private String defaultCategory;

    /**
     * Creates a new {@link Macro} instance.
     *
     * @param name the name of the macro (eg "Table Of Contents" for the TOC macro)
     * @since 2.0M3
     */
    public AbstractMacro(String name)
    {
        this(name, null);
    }

    /**
     * Creates a new {@link Macro} instance.
     * 
     * @param name the name of the macro (eg "Table Of Contents" for the TOC macro)
     * @param description a string describing this macro.
     * @since 2.0M3
     */
    public AbstractMacro(String name, String description)
    {
        this(name, description, null, Object.class);
    }

    /**
     * Creates a new {@link Macro} instance.
     * 
     * @param name the name of the macro (eg "Table Of Contents" for the TOC macro)
     * @param description a string describing this macro.
     * @param contentDescriptor {@link ContentDescriptor} for this macro.
     * @since 2.0M3
     */
    public AbstractMacro(String name, String description, ContentDescriptor contentDescriptor)
    {
        this(name, description, contentDescriptor, Object.class);
    }

    /**
     * Creates a new {@link Macro} instance.
     * 
     * @param name the name of the macro (eg "Table Of Contents" for the TOC macro)
     * @param description a string describing this macro.
     * @param parametersBeanClass class of the parameters bean of this macro.
     * @since 2.0M3
     */
    public AbstractMacro(String name, String description, Class< ? > parametersBeanClass)
    {
        this(name, description, null, parametersBeanClass);
    }

    /**
     * Creates a new {@link Macro} instance.
     * 
     * @param name the name of the macro (eg "Table Of Contents" for the TOC macro)
     * @param description string describing this macro.
     * @param contentDescriptor the {@link ContentDescriptor} describing the content of this macro.
     * @param parametersBeanClass class of the parameters bean.
     * @since 2.0M3
     */
    public AbstractMacro(String name, String description, ContentDescriptor contentDescriptor,
        Class< ? > parametersBeanClass)
    {
        this.name = name;
        this.description = description;
        this.contentDescriptor = contentDescriptor;
        this.parametersBeanClass = parametersBeanClass;
    }

    /**
     * {@inheritDoc}
     */
    public void initialize() throws InitializationException
    {
        MacroId macroId = null;
        // Try to get macro id from component hint - only possible for XWiki Java Macros
        Component annotation = this.getClass().getAnnotation(Component.class);
        if (annotation != null && !"".equals(annotation)) {
            macroId = new MacroId(annotation.value());
        }

        DefaultMacroDescriptor descriptor =
            new DefaultMacroDescriptor(macroId, this.name, this.description, this.contentDescriptor, this.beanManager
                .getBeanDescriptor(this.parametersBeanClass));
        descriptor.setDefaultCategory(this.defaultCategory);
        setDescriptor(descriptor);
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
        return getPriority() - macro.getPriority();
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

    /**
     * Allows sub classes to set the default macro category. This method only has an effect if the internal
     * {@link MacroDescriptor} is of type {@link AbstractMacroDescriptor}.
     * 
     * @param defaultCategory the default macro category to be set.
     */
    protected void setDefaultCategory(String defaultCategory)
    {
        // If setDefaultCategory() method is invoked before macro initialization, this will make sure the macro will
        // have correct default category after initialization.
        this.defaultCategory = defaultCategory;

        // In case if setDefaultCategory() is invoked after macro initialization. Only works if the internal
        // MacroDescriptor is of type AbstractMacroDescriptor.
        if (getDescriptor() instanceof AbstractMacroDescriptor) {
            ((AbstractMacroDescriptor) getDescriptor()).setDefaultCategory(defaultCategory);
        }
    }
}
