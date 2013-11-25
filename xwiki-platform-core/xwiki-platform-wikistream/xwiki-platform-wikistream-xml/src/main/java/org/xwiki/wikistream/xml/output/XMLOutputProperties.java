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
package org.xwiki.wikistream.xml.output;

import org.xwiki.properties.annotation.PropertyDescription;
import org.xwiki.properties.annotation.PropertyMandatory;
import org.xwiki.properties.annotation.PropertyName;
import org.xwiki.stability.Unstable;
import org.xwiki.wikistream.output.OutputTarget;
import org.xwiki.wikistream.xml.XMLProperties;

/**
 * Base properties for XML based serializers.
 * 
 * @version $Id$
 * @since 5.3RC1
 */
@Unstable
public class XMLOutputProperties extends XMLProperties
{
    /**
     * @see #isFormat()
     */
    private boolean format = true;

    /**
     * @see #getEncoding()
     */
    private String encoding = "UTF-8";

    /**
     * @see #getTarget()
     */
    private OutputTarget target;

    /**
     * @return true if the output XML should be formated
     */
    @PropertyName("Format")
    @PropertyDescription("Indicate if the output XML should be formated")
    public boolean isFormat()
    {
        return this.format;
    }

    /**
     * @param format Indicate if the output XML should be formated
     */
    public void setFormat(boolean format)
    {
        this.format = format;
    }

    /**
     * @return The encoding to use when serializing XML
     */
    @PropertyName("Encoding")
    @PropertyDescription("The encoding to use when serializing XML")
    public String getEncoding()
    {
        return this.encoding;
    }

    /**
     * @param encoding The encoding to use when serializing XML
     */
    public void setEncoding(String encoding)
    {
        this.encoding = encoding;
    }

    /**
     * @return The target where to save the content
     */
    @PropertyName("Target")
    @PropertyDescription("The target where to save the content")
    @PropertyMandatory
    public OutputTarget getTarget()
    {
        return this.target;
    }

    /**
     * @param target The target where to save the content
     */
    public void setTarget(OutputTarget target)
    {
        this.target = target;
    }
}
