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
package org.xwiki.url;

import java.util.Map;
import java.util.HashMap;
import java.util.Collections;

import org.xwiki.bridge.DocumentName;

/**
 * Represents a XWiki URL.
 *
 * @version $Id$
 */
public class XWikiURL
{
    private String action;

    private DocumentName documentName;

    private Map<String, String> parameters = new HashMap<String, String>();

    public String getAction()
    {
        return this.action;
    }

    public void setAction(String action)
    {
        this.action = action;
    }

    public DocumentName getDocumentName()
    {
        return this.documentName;
    }

    public void setDocumentName(DocumentName documentName)
    {
        this.documentName = documentName;
    }

    public void addParameter(String name, String value)
    {
        this.parameters.put(name, value);
    }

    public Map<String, String> getParameters()
    {
        return Collections.unmodifiableMap(this.parameters);
    }

    public String getParameterValue(String name)
    {
        return this.parameters.get(name);
    }
}
