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
package org.xwiki.component.phase;

/**
 * @see #initialize()
 */
public interface Initializable
{
    /**
     * Method called by the Component Manager when the component is created for the first time (i.e. when it's looked up
     * for the first time or if the component is specified as being loaded on startup). If the component instantiation
     * strategy is singleton then this method is called only once during the lifecycle of the Component Manager.
     * Otherwise the component is created at each lookup and thus this method is called at each lookup too.
     * 
     * @throws InitializationException if an error happens during a component's initialization
     */
    void initialize() throws InitializationException;
}
