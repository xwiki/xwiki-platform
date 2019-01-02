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
package org.xwiki.rendering.async;

import java.lang.reflect.Type;

import org.xwiki.component.annotation.Role;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.stability.Unstable;

/**
 * Contextual information related to asynchronous rendering.
 * 
 * @version $Id$
 * @since 10.10RC1
 */
@Role
@Unstable
public interface AsyncContext
{
    /**
     * @return true if it's allowed to render content asynchronously
     */
    boolean isEnabled();

    /**
     * @param enabled true if it's allowed to render content asynchronously
     */
    void setEnabled(boolean enabled);

    /**
     * Indicate that the current execution manipulate the passed entity and the result will need to be removed from the
     * cache if it's modified in any way.
     * <p>
     * <ul>
     * <li>If the reference is a document, any modification made to that document (including object and attachments)
     * will be affected.</li>
     * <li>If the reference is a document containing a class any modification of an object of that class will be
     * affected.</li>
     * </ul>
     * 
     * @param reference the reference of the entity
     */
    void useEntity(EntityReference reference);

    /**
     * Indicate that the current execution manipulate components of the passed type and the result will need to be
     * removed from the cache if any is unregistered or a new one registered.
     * 
     * @param roleType the type of the component role
     */
    void useComponent(Type roleType);

    /**
     * Indicate that the current execution manipulate component with the passed type and hint and the result will need
     * to be removed from the cache if it's registered or unregistered.
     * 
     * @param roleType the type of the component role
     * @param roleHint the hint of the component
     */
    void useComponent(Type roleType, String roleHint);

    /**
     * @param type the type of data to associated with the cached result
     * @param value the value to associated with the cached result
     */
    void use(String type, Object value);
}
