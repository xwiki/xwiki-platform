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

import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.icon.IconException;
import org.xwiki.icon.IconManager;
import org.xwiki.icon.IconSet;
import org.xwiki.icon.IconSetManager;
import org.xwiki.icon.rest.IconThemesResource;
import org.xwiki.icon.rest.model.jaxb.Icon;
import org.xwiki.icon.rest.model.jaxb.Icons;
import org.xwiki.icon.rest.model.jaxb.ObjectFactory;
import org.xwiki.model.ModelContext;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.rest.XWikiRestComponent;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;
import org.xwiki.wiki.manager.WikiManagerException;

import static org.xwiki.icon.IconManager.META_DATA_CSS_CLASS;
import static org.xwiki.icon.IconManager.META_DATA_ICON_SET_TYPE;
import static org.xwiki.icon.IconManager.META_DATA_URL;

/**
 * Default implementation of {@link IconThemesResource}.
 *
 * @version $Id$
 * @since 13.4RC1
 */
@Component
@Named("org.xwiki.icon.internal.DefaultIconThemesResource")
@Singleton
public class DefaultIconThemesResource implements IconThemesResource, XWikiRestComponent
{
    @Inject
    private ModelContext modelContext;

    @Inject
    private IconManager iconManager;

    @Inject
    private IconSetManager iconSetManager;
    
    @Inject
    private WikiDescriptorManager wikiDescriptorManager;

    @Override
    public Icons getIconsByTheme(String wikiId, String iconTheme, List<String> names)
    {
        return iconsByTheme(wikiId, iconTheme, names);
    }

    @Override
    public Icons getIcons(String wikiName, List<String> names)
    {
        return iconsByTheme(wikiName, null, names);
    }

    private Icons iconsByTheme(String wikiId, String iconTheme, List<String> iconNames)
    {
        wikiExists(wikiId);

        // Save the current entity reference. It will be restored on the method is finished.
        EntityReference oldEntityReference = this.modelContext.getCurrentEntityReference();
        try {
            // Set the requested wiki.
            this.modelContext.setCurrentEntityReference(new WikiReference(wikiId));

            IconSet iconSet = getIconSet(iconTheme);
            if (iconSet == null) {
                throw new WebApplicationException(Response.Status.NOT_FOUND);
            }
            String iconSetName = iconSet.getName();
            ObjectFactory objectFactory = new ObjectFactory();
            Icons icons = objectFactory.createIcons();
            for (String iconName : iconNames) {
                // First checks if the icon name is known.
                if (this.iconManager.hasIcon(iconSetName, iconName)) {
                    Map<String, Object> metaData = this.iconManager.getMetaData(iconName, iconSetName);
                    icons.getIcons().add(convertMapToIcon(objectFactory, metaData, iconName, iconSetName));
                } else if (iconName != null) {
                    // We only add non null icon names to the missing icons list.
                    icons.getMissingIcons().add(iconName);
                }
            }

            return icons;
        } catch (IconException e) {
            throw new WebApplicationException(e, Response.Status.INTERNAL_SERVER_ERROR);
        } finally {
            // Restore the old entity reference in the context.
            this.modelContext.setCurrentEntityReference(oldEntityReference);
        }
    }

    private IconSet getIconSet(String iconSetName) throws IconException
    {
        IconSet iconSet;
        if (iconSetName == null) {
            iconSet = this.iconSetManager.getCurrentIconSet();
            // Fallback to the default icon set if no current icon set is defined.
            if (iconSet == null) {
                iconSet = this.iconSetManager.getDefaultIconSet();
            }
        } else {
            iconSet = this.iconSetManager.getIconSet(iconSetName);
        }
        return iconSet;
    }

    private Icon convertMapToIcon(ObjectFactory objectFactory, Map<String, Object> metaData, String iconName,
        String setName)
    {
        Icon icon = objectFactory.createIcon();
        icon.setName(iconName);
        icon.setIconSetName(setName);
        icon.setIconSetType((String) metaData.get(META_DATA_ICON_SET_TYPE));
        String cssClass = (String) metaData.get(META_DATA_CSS_CLASS);
        if (StringUtils.isNotEmpty(cssClass)) {
            icon.setCssClass(cssClass);
        }
        String url = (String) metaData.get(META_DATA_URL);
        if (StringUtils.isNotEmpty(url)) {
            icon.setUrl(url);
        }
        return icon;
    }

    /**
     * Throw an exception if the wiki does not exist.
     *
     * @param wikiId a wiki id to validate
     */
    private void wikiExists(String wikiId)
    {
        try {
            if (!this.wikiDescriptorManager.exists(wikiId)) {
                throw new WebApplicationException(Response.Status.NOT_FOUND);
            }
        } catch (WikiManagerException e) {
            throw new WebApplicationException(e, Response.Status.INTERNAL_SERVER_ERROR);
        }
    }
}
