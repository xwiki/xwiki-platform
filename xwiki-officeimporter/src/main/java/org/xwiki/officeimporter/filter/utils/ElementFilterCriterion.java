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
package org.xwiki.officeimporter.filter.utils;

import org.w3c.dom.Element;

/**
 * Interface for defining element selections. Element filters are useful when an operation needs to be performed on a
 * selection of Elements depending on some criterion. In such cases, the operation will be performed only if the
 * provided filter returns true for that particular Element.
 * 
 * @version $Id$
 * @since 1.8M2
 */
public interface ElementFilterCriterion
{
    /**
     * @param element the {@link Element} against which the criterion is applied.
     * @return true if this particular element should be filtered.
     */
    boolean isFiltered(Element element);
}
