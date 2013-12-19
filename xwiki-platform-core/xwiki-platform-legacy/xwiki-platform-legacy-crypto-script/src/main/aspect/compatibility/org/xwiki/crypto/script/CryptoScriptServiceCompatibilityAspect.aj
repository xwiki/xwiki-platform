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

import org.xwiki.crypto.passwd.PasswordCryptoService;
import org.xwiki.crypto.x509.X509CryptoService;

/**
 * Script service allowing a user to sign text, determine the validity and signer of already signed text, create keys,
 * and register new certificates.
 *
 * @version $Id: 6576437c6ee1699b3d18c604a5fcbb3f8ccbaec2 $
 * @since 2.5M1
 */
public aspect CryptoScriptServiceCompatibilityAspect
{
    /** The x509/CMS service. */
    @Inject
    private X509CryptoService CryptoScriptService.x509;

    /** The password encryption and password hashing/protection service. */
    @Inject
    private PasswordCryptoService CryptoScriptService.passwd;

    /** @return the x509/CMS service. */
    @Deprecated
    public X509CryptoService CryptoScriptService.getX509()
    {
        return this.x509;
    }

    /** @return the password encryption and password hashing/protection service. */
    @Deprecated
    public PasswordCryptoService CryptoScriptService.getPasswd()
    {
        return this.passwd;
    }
}
