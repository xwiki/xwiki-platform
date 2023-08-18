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

import org.xwiki.configuration.internal.XWikiPropertiesConfigurationSource;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.user.internal.ConfiguredStringUserReferenceSerializer;
import org.xwiki.user.internal.CurrentConfiguredStringUserReferenceResolver;
import org.xwiki.user.internal.DefaultConfiguredStringUserReferenceResolver;
import org.xwiki.user.internal.DefaultUserConfiguration;
import org.xwiki.user.internal.document.CurrentDocumentStringUserReferenceResolver;
import org.xwiki.user.internal.document.CurrentUserReferenceResolver;
import org.xwiki.user.internal.document.DocumentDocumentReferenceUserReferenceResolver;
import org.xwiki.user.internal.document.DocumentDocumentReferenceUserReferenceSerializer;
import org.xwiki.user.internal.document.DocumentStringUserReferenceSerializer;
import org.xwiki.user.internal.document.XWikiUserUserReferenceResolver;

import com.xpn.xwiki.test.reference.ReferenceComponentList;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Default components related to user references for tests.
 * Note that some components might still be missing, those should be added when needed.
 *
 * @version $Id$
 * @since 14.10
 * @since 13.10.11
 * @since 14.4.7
 */
@Documented
@Retention(RUNTIME)
@Target({ TYPE, METHOD, ANNOTATION_TYPE })
@ComponentList({
    DocumentDocumentReferenceUserReferenceSerializer.class,
    CurrentUserReferenceResolver.class,
    DocumentDocumentReferenceUserReferenceResolver.class,
    ConfiguredStringUserReferenceSerializer.class,
    DefaultConfiguredStringUserReferenceResolver.class,
    XWikiUserUserReferenceResolver.class,
    DocumentStringUserReferenceSerializer.class,
    CurrentConfiguredStringUserReferenceResolver.class,
    CurrentDocumentStringUserReferenceResolver.class,

    // Needed for the ConfiguredXXX component
    DefaultUserConfiguration.class,
    XWikiPropertiesConfigurationSource.class
})
@Inherited
@ReferenceComponentList
public @interface UserReferenceComponentList
{
}
