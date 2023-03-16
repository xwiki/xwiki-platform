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
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.component.annotation.Component;
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
@Component
@Singleton
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
        return render(iconName, iconSet, iconSet.getRenderWiki());
    }

    @Override
    public String renderHTML(String iconName, IconSet iconSet) throws IconException
    {
        return render(iconName, iconSet, iconSet.getRenderHTML());
    }

    @Override
    public String render(String iconName, IconSet iconSet, String renderer) throws IconException
    {
        // The method should return an empty string in case no renderer or icon set was given
        if (renderer == null || iconSet == null) {
            return "";
        }
        // Get the icon
        Icon icon = iconSet.getIcon(iconName);

        // The icon may not exist
        if (icon == null) {
            // return an empty string. Idea: fallback on a different icon instead?
            return "";
        }

        // Add the icon set resources
        use(iconSet);

        // Interpret the velocity command
        StringWriter contentToParse = new StringWriter();
        contentToParse.write("#set($icon = \"");
        contentToParse.write(icon.getValue());
        contentToParse.write("\")\n");
        contentToParse.write(renderer);

        return this.velocityRenderer.render(contentToParse.toString(), iconSet.getSourceDocumentReference());
    }

    @Override
    public void use(IconSet iconSet) throws IconException
    {
        if (iconSet == null) {
            throw new IconException("The icon set is null");
        }
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

    private void activeCSS(IconSet iconSet) throws IconException
    {
        String url = this.velocityRenderer.render(iconSet.getCss(), iconSet.getSourceDocumentReference());
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("rel", "stylesheet");
        this.linkExtension.use(url, parameters);
    }

    private void activeSSX(IconSet iconSet)
    {
        this.skinExtension.use(iconSet.getSsx());
    }

    private void activeJSX(IconSet iconSet)
    {
        this.jsExtension.use(iconSet.getJsx());
    }
}
