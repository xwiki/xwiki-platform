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

import org.apache.commons.lang3.StringUtils;
import org.xwiki.icon.Icon;
import org.xwiki.icon.IconException;
import org.xwiki.icon.IconRenderer;
import org.xwiki.icon.IconSet;
import org.xwiki.skinx.SkinExtension;

/**
 * Default implementation of {@link org.xwiki.icon.IconRenderer}.
 *
 * @version $Id$
 * @since 6.2M1
 */
public class DefaultIconRenderer implements IconRenderer
{
    @Inject
    @Named("ssx")
    private SkinExtension skinExtension;

    @Inject
    @Named("linkx")
    private SkinExtension linkExtension;

    @Inject
    @Named("jsx")
    private SkinExtension jsExtension;

    @Inject
    private VelocityRenderer velocityRenderer;

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

    @Override public String renderCustom(String iconName, IconSet iconSet) throws IconException
    {
        return render(iconSet, iconName, iconSet.getRenderCustom());
    }

    @Override public void use(IconSet iconSet) throws IconException
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
    }

    private String render(IconSet iconSet, String iconName, String renderer) throws IconException
    {
        use(iconSet);
        return renderIcon(iconSet, iconName, renderer);
    }

    private void activeCSS(IconSet iconSet) throws IconException
    {
        String url = velocityRenderer.render(iconSet.getCss());
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

    private String renderIcon(IconSet iconSet, String iconName, String renderer) throws IconException
    {
        // Get the icon
        Icon icon = iconSet.getIcon(iconName);

        // The icon may not exist
        if (icon == null) {
            // return an empty string. Idea: fallback on a different icon instead?
            return "";
        }

        // Interpret the velocity command
        StringWriter contentToParse = new StringWriter();
        contentToParse.write("#set($icon = \"");
        contentToParse.write(icon.getValue());
        contentToParse.write("\")\n");
        contentToParse.write(renderer);

        return velocityRenderer.render(contentToParse.toString());
    }
}
