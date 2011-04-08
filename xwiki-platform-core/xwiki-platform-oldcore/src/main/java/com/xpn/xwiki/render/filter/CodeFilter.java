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

import java.io.Writer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.radeox.api.engine.context.InitialRenderContext;
import org.radeox.filter.context.FilterContext;
import org.radeox.filter.regex.RegexTokenFilter;
import org.radeox.macro.Macro;
import org.radeox.macro.parameter.MacroParameter;
import org.radeox.regex.MatchResult;
import org.radeox.util.StringBufferWriter;

import com.xpn.xwiki.render.macro.XWikiCodeMacro;

public class CodeFilter extends RegexTokenFilter
{
    private static Log log = LogFactory.getLog(CodeFilter.class);

    private Macro codeMacro = new XWikiCodeMacro();
    
    public CodeFilter()
    {
        super("\\{(code)(?::([^\\}]*))?\\}(.*?)\\{code}", MULTILINE);
    }

    public void setInitialContext(InitialRenderContext context)
    {
        this.codeMacro.setInitialContext(context);
    }

    public void handleMatch(StringBuffer buffer, MatchResult result, FilterContext context)
    {
    	// Call the XWikiCodeMacro macro...
        Writer writer = new StringBufferWriter(buffer);

        MacroParameter mParams = context.getMacroParameter();
        mParams.setParams(result.group(2));
        mParams.setContent(result.group(3));

        try {
        	this.codeMacro.execute(writer, mParams);
        } catch (Throwable e) {
            log.warn("CodeFilter: unable to format macro: " + result.group(1), e);
            buffer.append("<div class=\"error\">" + result.group(1) + ": " + e.getMessage() + "</div>");
        }
    }
}
