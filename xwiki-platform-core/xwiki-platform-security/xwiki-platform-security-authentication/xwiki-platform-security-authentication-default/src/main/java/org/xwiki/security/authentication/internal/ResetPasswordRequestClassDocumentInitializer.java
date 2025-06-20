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

package org.xwiki.security.authentication.internal;

import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.LocalDocumentReference;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.doc.AbstractMandatoryClassInitializer;
import com.xpn.xwiki.objects.classes.BaseClass;

/**
 * Initializer of the ResetPasswordRequestClass XClass.
 *
 * @version $Id$
 * @since 16.3.0RC1
 * @since 15.10.9
 */
@Component
@Singleton
@Named("XWiki.ResetPasswordRequestClass")
public class ResetPasswordRequestClassDocumentInitializer extends AbstractMandatoryClassInitializer
{
    /**
     * Reference of the xclass.
     */
    public static final LocalDocumentReference REFERENCE =
        new LocalDocumentReference(XWiki.SYSTEM_SPACE, "ResetPasswordRequestClass");

    /**
     * Verification field which stores the actual token.
     */
    public static final String VERIFICATION_FIELD = "verification";

    /**
     * Date field which stores the date when the request was made.
     */
    public static final String REQUEST_DATE_FIELD = "requestDate";

    /**
     * Default constructor.
     */
    public ResetPasswordRequestClassDocumentInitializer()
    {
        super(REFERENCE);
    }

    @Override
    protected void createClass(BaseClass xclass)
    {
        xclass.addPasswordField(VERIFICATION_FIELD, "Request verification string", 30);
        xclass.addDateField(REQUEST_DATE_FIELD, "Date when the request was performed");
    }
}
