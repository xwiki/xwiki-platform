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
import org.xwiki.gwt.wysiwyg.client.wiki.EntityConfig;
import org.xwiki.gwt.wysiwyg.client.wiki.EntityLink;
import org.xwiki.gwt.wysiwyg.client.wiki.ResourceReference;
import org.xwiki.gwt.wysiwyg.client.wiki.WikiServiceAsync;

import com.google.gwt.user.client.rpc.AsyncCallback;

/**
 * Abstract wizard step that aggregates multiple views for selecting an entity to link to.
 * 
 * @param <C> the type of link configuration data associated with the link
 * @version $Id$
 */
public abstract class AbstractEntitySelectorAggregatorWizardStep<C extends EntityConfig> extends
    AbstractSelectorAggregatorWizardStep<EntityLink<C>>
{
    /**
     * The service used access the wiki.
     */
    private final WikiServiceAsync wikiService;

    /**
     * Creates a new entity selector wizard step that uses the given service to access the wiki.
     * 
     * @param wikiService the service used to access the wiki
     */
    public AbstractEntitySelectorAggregatorWizardStep(WikiServiceAsync wikiService)
    {
        this.wikiService = wikiService;
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractSelectorAggregatorWizardStep#init(Object, AsyncCallback)
     */
    @SuppressWarnings("unchecked")
    @Override
    public void init(Object data, final AsyncCallback< ? > cb)
    {
        final EntityLink<C> entityLink = (EntityLink<C>) data;
        if (StringUtils.isEmpty(entityLink.getData().getReference())) {
            entityLink.getDestination().setEntityReference(entityLink.getOrigin().clone());
            super.init(entityLink, cb);
        } else {
            wikiService.parseLinkReference(entityLink.getData().getReference(), entityLink.getOrigin(),
                new AsyncCallback<ResourceReference>()
                {
                    public void onFailure(Throwable caught)
                    {
                        cb.onFailure(caught);
                    }

                    public void onSuccess(ResourceReference result)
                    {
                        entityLink.setDestination(result);
                        AbstractEntitySelectorAggregatorWizardStep.super.init(entityLink, cb);
                    }
                });
        }
    }

    /**
     * @return the service used to access the wiki
     */
    public WikiServiceAsync getWikiService()
    {
        return wikiService;
    }
}
