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
package org.xwiki.security.authservice.internal;

import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.LocalDocumentReference;

import com.xpn.xwiki.doc.AbstractMandatoryClassInitializer;
import com.xpn.xwiki.objects.classes.BaseClass;

/**
 * Initialize auth service configuration class.
 * 
 * @version $Id$
 * @since 15.3RC1
 */
@Component
@Named(AuthServiceConfigurationClassInitializer.CLASS_REFERENCE_STRING)
@Singleton
public class AuthServiceConfigurationClassInitializer extends AbstractMandatoryClassInitializer
{
    /**
     * The reference of the class holding the configuration of the authentication.
     */
    public static final LocalDocumentReference CLASS_REFERENCE =
        new LocalDocumentReference(AuthServiceConfiguration.SPACES, "ConfigurationClass");

    /**
     * The serialized reference of the class holding the configuration of the authentication.
     */
    public static final String CLASS_REFERENCE_STRING =
        AuthServiceConfiguration.SPACES_STRING + ".ConfigurationClass";

    /**
     * The name of the property containing the identifier of the authenticator in the wiki.
     */
    public static final String FIELD_SERVICE = "authService";

    /**
     * The default constructor.
     */
    public AuthServiceConfigurationClassInitializer()
    {
        super(CLASS_REFERENCE, "AuthService Configuration Class");
    }

    @Override
    protected void createClass(BaseClass xclass)
    {
        xclass.addTextField(FIELD_SERVICE, "Service", 30);
    }
}
