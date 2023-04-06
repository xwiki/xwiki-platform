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
package org.xwiki.security.authservice;

import org.xwiki.component.annotation.Role;
import org.xwiki.stability.Unstable;

import com.xpn.xwiki.user.api.XWikiAuthService;

/**
 * Expose {@link XWikiAuthService} instances as components.
 * <p>
 * A name and a description should also be exposed through translations using key of the following form:
 * <ul>
 * <li>{@code security.authservice.<id>.name for the name}</li>
 * <li>{@code security.authservice.<id>.description for the description}</li>
 * </ul>
 * 
 * @version $Id$
 * @since 15.3RC1
 */
@Role
@Unstable
public interface XWikiAuthServiceComponent extends XWikiAuthService
{
    /**
     * @return the identifier of the authenticator, used as component role hint
     */
    String getId();
}
