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
package org.xwiki.user.internal.converter;

import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.user.CurrentUserReference;
import org.xwiki.user.SuperAdminUserReference;

/**
 * A bridge to call {@link UserReferenceConverter} for a {@link CurrentUserReference}.
 * 
 * @version $Id$
 * @since 13.10.4
 * @since 14.2
 */
@Component
@Singleton
public class SuperAdminUserReferenceConverter extends AbstractUserReferenceConverter<SuperAdminUserReference>
{

}
