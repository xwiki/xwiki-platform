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

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.Properties;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.bridge.DocumentModelBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.icon.Icon;
import org.xwiki.icon.IconException;
import org.xwiki.icon.IconSet;
import org.xwiki.icon.IconSetLoader;
import org.xwiki.icon.IconType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;

/**
 * Default implementation of {@link org.xwiki.icon.IconSetLoader}.
 *
 * @version $Id$
 * @since 6.2M1
 */
@Component
@Singleton
public class DefaultIconSetLoader implements IconSetLoader
{
    private static final String CSS_PROPERTY_NAME = "xwiki.iconset.css";

    private static final String SSX_PROPERTY_NAME = "xwiki.iconset.ssx";

    private static final String JSX_PROPERTY_NAME = "xwiki.iconset.jsx";

    private static final String RENDER_WIKI_PROPERTY_NAME = "xwiki.iconset.render.wiki";

    private static final String RENDER_HTML_PROPERTY_NAME = "xwiki.iconset.render.html";

    private static final String ICON_TYPE_PROPERTY_NAME = "xwiki.iconset.type";

    private static final String ICON_URL_PROPERTY_NAME = "xwiki.iconset.icon.url";

    private static final String ICON_CSS_CLASS_PROPERTY_NAME = "xwiki.iconset.icon.cssClass";

    private static final String ERROR_MSG = "Failed to load the IconSet [%s].";

    @Inject
    private DocumentAccessBridge documentAccessBridge;

    @Inject
    private WikiDescriptorManager wikiDescriptorManager;

    @Override
    public IconSet loadIconSet(DocumentReference iconSetReference) throws IconException
    {
        try {
            // Get the document
            DocumentModelBridge doc = documentAccessBridge.getDocumentInstance(iconSetReference);
            String content = doc.getContent();
            // The name of the icon set is stored in the IconThemesCode.IconThemeClass XObject of the document
            DocumentReference iconClassRef = new DocumentReference(wikiDescriptorManager.getCurrentWikiId(),
                "IconThemesCode", "IconThemeClass");
            String name = (String) documentAccessBridge.getProperty(iconSetReference, iconClassRef, "name");
            // Load the icon set
            return loadIconSet(new StringReader(content), name);
        } catch (Exception e) {
            throw new IconException(String.format(ERROR_MSG, iconSetReference), e);
        }
    }

    @Override
    public IconSet loadIconSet(Reader input, String name) throws IconException
    {
        IconSet iconSet = new IconSet(name);
        Properties properties = new Properties();
        try {
            properties.load(input);
        } catch (IOException e) {
            throw new IconException(String.format(ERROR_MSG, name), e);
        }

        properties.stringPropertyNames().forEach(key -> setIconSetProperty(iconSet, key, properties.getProperty(key)));

        return iconSet;
    }

    private void setIconSetProperty(IconSet iconSet, String key, String value)
    {
        if (CSS_PROPERTY_NAME.equals(key)) {
            iconSet.setCss(value);
        } else if (SSX_PROPERTY_NAME.equals(key)) {
            iconSet.setSsx(value);
        } else if (JSX_PROPERTY_NAME.equals(key)) {
            iconSet.setJsx(value);
        } else if (RENDER_WIKI_PROPERTY_NAME.equals(key)) {
            iconSet.setRenderWiki(value);
        } else if (RENDER_HTML_PROPERTY_NAME.equals(key)) {
            iconSet.setRenderHTML(value);
        } else if (ICON_TYPE_PROPERTY_NAME.equals(key)) {
            iconSet.setType(IconType.valueOf(value.toUpperCase()));
        } else if (ICON_URL_PROPERTY_NAME.equals(key)) {
            iconSet.setUrl(value);
        } else if (ICON_CSS_CLASS_PROPERTY_NAME.equals(key)) {
            iconSet.setCssClass(value);
        } else {
            Icon icon = new Icon();
            icon.setValue(value);
            iconSet.addIcon(key, icon);
        }
    }
}
