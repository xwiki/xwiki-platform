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
package org.xwiki.netflux.internal;

import java.io.IOException;

import org.xwiki.component.annotation.Component;
import org.xwiki.netflux.internal.user.UserHandler;

/**
 * {@link UserHandler} implementation for {@link RemoteUser}.
 * 
 * @version $Id$
 * @since 17.10.0RC1
 */
@Component
public class RemoteUserHandler implements UserHandler<RemoteUser>
{
    @SuppressWarnings("resource")
    @Override
    public void sendText(RemoteUser user, String text) throws NetfluxException
    {
        if (text == null) {
            throw new IllegalArgumentException("Text must not be null");
        }

        TODO
    }
}
