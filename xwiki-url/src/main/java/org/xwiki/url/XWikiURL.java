/*
 * Copyright 2007, XpertNet SARL, and individual contributors as indicated
 * by the contributors.txt.
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

public class XWikiURL
{
    private String action;

    private String space;

    private String document;

    private Map parameters;

    public String getAction()
    {
        return this.action;
    }

    public void setAction(String action)
    {
        this.action = action;
    }

    public String getSpace()
    {
        return this.space;
    }

    public void setSpace(String space)
    {
        this.space = space;
    }

    public String getDocument()
    {
        return this.document;
    }

    public void setDocument(String document)
    {
        this.document = document;
    }

    public void addParameter(String name, String value)
    {
        this.parameters.put(name, value);
    }

    public String getParameter(String name)
    {
        return (String) this.parameters.get(name);
    }
}
