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
package org.xwiki.crypto.pkix;

import java.io.IOException;
import java.security.GeneralSecurityException;

import org.xwiki.crypto.params.cipher.asymmetric.PublicKeyParameters;
import org.xwiki.crypto.pkix.params.CertificateParameters;
import org.xwiki.crypto.pkix.params.CertifiedPublicKey;
import org.xwiki.crypto.pkix.params.PrincipalIndentifier;
import org.xwiki.stability.Unstable;

/**
 * Generator of certified public key.
 *
 * @version $Id$
 * @since 5.4
 */
@Unstable
public interface CertificateGenerator
{
    /**
     * Generate a new certificate.
     *
     * @param subjectName the identifier of the public key owner.
     * @param subject the public key to certify.
     * @param parameters the subject parameters of the certificate (ie: subjectAltName extension)
     * @return a new certified public key.
     * @throws IOException on encoding error.
     * @throws GeneralSecurityException if the signing algorithm is unable to proceed properly.
     */
    CertifiedPublicKey generate(PrincipalIndentifier subjectName, PublicKeyParameters subject,
        CertificateParameters parameters) throws IOException, GeneralSecurityException;
}
