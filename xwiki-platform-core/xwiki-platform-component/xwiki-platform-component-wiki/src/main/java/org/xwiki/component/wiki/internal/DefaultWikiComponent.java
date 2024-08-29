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
import org.xwiki.component.wiki.WikiComponentScope;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.syntax.Syntax;

/**
 * Default implementation of a wiki component definition.
 * 
 * @version $Id$
 * @since 4.2M3
 */
public class DefaultWikiComponent implements WikiComponent
{
    /**
     * @see #getDocumentReference()
     */
    private DocumentReference documentReference;

    /**
     * @see #getAuthorReference()
     */
    private DocumentReference authorReference;

    /**
     * @see #getHandledMethods()
     */
    private Map<String, XDOM> handledMethods = new HashMap<>();

    /**
     * @see #getRoleType()
     */
    private Type roleType;

    /**
     * @see #getRoleHint()
     */
    private String roleHint;

    /**
     * @see #getRoleTypePriority()
     */
    private int roleTypePriority;

    /**
     * @see #getRoleHintPriority()
     */
    private int roleHintPriority;

    /**
     * @see #getScope()
     */
    private WikiComponentScope scope;

    /**
     * @see #getImplementedInterfaces()
     */
    private List<Class< ? >> implementedInterfaces = new ArrayList<>();

    /**
     * @see #getDependencies()
     */
    private Map<String, ComponentDescriptor> dependencies = new HashMap<>();

    /**
     * @see #getSyntax()
     */
    private Syntax syntax;

    /**
     * Constructor of this component.
     * 
     * @param documentReference the document holding the component definition
     * @param authorReference the author of the document holding the component definition
     * @param roleType the role Type implemented
     * @param roleHint the role hint for this role implementation
     * @param scope the scope of this component
     */
    public DefaultWikiComponent(DocumentReference documentReference, DocumentReference authorReference,
        Type roleType, String roleHint, WikiComponentScope scope)
    {
        this.documentReference = documentReference;
        this.authorReference = authorReference;
        this.roleType = roleType;
        this.roleHint = roleHint;
        this.scope = scope;
    }

    @Override
    public DocumentReference getDocumentReference()
    {
        return this.documentReference;
    }

    @Override
    public DocumentReference getAuthorReference()
    {
        return this.authorReference;
    }

    /**
     * Get the implementations of all the methods the component handles. It allows to write method implementations in
     * wiki documents, using script macros. When a method has multiple signatures (different sets of parameters) the
     * same {@link org.xwiki.rendering.block.XDOM} will be executed.
     *
     * @return the map of method name/wiki code this component implementation handles.
     */
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
    public WikiComponentScope getScope()
    {
        return this.scope;
    }

    /**
     * Get the list of interfaces the wiki component implements, apart from its main Role. When the component is
     * entirely written in a document, it allows the {@link org.xwiki.component.wiki.WikiComponentManager} to add those
     * Interfaces to the list of implemented interfaces of the {@link java.lang.reflect.Proxy} it will create.
     *
     * @return the extra list of interfaces this component implementation implements.
     */
    public List<Class< ? >> getImplementedInterfaces()
    {
        return this.implementedInterfaces;
    }

    /**
     * Methods returned by {@link #getHandledMethods()} can require other components to be injected in their context.
     * Each entry in the map returned by this method will be injected in the rendering context when methods will be
     * executed. The name of the variable in the context is defined by the key in the returned Map.
     *
     * @return the map of dependencies of this component
     */
    public Map<String, ComponentDescriptor> getDependencies()
    {
        return this.dependencies;
    }

    /**
      * @return The syntax in which the component document is written
     */
    public Syntax getSyntax()
    {
        return syntax;
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

    /**
     * Set the syntax in which the component document is written.
     *
     * @param syntax the syntax to set
     */
    public void setSyntax(Syntax syntax)
    {
        this.syntax = syntax;
    }

    @Override
    public int getRoleTypePriority()
    {
        return this.roleTypePriority;
    }

    /**
     * @param roleTypePriority the role type priority of the component
     * @since 15.4RC1
     */
    public void setRoleTypePriority(int roleTypePriority)
    {
        this.roleTypePriority = roleTypePriority;
    }

    @Override
    public int getRoleHintPriority()
    {
        return this.roleTypePriority;
    }

    /**
     * @param roleHintPriority the role hint priority of the component
     * @since 15.4RC1
     */
    public void setRoleHintPriority(int roleHintPriority)
    {
        this.roleHintPriority = roleHintPriority;
    }
}
