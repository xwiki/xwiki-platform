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
 *
 */
package org.xwiki.component.descriptor;

/**
 * Represents a component dependency.
 * 
 * @version $Id$
 * @since 1.7M1
 */
public interface ComponentDependency<T> extends ComponentRole<T>
{
    /**
     * @return the name of the injection point (can be the name of the field for field injection or the name
     *         of the method for method injection
     */
    String getName();
    
    /**
     * @return the class of the type for the injection (java.lang.String, java.util.List, etc)
     */
    Class< ? > getMappingType();

    /**
     * @return a list of hints used when the mapping type is a collection or map so that only component implementations
     *         matching passed hints are injected
     */
    String[] getHints();
}
