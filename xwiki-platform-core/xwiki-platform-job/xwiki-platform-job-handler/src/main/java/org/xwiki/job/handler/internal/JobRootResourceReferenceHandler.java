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
package org.xwiki.job.handler.internal;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.model.ModelContext;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.resource.AbstractResourceReferenceHandler;
import org.xwiki.resource.ResourceReference;
import org.xwiki.resource.ResourceReferenceHandlerChain;
import org.xwiki.resource.ResourceReferenceHandlerException;
import org.xwiki.resource.ResourceType;
import org.xwiki.resource.annotations.Authenticate;
import org.xwiki.url.internal.ParentResourceReference;

/**
 * Job root resource handler.
 *
 * @version $Id$
 * @since 10.2
 */
@Component
@Named(JobRootResourceReferenceHandler.HINT)
@Singleton
@Authenticate
public class JobRootResourceReferenceHandler extends AbstractResourceReferenceHandler<ResourceType>
{
    /**
     * The role hint to use for job related resource handler.
     */
    public static final String HINT = "job";

    /**
     * Represents a Job Resource Type.
     */
    public static final ResourceType TYPE = new ResourceType(HINT);

    @Inject
    @Named("context")
    private Provider<ComponentManager> componentManagerProvider;

    @Inject
    private ModelContext modelContext;

    @Override
    public List<ResourceType> getSupportedResourceReferences()
    {
        return Arrays.asList(TYPE);
    }

    @Override
    public void handle(ResourceReference resourceReference, ResourceReferenceHandlerChain chain)
        throws ResourceReferenceHandlerException
    {
        ParentResourceReference reference = (ParentResourceReference) resourceReference;

        if ("wiki".equals(reference.getChild()) && !reference.getPathSegments().isEmpty()) {
            String wiki = reference.getPathSegments().get(0);

            this.modelContext.setCurrentEntityReference(new WikiReference(wiki));

            String child;
            if (reference.getPathSegments().size() > 1) {
                child = reference.getPathSegments().get(1);
            } else {
                child = "";
            }

            List<String> pathSegments = reference.getPathSegments();
            if (pathSegments.size() > 2) {
                pathSegments = pathSegments.subList(2, pathSegments.size());
            } else {
                pathSegments = Collections.emptyList();
            }

            handleChild(new ParentResourceReference(reference.getType(), reference.getRootPath(), child, pathSegments));
        } else {
            handleChild(reference);
        }

        // Be a good citizen, continue the chain, in case some lower-priority Handler has something to do for this
        // Resource Reference.
        chain.handleNext(reference);
    }

    private void handleChild(ParentResourceReference reference) throws ResourceReferenceHandlerException
    {
        if (StringUtils.isNotEmpty(reference.getChild())) {
            ComponentManager componentManager = this.componentManagerProvider.get();

            if (componentManager.hasComponent(JobResourceReferenceHandler.class, reference.getChild())) {
                JobResourceReferenceHandler child;
                try {
                    child = componentManager.getInstance(JobResourceReferenceHandler.class, reference.getChild());
                } catch (ComponentLookupException e) {
                    throw new ResourceReferenceHandlerException(
                        "Failed to initialize job resource handler with hint [" + reference.getChild() + "]");
                }

                child.handle(reference);
            } else {
                throw new ResourceReferenceHandlerException(
                    "Unknow job resource handler with hint [" + reference.getChild() + "]");
            }
        } else {
            // TODO: put some explanation about the various services provided by the job resource handler
        }
    }
}
