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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.xwiki.component.annotation.Component;
import org.xwiki.guidedtour.api.dtos.UserTourStatusDTO;
import org.xwiki.guidedtour.rest.UserTourResource;

/**
 * Default implementation of {@link UserTourResource}.
 *
 * @version $Id$
 * @since 18.4.0RC1
 */
@Component
@Named("org.xwiki.guidedtour.internal.DefaultUserTourResource")
@Singleton
public class DefaultUserTourResource extends AbstractGuidedTourResource implements UserTourResource
{
    @Inject
    private UserStatusManager userStatusManager;

    @Override
    public Response getUserTourStatus()
    {
        return execute("User tour status API: getting user tour status object.", () -> {
            UserTourStatusDTO json = this.userStatusManager.getUserToursStatus();
            return Response.ok(json).type(MediaType.APPLICATION_JSON_TYPE).build();
        });
    }

    @Override
    public Response createUserTourStatus()
    {
        return execute("User tour status API: creating new user tour status object.", () -> {
            this.userStatusManager.createUserTourStatus();
            return Response.status(Response.Status.CREATED).build();
        });
    }

    @Override
    public Response updateUserTourStatus(UserTourStatusDTO userTourStatus)
    {
        return execute("User tour status API: updating user tour status object.", () -> {
            this.userStatusManager.updateUserTourStatus(userTourStatus);
            return Response.ok().build();
        });
    }
}
