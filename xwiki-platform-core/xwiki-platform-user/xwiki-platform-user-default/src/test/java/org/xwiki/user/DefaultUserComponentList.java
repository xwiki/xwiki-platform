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
package org.xwiki.user;

import java.lang.annotation.Documented;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import org.xwiki.configuration.internal.DocumentsConfigurationSource;
import org.xwiki.configuration.internal.SpacesConfigurationSource;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.user.internal.AllGuestConfigurationSource;
import org.xwiki.user.internal.AllSuperAdminConfigurationSource;
import org.xwiki.user.internal.DefaultConfiguredStringUserReferenceResolver;
import org.xwiki.user.internal.DefaultUserManager;
import org.xwiki.user.internal.GuestConfigurationSource;
import org.xwiki.user.internal.SecureAllUserPropertiesResolver;
import org.xwiki.user.internal.SecureUserPropertiesResolver;
import org.xwiki.user.internal.SuperAdminConfigurationSource;
import org.xwiki.user.internal.document.NormalUserConfigurationSourceAuthorization;
import org.xwiki.user.internal.document.NormalUserPreferencesConfigurationSource;
import org.xwiki.user.internal.document.SecureUserDocumentUserPropertiesResolver;
import org.xwiki.user.internal.document.UserPreferencesConfigurationSource;
import org.xwiki.user.internal.group.DefaultGroupManager;
import org.xwiki.user.internal.group.GroupsCache;
import org.xwiki.user.internal.group.MembersCache;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Pack of default Components that are needed for the User API.
 *
 * @version $Id$
 * @since 13.9RC1
 * @since 13.4.4
 */
@Documented
@Retention(RUNTIME)
@Target({ TYPE, METHOD, ANNOTATION_TYPE })
@ComponentList({
    // User Script Service
    SecureUserPropertiesResolver.class,
    SuperAdminConfigurationSource.class,
    GuestConfigurationSource.class,
    SecureAllUserPropertiesResolver.class,
    AllSuperAdminConfigurationSource.class,
    DocumentsConfigurationSource.class,
    AllGuestConfigurationSource.class,
    DefaultUserManager.class,
    DefaultConfiguredStringUserReferenceResolver.class,
    SecureUserDocumentUserPropertiesResolver.class,
    UserPreferencesConfigurationSource.class,
    NormalUserPreferencesConfigurationSource.class,
    NormalUserConfigurationSourceAuthorization.class,
    // Group Script Service
    DefaultGroupManager.class,
    GroupsCache.class,
    MembersCache.class,
    SpacesConfigurationSource.class
})
@Inherited
@UserReferenceComponentList
public @interface DefaultUserComponentList
{
}
