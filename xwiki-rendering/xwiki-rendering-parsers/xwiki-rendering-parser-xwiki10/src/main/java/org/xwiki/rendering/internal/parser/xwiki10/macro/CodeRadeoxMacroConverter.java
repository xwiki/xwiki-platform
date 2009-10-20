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

import org.xwiki.component.annotation.Component;
import org.xwiki.rendering.parser.xwiki10.FilterContext;
import org.xwiki.rendering.parser.xwiki10.macro.AbstractRadeoxMacroConverter;
import org.xwiki.rendering.parser.xwiki10.macro.RadeoxMacroParameters;

/**
 * 
 * @version $Id$
 * @since 1.8M1
 */
@Component("code")
public class CodeRadeoxMacroConverter extends AbstractRadeoxMacroConverter
{
    public CodeRadeoxMacroConverter()
    {
        registerParameter("");
    }

    @Override
    protected void convertParameter(Map<String, String> parameters20, String key, String value)
    {
        if ("".equals(key)) {
            super.convertParameter(parameters20, "language", value);
        } else {
            super.convertParameter(parameters20, key, value);
        }
    }

    @Override
    protected String convertContent(String content, RadeoxMacroParameters parameters, FilterContext filterContext)
    {
        return super.convertContent(content, parameters, filterContext).trim();
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
