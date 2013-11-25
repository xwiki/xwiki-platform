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
package org.xwiki.wikistream.xml.input;

import org.xwiki.properties.annotation.PropertyDescription;
import org.xwiki.properties.annotation.PropertyMandatory;
import org.xwiki.properties.annotation.PropertyName;
import org.xwiki.stability.Unstable;
import org.xwiki.wikistream.input.InputSource;
import org.xwiki.wikistream.xml.XMLProperties;

/**
 * @version $Id$
 * @since 5.3RC1
 */
@Unstable
public class XMLInputProperties extends XMLProperties
{
    /**
     * @see #getSource()
     */
    private InputSource source;

    /**
     * @see #getEncoding()
     */
    private String encoding;

    /**
     * @return The source to load the wiki from
     */
    @PropertyName("Source")
    @PropertyDescription("The source to load the wiki from")
    @PropertyMandatory
    public InputSource getSource()
    {
        return this.source;
    }

    /**
     * @param source The source to load the wiki from
     */
    public void setSource(InputSource source)
    {
        this.source = source;
    }

    /**
     * @return The encoding to use to parse the content
     */
    @PropertyName("Encoding")
    @PropertyDescription("The encoding to use to parse the content")
    public String getEncoding()
    {
        return this.encoding;
    }

    /**
     * @param encoding The encoding to use to parse the content
     */
    public void setEncoding(String encoding)
    {
        this.encoding = encoding;
    }
}
