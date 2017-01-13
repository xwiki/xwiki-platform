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
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.wiki.user.MembershipType;
import org.xwiki.wiki.user.UserScope;

import com.xpn.xwiki.doc.AbstractMandatoryClassInitializer;
import com.xpn.xwiki.objects.classes.BaseClass;

/**
 * Update the WikiManager.WikiUserClass document with all required information.
 *
 * @since 5.3M2
 * @version $Id$
 */
@Component
@Named("WikiManager.WikiUserClass")
@Singleton
public class WikiUserClassDocumentInitializer extends AbstractMandatoryClassInitializer
{
    /**
     * The name of the mandatory document.
     */
    public static final String DOCUMENT_NAME = "WikiUserClass";

    /**
     * The space of the mandatory document.
     */
    public static final String DOCUMENT_SPACE = "WikiManager";

    /**
     * Reference to the server class.
     */
    public static final LocalDocumentReference CONFIGURATION_CLASS =
        new LocalDocumentReference(DOCUMENT_SPACE, DOCUMENT_NAME);

    /**
     * Default list separators of WikiManager.XWikiServerClass fields.
     */
    public static final String DEFAULT_FIELDS_SEPARATOR = "|";

    /**
     * Name of field <code>membershipType</code> for the XWiki class WikiManager.WikiUserClass.
     */
    public static final String FIELD_MEMBERSHIPTYPE = "membershipType";

    /**
     * Pretty name of field <code>membershipType</code> for the XWiki class WikiManager.WikiUserClass.
     */
    public static final String FIELDPN_MEMBERSHIPTYPE = "Membership Type";

    /**
     * List of possible values for <code>membershipType</code> for the XWiki class WikiManager.WikiUserClass.
     */
    public static final String FIELDL_MEMBERSHIPTYPE = MembershipType.OPEN.name().toLowerCase()
        + DEFAULT_FIELDS_SEPARATOR + MembershipType.REQUEST.name().toLowerCase() + DEFAULT_FIELDS_SEPARATOR
        + MembershipType.INVITE.name().toLowerCase();

    /**
     * Display type of field <code>membershipType</code> for the XWiki class WikiManager.WikiUserClass.
     */
    public static final String FIELDDT_MEMBERSHIPTYPE = "radio";

    /**
     * Name of field <code>userScope</code> for the XWiki class WikiManager.WikiUserClass.
     */
    public static final String FIELD_USERSCOPE = "userScope";

    /**
     * Pretty name of field <code>userScope</code> for the XWiki class WikiManager.WikiUserClass.
     */
    public static final String FIELDPN_USERSCOPE = "User scope";

    /**
     * Display type of field <code>userScope</code> for the XWiki class WikiManager.WikiUserClass.
     */
    public static final String FIELDDT_USERSCOPE = FIELDDT_MEMBERSHIPTYPE;

    /**
     * List of possible values for <code>userScope</code> for the XWiki class WikiManager.WikiUserClass.
     */
    public static final String FIELDL_USERSCOPE = UserScope.GLOBAL_ONLY.name().toLowerCase() + DEFAULT_FIELDS_SEPARATOR
        + UserScope.LOCAL_ONLY.name().toLowerCase() + DEFAULT_FIELDS_SEPARATOR
        + UserScope.LOCAL_AND_GLOBAL.name().toLowerCase();

    /**
     * Constructor.
     */
    public WikiUserClassDocumentInitializer()
    {
        super(CONFIGURATION_CLASS);
    }

    @Override
    protected void createClass(BaseClass xclass)
    {
        xclass.addStaticListField(FIELD_MEMBERSHIPTYPE, FIELDPN_MEMBERSHIPTYPE, MembershipType.values().length, false,
            FIELDL_MEMBERSHIPTYPE, FIELDDT_MEMBERSHIPTYPE);
        xclass.addStaticListField(FIELD_USERSCOPE, FIELDPN_USERSCOPE, UserScope.values().length, false,
            FIELDL_USERSCOPE, FIELDDT_USERSCOPE);
    }
}
