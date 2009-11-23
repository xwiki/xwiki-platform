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

import org.apache.commons.lang.StringUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.rendering.parser.xwiki10.Filter;
import org.xwiki.rendering.parser.xwiki10.FilterContext;
import org.xwiki.rendering.parser.xwiki10.macro.AbstractRadeoxMacroConverter;
import org.xwiki.rendering.parser.xwiki10.macro.RadeoxMacroParameters;
import org.xwiki.rendering.parser.xwiki10.util.CleanUtil;

@Component("quote")
public class QuoteRadeoxMacroConverter extends AbstractRadeoxMacroConverter
{
    @Requirement("standalonenewlinecleanning")
    private Filter standaloneNewLineCleaningFilter;

    @Override
    public String convert(String name, RadeoxMacroParameters parameters, String content, FilterContext filterContext)
    {
        StringBuffer result = new StringBuffer();

        if (!StringUtils.isEmpty(content.replaceAll("[ \t]", ""))) {
            result.append(">");
            result.append(CleanUtil.removeTrailingNewLines(CleanUtil.removeLeadingNewLines(this.standaloneNewLineCleaningFilter.filter(content, filterContext))).replaceAll("\n", "\n>"));
        }

        return result.toString();
    }

    public boolean supportContent()
    {
        return true;
    }
    
    @Override
    public boolean isInline()
    {
        return false;
    }
}
