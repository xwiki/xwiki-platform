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
package org.xwiki.image.style.rest.internal;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.image.style.ImageStyleConfiguration;
import org.xwiki.image.style.ImageStyleException;
import org.xwiki.image.style.ImageStyleManager;
import org.xwiki.image.style.model.ImageStyle;
import org.xwiki.image.style.rest.ImageStylesResource;
import org.xwiki.image.style.rest.model.jaxb.Style;
import org.xwiki.image.style.rest.model.jaxb.Styles;
import org.xwiki.rest.XWikiRestComponent;

import com.xpn.xwiki.XWikiContext;

import static javax.ws.rs.core.Response.Status.NO_CONTENT;

/**
 * Default image style rest endpoint implementation.
 *
 * @version $Id$
 * @since 14.3RC1
 */
@Component
@Named("org.xwiki.image.style.rest.internal.DefaultImageStylesResource")
@Singleton
public class DefaultImageStylesResource implements ImageStylesResource, XWikiRestComponent
{
    @Inject
    private ImageStyleConfiguration imageStyleConfiguration;

    @Inject
    private ImageStyleManager imageStyleManager;

    @Inject
    private Provider<XWikiContext> contextProvider;

    @Override
    public Styles getStyles(String wikiName) throws ImageStyleException
    {
        Styles styles = new Styles();
        styles.getImageStyles().addAll(convert(this.imageStyleManager.getImageStyles(wikiName)));
        return styles;
    }

    @Override
    public Map<String, String> getDefaultStyleIdentifier(String wikiName, String documentReference)
        throws ImageStyleException
    {
        String defaultStyle = this.imageStyleConfiguration.getDefaultStyle(wikiName, documentReference);
        boolean forceDefaultStyle = this.imageStyleConfiguration.getForceDefaultStyle(wikiName, documentReference);
        Map<String, String> response;
        if (StringUtils.isEmpty(defaultStyle)) {
            this.contextProvider.get().getResponse().setStatus(NO_CONTENT.getStatusCode());
            response = Map.of();
        } else {
            response = Map.of(
                "defaultStyle", defaultStyle,
                "forceDefaultStyle", Boolean.toString(forceDefaultStyle)
            );
        }
        return response;
    }

    private List<Style> convert(Set<ImageStyle> imageStyles)
    {
        return imageStyles
            .stream()
            .map(this::convert)
            .collect(Collectors.toList());
    }

    private Style convert(ImageStyle imageStyle)
    {
        Style style = new Style();
        style.setIdentifier(imageStyle.getIdentifier());
        style.setPrettyName(imageStyle.getPrettyName());
        style.setType(imageStyle.getType());
        style.setAdjustableSize(imageStyle.getAdjustableSize());
        style.setDefaultWidth(imageStyle.getDefaultWidth());
        style.setDefaultHeight(imageStyle.getDefaultHeight());
        style.setAdjustableBorder(imageStyle.getAdjustableBorder());
        style.setDefaultBorder(imageStyle.getDefaultBorder());
        style.setAdjustableAlignment(imageStyle.getAdjustableAlignment());
        style.setDefaultAlignment(imageStyle.getDefaultAlignment());
        style.setAdjustableTextWrap(imageStyle.getAdjustableTextWrap());
        style.setDefaultTextWrap(imageStyle.getDefaultTextWrap());
        return style;
    }
}
