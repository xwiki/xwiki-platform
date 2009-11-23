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
package org.xwiki.rendering.parser.xwiki10.macro;

import java.util.Map;

import org.xwiki.rendering.internal.parser.xwiki10.HTMLFilter.HTMLFilterContext;

/**
 * Base class for HTML elements converters.
 * 
 * @version $Id$
 * @since 1.8M1
 */
public abstract class AbstractHTMLElementConverter implements HTMLElementConverter
{
    private String elementName;

    protected AbstractHTMLElementConverter()
    {

    }

    protected AbstractHTMLElementConverter(String elementName)
    {
        this.elementName = elementName;
    }

    public String getElementName()
    {
        return this.elementName;
    }

    public boolean protectResult()
    {
        return false;
    }

    public boolean isInline()
    {
        return true;
    }

    public abstract String convert(String name, Map<String, String> parameters, String content,
        HTMLFilterContext filterContext);

    protected void appendParameters(StringBuffer result, Map<String, String> parameters, HTMLFilterContext filterContext)
    {
        StringBuffer parametersSB = new StringBuffer();
        for (Map.Entry<String, String> parameter : parameters.entrySet()) {
            if (parametersSB.length() > 0) {
                parametersSB.append(" ");
            }
            parametersSB.append(filterContext.cleanContent(parameter.getKey()));
            parametersSB.append("=");
            parametersSB.append(filterContext.cleanContent(parameter.getValue()));
        }

        result.append(parametersSB);
    }
}
