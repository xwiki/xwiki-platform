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
package com.xpn.xwiki.test.reference;

import java.lang.annotation.Documented;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import org.xwiki.model.internal.DefaultModelConfiguration;
import org.xwiki.model.internal.reference.DefaultDocumentReferenceProvider;
import org.xwiki.model.internal.reference.DefaultEntityReferenceProvider;
import org.xwiki.model.internal.reference.DefaultPageReferenceProvider;
import org.xwiki.model.internal.reference.DefaultReferenceDocumentReferenceResolver;
import org.xwiki.model.internal.reference.DefaultReferenceEntityReferenceResolver;
import org.xwiki.model.internal.reference.DefaultReferencePageAttachmentReferenceResolver;
import org.xwiki.model.internal.reference.DefaultReferencePageReferenceResolver;
import org.xwiki.model.internal.reference.DefaultSpaceReferenceProvider;
import org.xwiki.model.internal.reference.DefaultStringDocumentReferenceResolver;
import org.xwiki.model.internal.reference.DefaultStringEntityReferenceResolver;
import org.xwiki.model.internal.reference.DefaultStringEntityReferenceSerializer;
import org.xwiki.model.internal.reference.DefaultStringPageAttachmentReferenceResolver;
import org.xwiki.model.internal.reference.DefaultStringPageObjectPropertyReferenceResolver;
import org.xwiki.model.internal.reference.DefaultStringPageObjectReferenceResolver;
import org.xwiki.model.internal.reference.DefaultStringPageReferenceResolver;
import org.xwiki.model.internal.reference.DefaultSymbolScheme;
import org.xwiki.model.internal.reference.DefaultWikiReferenceProvider;
import org.xwiki.model.internal.reference.EntityReferenceFactory;
import org.xwiki.model.internal.reference.ExplicitReferenceDocumentReferenceResolver;
import org.xwiki.model.internal.reference.ExplicitReferenceEntityReferenceResolver;
import org.xwiki.model.internal.reference.ExplicitReferencePageReferenceResolver;
import org.xwiki.model.internal.reference.ExplicitStringAttachmentReferenceResolver;
import org.xwiki.model.internal.reference.ExplicitStringDocumentReferenceResolver;
import org.xwiki.model.internal.reference.ExplicitStringEntityReferenceResolver;
import org.xwiki.model.internal.reference.ExplicitStringPageAttachmentReferenceResolver;
import org.xwiki.model.internal.reference.ExplicitStringPageReferenceResolver;
import org.xwiki.model.internal.reference.LocalStringEntityReferenceSerializer;
import org.xwiki.model.internal.reference.LocalUidStringEntityReferenceSerializer;
import org.xwiki.model.internal.reference.RelativeStringEntityReferenceResolver;
import org.xwiki.model.internal.reference.UidStringEntityReferenceSerializer;
import org.xwiki.model.internal.reference.WithParametersStringEntityReferenceResolver;
import org.xwiki.model.internal.reference.WithParametersStringEntityReferenceSerializer;
import org.xwiki.model.internal.reference.WithPatametersSymbolScheme;
import org.xwiki.model.internal.reference.WithTypeAndParametersStringEntityReferenceSerializer;
import org.xwiki.model.internal.reference.WithTypeStringEntityReferenceSerializer;
import org.xwiki.test.annotation.ComponentList;

import com.xpn.xwiki.internal.model.reference.CompactStringEntityReferenceSerializer;
import com.xpn.xwiki.internal.model.reference.CompactWikiStringEntityReferenceSerializer;
import com.xpn.xwiki.internal.model.reference.CurrentAttachmentReferenceResolver;
import com.xpn.xwiki.internal.model.reference.CurrentEntityReferenceProvider;
import com.xpn.xwiki.internal.model.reference.CurrentEntityUserAndGroupDocumentReferenceResolver;
import com.xpn.xwiki.internal.model.reference.CurrentEntityUserAndGroupEntityReferenceResolver;
import com.xpn.xwiki.internal.model.reference.CurrentGetDocumentDocumentReferenceResolver;
import com.xpn.xwiki.internal.model.reference.CurrentGetPagePageReferenceResolver;
import com.xpn.xwiki.internal.model.reference.CurrentMixedEntityReferenceProvider;
import com.xpn.xwiki.internal.model.reference.CurrentMixedReferenceDocumentReferenceResolver;
import com.xpn.xwiki.internal.model.reference.CurrentMixedReferenceEntityReferenceResolver;
import com.xpn.xwiki.internal.model.reference.CurrentMixedStringDocumentReferenceResolver;
import com.xpn.xwiki.internal.model.reference.CurrentPageAttachmentReferenceResolver;
import com.xpn.xwiki.internal.model.reference.CurrentPageReferenceProvider;
import com.xpn.xwiki.internal.model.reference.CurrentReferenceDocumentReferenceResolver;
import com.xpn.xwiki.internal.model.reference.CurrentReferenceEntityReferenceResolver;
import com.xpn.xwiki.internal.model.reference.CurrentReferenceObjectReferenceResolver;
import com.xpn.xwiki.internal.model.reference.CurrentReferencePageObjectReferenceResolver;
import com.xpn.xwiki.internal.model.reference.CurrentReferencePageReferenceResolver;
import com.xpn.xwiki.internal.model.reference.CurrentStringAttachmentReferenceResolver;
import com.xpn.xwiki.internal.model.reference.CurrentStringDocumentReferenceResolver;
import com.xpn.xwiki.internal.model.reference.CurrentStringEntityReferenceResolver;
import com.xpn.xwiki.internal.model.reference.CurrentStringPageAttachmentReferenceResolver;
import com.xpn.xwiki.internal.model.reference.CurrentStringPageReferenceResolver;
import com.xpn.xwiki.internal.model.reference.CurrentStringSpaceReferenceResolver;
import com.xpn.xwiki.internal.model.reference.CurrentUserAndGroupDocumentReferenceResolver;
import com.xpn.xwiki.internal.model.reference.CurrentUserAndGroupEntityReferenceResolver;
import com.xpn.xwiki.internal.model.reference.UserAndGroupDocumentReferenceResolver;
import com.xpn.xwiki.internal.model.reference.UserAndGroupEntityReferenceResolver;
import com.xpn.xwiki.internal.model.reference.XClassRelativeStringEntityReferenceResolver;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Pack of default Component implementations that are needed for rendering wiki pages.
 *
 * @version $Id$
 * @since 7.3M1
 */
@Documented
@Retention(RUNTIME)
@Target({ TYPE, METHOD, ANNOTATION_TYPE })
@ComponentList({
    CompactStringEntityReferenceSerializer.class,
    CompactWikiStringEntityReferenceSerializer.class,
    CurrentAttachmentReferenceResolver.class,
    CurrentEntityReferenceProvider.class,
    CurrentMixedEntityReferenceProvider.class,
    CurrentMixedReferenceDocumentReferenceResolver.class,
    CurrentMixedReferenceEntityReferenceResolver.class,
    CurrentMixedStringDocumentReferenceResolver.class,
    CurrentReferenceDocumentReferenceResolver.class,
    CurrentReferenceEntityReferenceResolver.class,
    CurrentStringAttachmentReferenceResolver.class,
    CurrentStringDocumentReferenceResolver.class,
    CurrentStringEntityReferenceResolver.class,
    CurrentStringSpaceReferenceResolver.class,
    DefaultDocumentReferenceProvider.class,
    DefaultSpaceReferenceProvider.class,
    DefaultStringDocumentReferenceResolver.class,
    DefaultStringEntityReferenceResolver.class,
    DefaultStringEntityReferenceSerializer.class,
    DefaultWikiReferenceProvider.class,
    DefaultEntityReferenceProvider.class,
    ExplicitReferenceDocumentReferenceResolver.class,
    ExplicitReferenceEntityReferenceResolver.class,
    ExplicitStringAttachmentReferenceResolver.class,
    ExplicitStringDocumentReferenceResolver.class,
    ExplicitStringEntityReferenceResolver.class,
    LocalStringEntityReferenceSerializer.class,
    LocalUidStringEntityReferenceSerializer.class,
    RelativeStringEntityReferenceResolver.class,
    UidStringEntityReferenceSerializer.class,
    XClassRelativeStringEntityReferenceResolver.class,
    CurrentGetDocumentDocumentReferenceResolver.class,
    DefaultReferenceDocumentReferenceResolver.class,
    DefaultReferenceEntityReferenceResolver.class,
    DefaultSymbolScheme.class,
    CurrentReferenceObjectReferenceResolver.class,
    CurrentPageAttachmentReferenceResolver.class,
    CurrentPageReferenceProvider.class,
    CurrentGetPagePageReferenceResolver.class,
    DefaultPageReferenceProvider.class,
    DefaultReferencePageAttachmentReferenceResolver.class,
    DefaultReferencePageReferenceResolver.class,
    DefaultStringPageAttachmentReferenceResolver.class,
    DefaultStringPageObjectPropertyReferenceResolver.class,
    DefaultStringPageObjectReferenceResolver.class,
    DefaultStringPageReferenceResolver.class,
    ExplicitReferencePageReferenceResolver.class,
    ExplicitStringPageAttachmentReferenceResolver.class,
    ExplicitStringPageReferenceResolver.class,
    CurrentStringPageAttachmentReferenceResolver.class,
    CurrentStringPageReferenceResolver.class,
    CurrentReferencePageObjectReferenceResolver.class,
    CurrentReferencePageReferenceResolver.class,
    CurrentEntityUserAndGroupDocumentReferenceResolver.class,
    CurrentEntityUserAndGroupEntityReferenceResolver.class,
    CurrentUserAndGroupDocumentReferenceResolver.class,
    CurrentUserAndGroupEntityReferenceResolver.class,
    UserAndGroupDocumentReferenceResolver.class,
    UserAndGroupEntityReferenceResolver.class,
    WithParametersStringEntityReferenceResolver.class,
    WithParametersStringEntityReferenceSerializer.class,
    WithPatametersSymbolScheme.class,
    WithTypeAndParametersStringEntityReferenceSerializer.class,
    WithTypeStringEntityReferenceSerializer.class,

    DefaultModelConfiguration.class,
    EntityReferenceFactory.class
})
@Inherited
public @interface ReferenceComponentList
{
}
