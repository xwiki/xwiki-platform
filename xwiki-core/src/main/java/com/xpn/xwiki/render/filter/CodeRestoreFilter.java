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
package com.xpn.xwiki.render.filter;

import org.radeox.filter.context.FilterContext;
import org.radeox.filter.regex.RegexTokenFilter;
import org.radeox.regex.MatchResult;

import java.util.LinkedList;

/**
 * @see CodeRemoveFilter
 */
public class CodeRestoreFilter extends RegexTokenFilter
{
    public CodeRestoreFilter()
    {
        super("(\\{(code)(?::([^\\}]*))?\\})\\{code}", SINGLELINE);
    }

    /**
     * @see CodeRemoveFilter#handleMatch(StringBuffer, MatchResult, FilterContext) 
     */
    public void handleMatch(StringBuffer buffer, MatchResult result, FilterContext context)
    {
        LinkedList contentList =
            (LinkedList) context.getRenderContext().get(CodeRemoveFilter.CODE_MACRO_CONTENT);
        
    	buffer.append(result.group(1));
    	buffer.append((String) contentList.removeFirst());
    	buffer.append("{code}");
    }
}
