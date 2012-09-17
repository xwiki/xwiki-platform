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
package org.xwiki.component.wiki;

import java.util.List;
import java.util.Map;

import org.xwiki.model.reference.DocumentReference;
import org.xwiki.rendering.block.XDOM;

/**
 * Represents the definition of a wiki component implementation. A java component can extend this interface if it needs
 * to be bound to a document, in order to be unregistered and registered again when the document is modified, and
 * unregistered when the document is deleted.
 * 
 * @version $Id$
 * @since 4.2M3
 */
public interface WikiComponent
{
    /**
     * Get the reference of the document this component instance is bound to.
     *
     * @return the reference to the document holding this wiki component definition.
     */
    DocumentReference getDocumentReference();
    
    /**
     * @return the role implemented by this component implementation.
     */
    Class< ? > getRole();

    /**
     * @return the hint of the role implemented by this component implementation.
     */
    String getRoleHint();

    /**
     * Get the list of interfaces the wiki component implements, apart from its main Role. When the component is
     * entirely written in a document, it allows the {@link WikiComponentManager} to add those Interfaces to the list of
     * implemented interfaces of the {@link java.lang.reflect.Proxy} it will create.
     * Classes extending this interface only need to return an empty list here since the list of interfaces they
     * implement will be determined using Java reflection.
     *
     * @return the extra list of interfaces this component implementation implements.
     */
    List<Class< ? >> getImplementedInterfaces();

    /**
     * Get the implementations of all the methods the component handles. It allows to write method implementations in
     * wiki documents, using script macros. When a method has multiple signatures (different sets of parameters) the
     * same {@link XDOM} will be executed.
     * Classes extending this interface only need to return an empty list here since the methods they handle are native
     * Java methods.
     *
     * @return the map of method name/wiki code this component implementation handles. 
     */
    Map<String, XDOM> getHandledMethods();
}
