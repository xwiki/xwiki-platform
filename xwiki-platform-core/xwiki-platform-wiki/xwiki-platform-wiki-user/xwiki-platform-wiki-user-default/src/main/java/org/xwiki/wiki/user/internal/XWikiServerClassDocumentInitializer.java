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
package org.xwiki.wiki.user.internal;

import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.wiki.user.MembershipType;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.classes.BaseClass;

/**
 * Update the XWiki.XWikiServerClass document with all required information.
 *
 * @since 5.3M2
 */
@Component
@Named("XWiki.XWikiServerClassUser")
@Singleton
public class XWikiServerClassDocumentInitializer extends
        org.xwiki.wiki.internal.descriptor.document.XWikiServerClassDocumentInitializer
{
    /**
     * Name of field <code>membershipType</code> for the XWiki class XWiki.XWikiServerClass.
     */
    public static final String FIELD_MEMBERSHIPTYPE = "membershipType";

    /**
     * Pretty name of field <code>membershipType</code> for the XWiki class XWiki.XWikiServerClass.
     */
    public static final String FIELDPN_MEMBERSHIPTYPE = "Membership Type";

    /**
     * List of possible values for <code>membershipType</code> for the XWiki class XWiki.XWikiServerClass.
     */
    public static final String FIELDL_MEMBERSHIPTYPE = MembershipType.OPEN.name().toLowerCase() +
            DEFAULT_FIELDS_SEPARATOR + MembershipType.REQUEST.name().toLowerCase() + DEFAULT_FIELDS_SEPARATOR +
            MembershipType.INVITE.name().toLowerCase();

    /**
     * Display type of field <code>membershipType</code> for the XWiki class XWiki.XWikiServerClass.
     */
    public static final String FIELDDT_MEMBERSHIPTYPE = "radio";

    /**
     * Name of field <code>enableLocalUsers</code> for the XWiki class XWiki.XWikiServerClass.
     */
    public static final String FIELD_ENABLELOCALUSERS = "enableLocalUsers";

    /**
     * Pretty name of field <code>enableLocalUsers</code> for the XWiki class XWiki.XWikiServerClass.
     */
    public static final String FIELDPN_ENABLELOCALUSERS = "Enable local users";

    /**
     * Display type of field <code>enableLocalUsers</code> for the XWiki class XWiki.XWikiServerClass.
     */
    public static final String FIELDDT_ENABLELOCALUSERS = "checkbox";

    /**
     * Default value of field <code>enableLocalUsers</code> for the XWiki class XWiki.XWikiServerClass.
     */
    public static final Boolean DEFAULT_ENABLELOCALUSERS = Boolean.FALSE;

    @Override
    public boolean updateDocument(XWikiDocument document)
    {
        boolean needsUpdate = false;

        // Add missing class fields
        BaseClass baseClass = document.getXClass();

        needsUpdate |= baseClass.addStaticListField(FIELD_MEMBERSHIPTYPE, FIELDPN_MEMBERSHIPTYPE,
                MembershipType.values().length, false, FIELDL_MEMBERSHIPTYPE, FIELDDT_MEMBERSHIPTYPE);
        needsUpdate |= baseClass.addBooleanField(FIELD_ENABLELOCALUSERS, FIELDPN_ENABLELOCALUSERS,
                FIELDDT_ENABLELOCALUSERS);
        needsUpdate |= updateBooleanClassDefaultValue(baseClass, FIELD_ENABLELOCALUSERS, DEFAULT_ENABLELOCALUSERS);

        return needsUpdate;
    }
}
