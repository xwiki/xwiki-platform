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

import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.crypto.pkix.params.x509certificate.extension.X509DirectoryName;
import org.xwiki.crypto.pkix.params.x509certificate.extension.X509GeneralName;
import org.xwiki.crypto.pkix.params.x509certificate.extension.X509IpAddress;
import org.xwiki.crypto.pkix.params.x509certificate.extension.X509Rfc822Name;
import org.xwiki.crypto.pkix.params.x509certificate.extension.X509URI;
import org.xwiki.script.service.ScriptService;

/**
 * Helper script service to create X509 names.
 *
 * @version $Id$
 * @since 8.4RC1
 */
@Component
@Named(CryptoScriptService.ROLEHINT + '.' + X509NameScriptService.ROLEHINT)
@Singleton
public class X509NameScriptService implements ScriptService
{
    /**
     * The role hint of this component.
     */
    public static final String ROLEHINT = "x509name";

    /**
     * Create a new {@link X509Rfc822Name}.
     *
     * @param email the name.
     * @return an typed {@link X509GeneralName}
     */
    public X509Rfc822Name createX509Rfc822Name(String email) {
        return new X509Rfc822Name(email);
    }

    /**
     * Create a new {@link X509DirectoryName}.
     *
     * @param dn the name.
     * @return an typed {@link X509GeneralName}
     */
    public X509DirectoryName createX509DirectoryName(String dn) {
        return new X509DirectoryName(dn);
    }

    /**
     * Create a new {@link X509IpAddress}.
     *
     * @param ip the name.
     * @return an typed {@link X509GeneralName}
     */
    public X509IpAddress createX509IpAddress(String ip) {
        return new X509IpAddress(ip);
    }

    /**
     * Create a new {@link X509URI}.
     *
     * @param uri the name.
     * @return an typed {@link X509GeneralName}
     */
    public X509URI createX509URI(String uri) {
        return new X509URI(uri);
    }
}
