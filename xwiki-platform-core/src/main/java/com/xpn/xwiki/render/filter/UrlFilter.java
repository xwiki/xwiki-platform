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
package com.xpn.xwiki.render.filter;

import java.text.MessageFormat;

import org.radeox.api.engine.context.InitialRenderContext;
import org.radeox.filter.CacheFilter;
import org.radeox.filter.context.FilterContext;
import org.radeox.filter.regex.LocaleRegexTokenFilter;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.web.Utils;

public class UrlFilter extends LocaleRegexTokenFilter implements CacheFilter
{
    private MessageFormat formatter;

    @Override
    protected String getLocaleKey()
    {
        return "filter.url";
    }

    @Override
    public void setInitialContext(InitialRenderContext context)
    {
        super.setInitialContext(context);
        String outputTemplate = this.outputMessages.getString(getLocaleKey() + ".print");
        this.formatter = new MessageFormat(outputTemplate);
    }

    @Override
    public void handleMatch(StringBuffer buffer, org.radeox.regex.MatchResult result, FilterContext context)
    {
        XWikiContext xcontext = (XWikiContext) context.getRenderContext().get("xcontext");
        String url = result.group(0);
        url = Utils.createPlaceholder(url, xcontext);
        buffer.append(this.formatter.format(new Object[] {url}));
    }
}
