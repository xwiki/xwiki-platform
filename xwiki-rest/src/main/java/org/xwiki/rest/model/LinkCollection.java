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
package org.xwiki.rest.model;

import java.util.ArrayList;
import java.util.List;

import com.thoughtworks.xstream.annotations.XStreamImplicit;

/**
 * @version $Id$
 */
public class LinkCollection
{
    @XStreamImplicit
    private List<Link> links;

    public void addLink(Link link)
    {
        if (links == null) {
            links = new ArrayList<Link>();
        }

        links.add(link);
    }

    public List<Link> getLinks()   
    {
        if(links == null) {
            return new ArrayList<Link>();
        }
        
        return links;
    }

    public List<Link> getLinksByRelation(String rel)
    {
        List<Link> result = new ArrayList<Link>();

        if (links != null) {
            for (Link link : links) {
                if (rel.equals(link.getRel())) {
                    result.add(link);
                }
            }
        }

        return result;
    }

    public Link getFirstLinkByRelation(String rel)
    {
        if (links != null) {
            for (Link link : links) {
                if (rel.equals(link.getRel())) {
                    return link;
                }
            }
        }

        return null;
    }

}
