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

import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.bridge.DocumentModelBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.icon.Icon;
import org.xwiki.icon.IconException;
import org.xwiki.icon.IconSet;
import org.xwiki.icon.IconSetLoader;
import org.xwiki.icon.IconType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceSerializer;

/**
 * Default implementation of {@link org.xwiki.icon.IconSetLoader}.
 *
 * @since 6.2M1
 * @version $Id$
 */
@Component
public class DefaultIconSetLoader implements IconSetLoader
{
    private static final String CSS_PROPERTY_NAME = "xwiki.iconset.css";

    private static final String SSX_PROPERTY_NAME = "xwiki.iconset.ssx";

    private static final String JSX_PROPERTY_NAME = "xwiki.iconset.jsx";

    private static final String RENDER_WIKI_PROPERTY_NAME = "xwiki.iconset.render.wiki";

    private static final String RENDER_HTML_PROPERTY_NAME = "xwiki.iconset.render.html";

    private static final String ICON_TYPE_PROPERTY_NAME = "xwiki.iconset.type";

    private static final String ERROR_MSG = "Failed to load the IconSet [%s].";

    @Inject
    private DocumentAccessBridge documentAccessBridge;

    @Inject
    private EntityReferenceSerializer<String> entityReferenceSerializer;

    @Override
    public IconSet loadIconSet(DocumentReference iconSetReference) throws IconException
    {
        String name = entityReferenceSerializer.serialize(iconSetReference);
        try {
            // Get the document
            DocumentModelBridge doc = documentAccessBridge.getDocument(iconSetReference);
            String content = doc.getContent();
            return loadIconSet(new StringReader(content), name);
        } catch (Exception e) {
            throw new IconException(String.format(ERROR_MSG, name), e);
        }
    }

    @Override
    public IconSet loadIconSet(Reader input, String name) throws IconException
    {
        try {
            IconSet iconSet = new IconSet(name);

            Properties properties = new Properties();
            properties.load(input);

            // Load all the keys
            for (String key : properties.stringPropertyNames()) {
                String value = properties.getProperty(key);
                if (key.equals(CSS_PROPERTY_NAME)) {
                    iconSet.setCss(value);
                } else if (key.equals(SSX_PROPERTY_NAME)) {
                    iconSet.setSsx(value);
                } else if (key.equals(JSX_PROPERTY_NAME)) {
                    iconSet.setJsx(value);
                } else if (key.equals(RENDER_WIKI_PROPERTY_NAME)) {
                    iconSet.setRenderWiki(value);
                } else if (key.equals(RENDER_HTML_PROPERTY_NAME)) {
                    iconSet.setRenderHTML(value);
                } else if (key.equals(ICON_TYPE_PROPERTY_NAME)) {
                    iconSet.setType(IconType.valueOf(value.toUpperCase()));
                } else {
                    Icon icon = new Icon();
                    icon.setValue(properties.getProperty(key));
                    iconSet.addIcon(key, icon);
                }
            }

            // return
            return iconSet;
        } catch (IOException e) {
            throw new IconException(String.format(ERROR_MSG, name), e);
        }
    }
}
