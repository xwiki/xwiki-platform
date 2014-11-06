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
package com.xpn.xwiki.internal.skin;

import java.io.InputStream;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.filter.input.DefaultInputStreamInputSource;
import org.xwiki.filter.input.InputSource;
import org.xwiki.filter.input.StringInputSource;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.LocalDocumentReference;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.BaseProperty;

@Component(roles = SkinManager.class)
@Singleton
public class SkinManager
{
    @Inject
    private Provider<XWikiContext> xcontextProvider;

    @Inject
    @Named("all")
    private ConfigurationSource allConfiguration;

    @Inject
    @Named("xwikicfg")
    private ConfigurationSource xwikicfg;

    @Inject
    @Named("currentmixed")
    private DocumentReferenceResolver<String> currentMixedDocumentReferenceResolver;

    @Inject
    private Logger logger;

    public Skin getSkin(String id)
    {

    }

    public Skin getCurrentSkin()
    {

    }

    public String getCurrentSkinId()
    {
        String skin;

        XWikiContext xcontext = this.xcontextProvider.get();

        if (xcontext != null) {
            // Try to get it from context
            skin = (String) xcontext.get("skin");
            if (StringUtils.isNotEmpty(skin)) {
                return skin;
            } else {
                skin = null;
            }

            // Try to get it from URL
            if (xcontext.getRequest() != null) {
                skin = xcontext.getRequest().getParameter("skin");
                if (StringUtils.isNotEmpty(skin)) {
                    return skin;
                } else {
                    skin = null;
                }
            }

            // Try to get it from preferences (user -> space -> wiki -> xwiki.properties)
            skin = this.allConfiguration.getProperty("skin");
            if (skin != null) {
                return skin;
            }
        }

        // Try to get it from xwiki.cfg
        skin = this.xwikicfg.getProperty("xwiki.defaultskin", XWiki.DEFAULT_SKIN);

        return StringUtils.isNotEmpty(skin) ? skin : null;
    }

    // TODO: put that in some SkinContext component
    private String getBaseSkin()
    {
        String baseskin;

        XWikiContext xcontext = this.xcontextProvider.get();

        if (xcontext != null) {
            // Try to get it from context
            baseskin = (String) xcontext.get("baseskin");
            if (StringUtils.isNotEmpty(baseskin)) {
                return baseskin;
            } else {
                baseskin = null;
            }

            // Try to get it from the skin
            String skin = getCurrentSkinId();
            if (skin != null) {
                BaseObject skinObject = getSkinObject(skin);
                if (skinObject != null) {
                    baseskin = skinObject.getStringValue("baseskin");
                    if (StringUtils.isNotEmpty(baseskin)) {
                        return baseskin;
                    }
                }
            }
        }

        // Try to get it from xwiki.cfg
        baseskin = this.xwikicfg.getProperty("xwiki.defaultbaseskin");

        return StringUtils.isNotEmpty(baseskin) ? baseskin : null;
    }

    private InputSource getTemplateContentFromSkin(String template, String skin)
    {
        InputSource source;

        // Try from wiki pages
        // FIXME: macros.vm from document based skins is not supported by default VelocityManager yet
        XWikiDocument skinDocument = template.equals("macros.vm") ? null : getSkinDocument(skin);
        if (skinDocument != null) {
            source = getTemplateContentFromDocumentSkin(template, skinDocument);
        } else {
            // If not a wiki based skin try from filesystem skins
            source = getResourceAsStringContent("/skins/" + skin + '/', template);
        }

        return source;
    }


}
