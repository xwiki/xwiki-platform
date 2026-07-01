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
package org.xwiki.component.wiki.internal.bridge;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

import org.xwiki.component.annotation.Role;
import org.xwiki.component.descriptor.ComponentDescriptor;
import org.xwiki.component.wiki.WikiComponentException;
import org.xwiki.component.wiki.WikiComponentScope;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.syntax.Syntax;

/**
 * A bridge allowing to isolate the {@link org.xwiki.component.wiki.WikiComponentManager} from the old model.
 *
 * @version $Id$
 * @since 4.3M2
 */
@Role
public interface WikiComponentBridge
{
    /**
     * Get the syntax of a document.
     *
     * @param reference a reference to a document holding a component
     * @return the syntax of the given document
     * @throws WikiComponentException if the document can't be retrieved
     */
    Syntax getSyntax(DocumentReference reference) throws WikiComponentException;

    /**
     * Get a reference to the author of a document.
     *
     * @param reference a reference to a document holding a component
     * @return a reference to the author of the given document
     * @throws WikiComponentException if the document can't be retrieved
     */
    DocumentReference getAuthorReference(DocumentReference reference) throws WikiComponentException;

    /**
     * Get the role {@link Type} of a wiki component.
     *
     * @param reference a reference to a document holding a component
     * @return the role {@link Type} of the wiki component.
     * @throws WikiComponentException if the document can't be retrieved or if it doesn't contain a component definition
     */
    Type getRoleType(DocumentReference reference) throws WikiComponentException;

    /**
     * Get the role hint of a wiki component.
     *
     * @param reference a reference to a document holding a component
     * @return the role hint of the wiki component.
     * @throws WikiComponentException if the document can't be retrieved or if it doesn't contain a component definition
     */
    String getRoleHint(DocumentReference reference) throws WikiComponentException;

    /**
     * Get the scope of a wiki component.
     *
     * @param reference a reference to a document holding a component
     * @return the scope of the wiki component
     * @throws WikiComponentException if the document can't be retrieved or if it doesn't contain a component definition
     */
    WikiComponentScope getScope(DocumentReference reference) throws WikiComponentException;

    /**
     * The role type priority represents the priority for ordering components sharing the same type: it can be used to
     * order the components when retrieving a list of components of the same type. The lower the value, the higher the
     * priority.
     *
     * @param reference a reference to a document
     * @return the role type priority of the component.
     * @throws WikiComponentException if the document can't be retrieved or if it doesn't contain a component definition
     * @since 15.4RC1
     */
    int getRoleTypePriority(DocumentReference reference) throws WikiComponentException;

    /**
     * The role hint priority represents the priority for ordering components sharing the same type and hint: it can be
     * used to decide which component should be overridden when loading them. The lower the value, the higher the
     * priority.
     *
     * @param reference a reference to a document
     * @return the role hint priority of the component.
     * @throws WikiComponentException if the document can't be retrieved or if it doesn't contain a component definition
     * @since 15.4RC1
     */
    int getRoleHintPriority(DocumentReference reference) throws WikiComponentException;

    /**
     * @param reference a reference to a document holding a wiki component
     * @return the map of component handled methods/method body
     * @throws WikiComponentException if the document can't be retrieved or if it doesn't contain a component definition
     */
    Map<String, XDOM> getHandledMethods(DocumentReference reference) throws WikiComponentException;

    /**
     * The array of interfaces declared by a wiki component, if some interfaces can't be found by the
     * {@link ClassLoader} they will be filtered out and a warning will be displayed in the log.
     *
     * @param reference a reference to a document holding a component
     * @return the array of interfaces declared by the wiki component
     * @throws WikiComponentException if the document can't be retrieved or if it doesn't contain a component definition
     */
    List<Class< ? >> getDeclaredInterfaces(DocumentReference reference) throws WikiComponentException;

    /**
     * Retrieve the Map of dependencies declared by the wiki component, if some dependencies can't be found by the
     * {@link ClassLoader} they will be filtered out a warning will be displayed in the log.
     *
     * @param reference a reference to a document holding a component
     * @return the Map of dependencies declared by the wiki component
     * @throws WikiComponentException if the document can't be retrieved or if it doesn't contain a component definition
     */
    Map<String, ComponentDescriptor> getDependencies(DocumentReference reference) throws WikiComponentException;

    /**
     * Determine if the document has been saved by a user with programming rights.
     *
     * @param reference a reference to a document
     * @return true if the document has been saved by a user with programming rights, false otherwise
     * @throws WikiComponentException if the document can't be retrieved
     */
    boolean hasProgrammingRights(DocumentReference reference) throws WikiComponentException;
}
