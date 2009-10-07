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
package org.xwiki.rendering.internal.parser.xwiki10.macro;

import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.rendering.internal.parser.xwiki10.HTMLFilter.HTMLFilterContext;
import org.xwiki.rendering.parser.xwiki10.macro.AbstractHTMLElementConverter;

@Component("tt")
public class TeletypeHTMLElementConverter extends AbstractHTMLElementConverter
{
    @Override
    public String convert(String name, Map<String, String> parameters, String content, HTMLFilterContext filterContext)
    {
        String convertedString = null;

        if (parameters.isEmpty()) {
            if (!StringUtils.isEmpty(content)) {
                StringBuffer result = new StringBuffer();

                result.append(filterContext.getFilterContext().addProtectedContent("##", true));
                result.append(filterContext.cleanContent(content));
                result.append(filterContext.getFilterContext().addProtectedContent("##", true));

                convertedString = result.toString();
            } else {
                convertedString = "";
            }
        }

        return convertedString;
    }
}
