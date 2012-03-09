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
package com.xpn.xwiki.objects;

import org.dom4j.Element;

public interface PropertyInterface extends ElementInterface
{
    /**
     * Returns the identifier of this property for hibernate.
     * The return type is long since 4.0M1
     * @return the identifier of this property
     */
    long getId();

    /**
     * Dummy function to satisfy hibernate requirements.
     * @param id the identifier. A long since 4.0M1
     */
    void setId(long id);

    BaseCollection getObject();

    void setObject(BaseCollection object);

    String toFormString();

    Element toXML();

    PropertyInterface clone();
}
