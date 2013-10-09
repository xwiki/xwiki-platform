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
package org.xwiki.crypto.script;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.crypto.passwd.PasswordCryptoService;
import org.xwiki.crypto.x509.X509CryptoService;
import org.xwiki.script.service.ScriptService;

/**
 * Script service allowing a user to sign text, determine the validity and signer of already signed text, create keys,
 * and register new certificates.
 * 
 * @version $Id$
 * @since 2.5M1
 */
@Component
@Named("crypto")
@Singleton
public class CryptoScriptService implements ScriptService
{
    /** The x509/CMS service. */
    @Inject
    private X509CryptoService x509;

    /** The password encryption and password hashing/protection service. */
    @Inject
    private PasswordCryptoService passwd;

    /** @return the x509/CMS service. */
    public X509CryptoService getX509()
    {
        return this.x509;
    }

    /** @return the password encryption and password hashing/protection service. */
    public PasswordCryptoService getPasswd()
    {
        return this.passwd;
    }
}
