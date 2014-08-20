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
package org.xwiki.icon.internal;

import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;

import org.apache.commons.lang.StringUtils;
import org.xwiki.icon.Icon;
import org.xwiki.icon.IconException;
import org.xwiki.icon.IconRenderer;
import org.xwiki.icon.IconSet;
import org.xwiki.skinx.SkinExtension;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;

/**
 * Default implementation of {@link org.xwiki.icon.IconRenderer}.
 *
 * @since 6.2M1
 * @version $Id$
 */
public class DefaultIconRenderer implements IconRenderer
{
    @Inject
    private Provider<XWikiContext> xcontextProvider;

    @Inject
    @Named("ssx")
    private SkinExtension skinExtension;

    @Inject
    @Named("linkx")
    private SkinExtension linkExtension;

    @Inject
    @Named("jsx")
    private SkinExtension jsExtension;


    @Override
    public String render(String iconName, IconSet iconSet) throws IconException
    {
        return render(iconSet, iconName, iconSet.getRenderWiki());
    }

    @Override
    public String renderHTML(String iconName, IconSet iconSet) throws IconException
    {
        return render(iconSet, iconName, iconSet.getRenderHTML());
    }

    private String render(IconSet iconSet, String iconName, String renderer)
    {
        if (!StringUtils.isBlank(iconSet.getCss())) {
            activeCSS(iconSet);
        }
        if (!StringUtils.isBlank(iconSet.getSsx())) {
            activeSSX(iconSet);
        }
        if (!StringUtils.isBlank(iconSet.getJsx())) {
            activeJSX(iconSet);
        }
        return renderIcon(iconSet, iconName, renderer);
    }

    private void activeCSS(IconSet iconSet)
    {
        XWikiContext xcontext = xcontextProvider.get();
        XWiki xwiki = xcontext.getWiki();
        String url = xwiki.parseContent(iconSet.getCss(), xcontext);
        Map<String, Object> parameters = new HashMap();
        parameters.put("rel", "stylesheet");
        linkExtension.use(url, parameters);
    }

    private void activeSSX(IconSet iconSet)
    {
        skinExtension.use(iconSet.getSsx());
    }

    private void activeJSX(IconSet iconSet)
    {
        jsExtension.use(iconSet.getJsx());
    }

    private String renderIcon(IconSet iconSet, String iconName, String renderer)
    {
        XWikiContext xcontext = xcontextProvider.get();
        XWiki xwiki = xcontext.getWiki();

        // Get the icon
        Icon icon = iconSet.getIcon(iconName);

        // Interpret the velocity command
        StringWriter contentToParse = new StringWriter();
        contentToParse.write("#set($icon = \"");
        contentToParse.write(icon.getValue());
        contentToParse.write("\")\n");
        contentToParse.write(renderer);

        return xwiki.parseContent(contentToParse.toString(), xcontext);
    }
}
