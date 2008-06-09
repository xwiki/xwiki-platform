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
package org.xwiki.velocity.introspection;

import org.apache.velocity.util.introspection.Uberspect;

/**
 * Interface that marks uberspectors as chainable, meaning that multiple uberspectors can be
 * combined in a chain (using the Decorator pattern).
 * 
 * @version $Id$
 * @since 1.5M1
 */
public interface ChainableUberspector extends Uberspect
{
    /**
     * Since uberspectors are dynamically enabled using a configuration string, we cannot simply
     * call the constructors to pass the inner uberspector. We can either instantiate uberspectors
     * using reflection and passing the wrapped object to the constructor, or explicitely "wrap" the
     * inner uberspector. The second method has several advantages, so it was chosen here:
     * <ul>
     * <li>Doesn't require adding an extra constructor</li>
     * <li>Keeps the initialization process a bit more simple</li>
     * <li>Allows the wrapping to take place at a different time</li>
     * </ul>
     * 
     * @param inner The decorated uberspector.
     */
    void wrap(Uberspect inner);
}
