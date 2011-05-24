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
package org.xwiki.gwt.wysiwyg.client.plugin.alfresco;

import java.util.ArrayList;
import java.util.List;

import org.xwiki.gwt.user.client.ui.ListItem;
import org.xwiki.gwt.wysiwyg.client.Strings;
import org.xwiki.gwt.wysiwyg.client.wiki.EntityReference;

import com.google.gwt.user.client.ui.Image;

/**
 * Wizard step that selects an Alfresco image.
 * 
 * @version $Id$
 */
public class AlfrescoImageSelectorWizardStep extends AlfrescoEntitySelectorWizardStep
{
    /**
     * Creates a new instance.
     * 
     * @param alfrescoService the service used to access an Alfresco content management system
     */
    public AlfrescoImageSelectorWizardStep(AlfrescoServiceAsync alfrescoService)
    {
        super(alfrescoService);

        setStepTitle(Strings.INSTANCE.imageSelectImageTitle());
    }

    /**
     * {@inheritDoc}
     * 
     * @see AlfrescoEntitySelectorWizardStep#fill(List, EntityReference)
     */
    @Override
    protected ListItem<AlfrescoEntity> fill(List<AlfrescoEntity> children, EntityReference selectedChildReference)
    {
        // Keep only folders and images.
        List<AlfrescoEntity> foldersAndImages = new ArrayList<AlfrescoEntity>();
        for (AlfrescoEntity child : children) {
            if (child.getMediaType() == null || child.getMediaType().startsWith("image/")) {
                foldersAndImages.add(child);
            }
        }

        return super.fill(foldersAndImages, selectedChildReference);
    }

    /**
     * {@inheritDoc}
     * 
     * @see AlfrescoEntitySelectorWizardStep#createListItem(AlfrescoEntity)
     */
    @Override
    protected ListItem<AlfrescoEntity> createListItem(AlfrescoEntity entity)
    {
        ListItem<AlfrescoEntity> item = super.createListItem(entity);
        if (entity.getMediaType() != null) {
            item.clear();
            Image image = new Image(entity.getPreviewURL());
            image.setTitle(entity.getName());
            item.add(image);
            item.addStyleName("preview");
        }
        return item;
    }
}
