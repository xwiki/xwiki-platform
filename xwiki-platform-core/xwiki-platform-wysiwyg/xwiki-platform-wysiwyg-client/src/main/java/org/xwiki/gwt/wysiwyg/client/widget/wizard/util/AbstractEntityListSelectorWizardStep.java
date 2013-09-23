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
        } else if (StringUtils.isEmpty(getData().getData().getReference())
            || !getData().getDestination().getEntityReference().equals(selectedEntity.getReference())) {
            getData().getDestination().setEntityReference(selectedEntity.getReference().clone());
            // We have a new target entity reference so we reset the previous entity configuration.
            getData().getData().setReference(null);
            getData().getData().setUrl(null);
        }
        async.onSuccess(true);
    }
}
