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
package org.xwiki.component.wiki.internal;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.xwiki.component.descriptor.ComponentDescriptor;
import org.xwiki.component.wiki.WikiComponent;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.rendering.block.XDOM;

/**
 * Default implementation of a wiki component definition.
 * 
 * @version $Id$
 * @since 4.2M3
 */
public class DefaultWikiComponent implements WikiComponent
{
    /**
     * @see {@link #getDocumentReference()}
     */
    private DocumentReference documentReference;

    /**
     * @see {@link #getHandledMethods()}
     */
    private Map<String, XDOM> handledMethods = new HashMap<String, XDOM>();

    /**
     * @see {@link #getRoleType()}
     */
    private Type roleType;

    /**
     * @see {@link #getRoleHint()}
     */
    private String roleHint;

    /**
     * @see {@link #getImplementedInterfaces()}
     */
    private List<Class< ? >> implementedInterfaces = new ArrayList<Class< ? >>();

    /**
     * @see {@link #getDependencies()}
     */
    private Map<String, ComponentDescriptor> dependencies = new HashMap<String, ComponentDescriptor>();

    /**
     * Constructor of this component.
     * 
     * @param reference the document holding the component definition
     * @param roleType the role Type implemented
     * @param roleHint the role hint for this role implementation
     */
    public DefaultWikiComponent(DocumentReference reference, Type roleType, String roleHint)
    {
        this.documentReference = reference;
        this.roleType = roleType;
        this.roleHint = roleHint;
    }

    @Override
    public DocumentReference getDocumentReference()
    {
        return this.documentReference;
    }

    @Override
    public Map<String, XDOM> getHandledMethods()
    {
        return this.handledMethods;
    }

    @Override
    public Type getRoleType()
    {
        return this.roleType;
    }

    @Override
    public String getRoleHint()
    {
        return this.roleHint;
    }

    @Override
    public List<Class< ? >> getImplementedInterfaces()
    {
        return this.implementedInterfaces;
    }

    @Override
    public Map<String, ComponentDescriptor> getDependencies()
    {
        return this.dependencies;
    }

    /**
     * Sets the handled method.
     * 
     * @see {@link #getHandledMethods()}
     * 
     * @param methods the methods this component will handle
     */
    public void setHandledMethods(Map<String, XDOM> methods)
    {
        this.handledMethods = methods;
    }

    /**
     * Sets the implemented interfaces.
     *
     * @see {@link #getImplementedInterfaces()}
     *
     * @param interfaces the interfaces this component implements
     */
    public void setImplementedInterfaces(List<Class< ? >> interfaces)
    {
        this.implementedInterfaces = interfaces;
    }

    /**
     * Sets the component dependencies.
     *
     * @param dependencies the dependencies of this component
     */
    public void setDependencies(Map<String, ComponentDescriptor> dependencies)
    {
        this.dependencies = dependencies;
    }
}
