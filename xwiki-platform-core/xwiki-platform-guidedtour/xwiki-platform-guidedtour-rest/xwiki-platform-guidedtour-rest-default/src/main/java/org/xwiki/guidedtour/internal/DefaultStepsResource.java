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
package org.xwiki.guidedtour.internal;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.xwiki.component.annotation.Component;
import org.xwiki.guidedtour.api.dtos.StepDTO;
import org.xwiki.guidedtour.rest.StepsResource;
import org.xwiki.rest.XWikiRestException;
import org.xwiki.security.authorization.Right;

/**
 * Default implementation of {@link StepsResource}.
 *
 * @version $Id$
 * @since 18.4.0RC1
 */
@Component
@Named("org.xwiki.guidedtour.internal.DefaultStepsResource")
@Singleton
public class DefaultStepsResource extends AbstractGuidedTourResource implements StepsResource
{
    @Inject
    private StepsManager stepsManager;

    @Override
    public Response getTaskSteps(String tourId, String taskId) throws XWikiRestException
    {
        return execute("Steps API: retrieving the steps for task [{}] from tour [{}].", () -> {
            List<StepDTO> tasks = this.stepsManager.getAllSteps(tourId, taskId);
            return Response.ok(tasks).type(MediaType.APPLICATION_JSON_TYPE).build();
        }, taskId, tourId);
    }

    @Override
    public Response createStep(String tourId, String taskId, StepDTO stepDTO) throws XWikiRestException
    {
        return execute("Steps API: creating step for task [{}] from tour [{}].", () -> {
            this.stepsManager.createStep(tourId, taskId, stepDTO);
            return Response.status(Response.Status.CREATED).build();
        }, taskId, tourId);
    }

    @Override
    public Response updateStep(String tourId, String taskId, int stepId, StepDTO stepDTO) throws XWikiRestException
    {
        return execute("Steps API: updating step on position [{}] for task [{}] from tour [{}].", () -> {
            this.stepsManager.updateStep(tourId, taskId, stepId, stepDTO);
            return Response.ok().build();
        }, stepId, taskId, tourId);
    }

    @Override
    public Response deleteStep(String tourId, String taskId, int stepId) throws XWikiRestException
    {
        return execute("Steps API: removing step on position [{}] for task [{}] from tour [{}].", () -> {
            this.contextualAuthorizationManager.checkAccess(Right.DELETE);
            this.stepsManager.deleteStep(tourId, taskId, stepId);
            return Response.ok().build();
        }, stepId, taskId, tourId);
    }
}
