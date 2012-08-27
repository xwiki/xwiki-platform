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
 * Represents the definition of a wiki component implementation.
 * 
 * @version $Id$
 * @since 4.2M3
 */
public interface WikiComponent
{
    /**
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
     * @return the extra list of interfaces this component implementation implements.
     */
    List<Class< ? >> getImplementedInterfaces();

    /**
     * @return the map of method name/wiki code this component implementation handles. 
     */
    Map<String, XDOM> getHandledMethods();
}
