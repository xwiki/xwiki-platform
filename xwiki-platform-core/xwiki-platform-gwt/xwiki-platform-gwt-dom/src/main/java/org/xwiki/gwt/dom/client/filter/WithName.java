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
package org.xwiki.gwt.dom.client.filter;

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.dom.client.Node;

/**
 * Filters nodes with the specified name.
 * 
 * @version $Id$
 */
public class WithName implements NodeFilter
{
    /**
     * The list of lower cased node names that are accepted by this filter.
     */
    private final List<String> lowerCaseNames = new ArrayList<String>();

    /**
     * Creates a new filter that accepts DOM nodes if their name is contained in the given list.
     * 
     * @param names the list of node names to accept
     */
    public WithName(String... names)
    {
        for (String name : names) {
            lowerCaseNames.add(name.toLowerCase());
        }
    }

    @Override
    public Action acceptNode(Node node)
    {
        return lowerCaseNames.contains(node.getNodeName().toLowerCase()) ? Action.ACCEPT : Action.SKIP;
    }
}
