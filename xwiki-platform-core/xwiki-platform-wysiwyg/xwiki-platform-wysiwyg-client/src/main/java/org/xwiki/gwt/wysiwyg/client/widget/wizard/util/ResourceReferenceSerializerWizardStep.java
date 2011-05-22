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

import org.xwiki.gwt.user.client.ui.wizard.AbstractAutoSubmitWizardStep;
import org.xwiki.gwt.wysiwyg.client.wiki.EntityConfig;
import org.xwiki.gwt.wysiwyg.client.wiki.EntityLink;
import org.xwiki.gwt.wysiwyg.client.wiki.WikiServiceAsync;

import com.google.gwt.user.client.rpc.AsyncCallback;

/**
 * An auto-submit wizard step that serializes the resource reference from the input data.
 * 
 * @param <T> the type of entity configuration
 * @version $Id$
 */
public class ResourceReferenceSerializerWizardStep<T extends EntityConfig> extends
    AbstractAutoSubmitWizardStep<EntityLink<T>>
{
    /**
     * The service used to serialize the resource reference.
     */
    private final WikiServiceAsync wikiService;

    /**
     * Creates a new step that serializes the resource reference from the input data.
     * 
     * @param wikiService the service used to serialize the resource reference
     */
    public ResourceReferenceSerializerWizardStep(WikiServiceAsync wikiService)
    {
        this.wikiService = wikiService;
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractAutoSubmitWizardStep#onSubmit(AsyncCallback)
     */
    @Override
    public void onSubmit(final AsyncCallback<Boolean> callback)
    {
        if (getData().getData().getReference() != null) {
            // Resource reference already serialized.
            callback.onSuccess(Boolean.TRUE);
            return;
        }
        wikiService.getEntityConfig(getData().getOrigin(), getData().getDestination(),
            new AsyncCallback<EntityConfig>()
            {
                public void onFailure(Throwable caught)
                {
                    callback.onFailure(caught);
                }

                public void onSuccess(EntityConfig result)
                {
                    getData().getData().setReference(result.getReference());
                    getData().getData().setUrl(result.getUrl());
                    callback.onSuccess(Boolean.TRUE);
                }
            });
    }
}
