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
package org.xwiki.gwt.dom.client;

import org.xwiki.gwt.dom.client.internal.DefaultRangeFactory;

import com.google.gwt.core.client.GWT;

/**
 * Defines the interface used to create range objects.
 * 
 * @version $Id$
 */
public interface RangeFactory
{
    /**
     * We create the singleton instance using deferred binding in order to use different implementations for different
     * browsers.
     */
    RangeFactory INSTANCE = GWT.create(DefaultRangeFactory.class);

    /**
     * @param doc The DOM document for which to create the range.
     * @return A new range bound to the specified document.
     */
    Range createRange(Document doc);
}
