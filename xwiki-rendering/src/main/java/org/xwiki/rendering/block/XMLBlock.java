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
package org.xwiki.rendering.block;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.xwiki.rendering.listener.Listener;

/**
 * @version $Id$
 * @since 1.5M2
 */
public class XMLBlock extends AbstractFatherBlock
{
    private String name;
    
    private Map<String, String> attributes;
    
    public XMLBlock(List<Block> childrenBlocks, String name, Map<String, String> attributes)
    {
        super(childrenBlocks);
        this.name = name;

        // Make sure we preserve the order provided to us so that getAttributes() will return the same order.
        this.attributes = new LinkedHashMap<String, String>(attributes);
    }

    public XMLBlock(String name, Map<String, String> attributes)
    {
        this(new ArrayList<Block>(), name, attributes);
    }

    public String getName()
    {
        return this.name;
    }
    
    public Map<String, String> getAttributes() 
    {
        return Collections.unmodifiableMap(this.attributes);
    }
    
    public void after(Listener listener)
    {
        listener.endXMLElement(getName(), getAttributes());
    }

    public void before(Listener listener)
    {
        listener.beginXMLElement(getName(), getAttributes());
    }

}
