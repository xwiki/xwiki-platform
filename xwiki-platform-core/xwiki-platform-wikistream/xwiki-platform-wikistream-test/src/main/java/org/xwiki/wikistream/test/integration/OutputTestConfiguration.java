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
package org.xwiki.wikistream.test.integration;

import java.util.HashMap;

import org.xwiki.wikistream.output.target.OutputTarget;
import org.xwiki.wikistream.utils.WikiStreamConstants;

public class OutputTestConfiguration extends HashMap<String, Object>
{
    public final String typeId;

    public OutputTestConfiguration(String typeId)
    {
        this.typeId = typeId;
    }
    
    public OutputTestConfiguration(OutputTestConfiguration other)
    {
        super(other);

        this.typeId = other.typeId;
    }

    public OutputTarget getTarget()
    {
        return (OutputTarget) get(WikiStreamConstants.PROPERTY_TARGET);
    }

    public void setTarget(OutputTarget target)
    {
        put(WikiStreamConstants.PROPERTY_TARGET, target);
    }

    public void setEncoding(String encoding)
    {
        put(WikiStreamConstants.PROPERTY_ENCODING, encoding);
    }

    public void setFormat(boolean format)
    {
        put(WikiStreamConstants.PROPERTY_FORMAT, format);
    }
}
