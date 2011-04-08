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
package org.xwiki.gwt.wysiwyg.client.widget.wizard.util;

import org.xwiki.gwt.user.client.StringUtils;
import org.xwiki.gwt.wysiwyg.client.wiki.Entity;
import org.xwiki.gwt.wysiwyg.client.wiki.EntityConfig;
import org.xwiki.gwt.wysiwyg.client.wiki.EntityLink;
import org.xwiki.gwt.wysiwyg.client.wiki.ResourceReference;
import org.xwiki.gwt.wysiwyg.client.wiki.WikiServiceAsync;

import com.google.gwt.user.client.rpc.AsyncCallback;

/**
 * Wizard step that allows the user to select an entity from a list.
 * 
 * @version $Id$
 * @param <C> the type of entity configuration data associated with the link
 * @param <E> the type of entity that is being selected
 */
public abstract class AbstractEntityListSelectorWizardStep<C extends EntityConfig, E extends Entity> extends
    AbstractListSelectorWizardStep<EntityLink<C>, E>
{
    /**
     * The service used to serialzie entity references.
     */
    private final WikiServiceAsync wikiService;

    /**
     * Creates a new entity selector that allows the user to select the entity to link to from a list.
     * 
     * @param wikiService the service used to serialize entity references
     */
    public AbstractEntityListSelectorWizardStep(WikiServiceAsync wikiService)
    {
        this.wikiService = wikiService;
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractListSelectorWizardStep#isSelectedByDefault(Object)
     */
    @Override
    protected boolean isSelectedByDefault(E listItemData)
    {
        return listItemData.getReference().equals(getData().getDestination().getEntityReference());
    };

    /**
     * {@inheritDoc}
     * 
     * @see AbstractListSelectorWizardStep#saveSelectedValue(AsyncCallback)
     */
    @Override
    protected void saveSelectedValue(final AsyncCallback<Boolean> async)
    {
        final E selectedEntity = getSelectedItem().getData();
        if (selectedEntity == null) {
            getData().getDestination().setEntityReference(getData().getOrigin().clone());
            async.onSuccess(true);
        } else if (!StringUtils.isEmpty(getData().getData().getReference())
            && getData().getDestination().getEntityReference().equals(selectedEntity.getReference())) {
            async.onSuccess(true);
        } else {
            final ResourceReference destination = getData().getDestination().clone();
            destination.setEntityReference(selectedEntity.getReference().clone());
            wikiService.getEntityConfig(getData().getOrigin(), destination, new AsyncCallback<EntityConfig>()
            {
                public void onFailure(Throwable caught)
                {
                    async.onFailure(caught);
                }

                public void onSuccess(EntityConfig result)
                {
                    getData().setDestination(destination);
                    getData().getData().setReference(result.getReference());
                    getData().getData().setUrl(result.getUrl());
                    async.onSuccess(true);
                }
            });
        }
    }

    /**
     * @return the service used to serialize entity references
     */
    public WikiServiceAsync getWikiService()
    {
        return wikiService;
    }
}
