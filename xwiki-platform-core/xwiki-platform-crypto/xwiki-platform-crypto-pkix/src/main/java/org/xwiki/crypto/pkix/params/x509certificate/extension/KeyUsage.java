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
package org.xwiki.crypto.pkix.params.x509certificate.extension;

import org.bouncycastle.asn1.x509.Extension;
import org.xwiki.stability.Unstable;

/**
 * X.509 Certificates Key Usages.
 *
 * @version $Id$
 * @since 5.4
 */
@Unstable
public enum KeyUsage
{
    /**
     * for SSL client certificates, S/MIME signing certificates, and object-signing certificates.
     */
    digitalSignature(1 << 7),
    /**
     * for some S/MIME signing certificates and object-signing certificates.
     */
    nonRepudiation(1 << 6),
    /**
     * or SSL server certificates and S/MIME encryption certificates.
     */
    keyEncipherment(1 << 5),
    /**
     * when the subject's public key is used to encrypt user data instead of key material.
     */
    dataEncipherment(1 << 4),
    /**
     * when the subject's public key is used for key agreement.
     */
    keyAgreement(1 << 3),
    /**
     * for all CA signing certificates.
     */
    keyCertSign(1 << 2),
    /**
     * for CA signing certificates that are used to sign CRLs.
     */
    cRLSign(1 << 1),
    /**
     * if the public key is used only for enciphering data. If this bit is set, keyAgreement should also be set.
     */
    encipherOnly(1),
    /**
     * if the public key is used only for deciphering data. If this bit is set, keyAgreement should also be set.
     */
    decipherOnly(1 << 15);

    /**
     * OID of KeyUsage.
     */
    public static final String OID = Extension.keyUsage.getId();

    private int usage;

    KeyUsage(int usage)
    {
        this.usage = usage;
    }

    /**
     * @return the integer value representing this usage.
     */
    public int value()
    {
        return usage;
    }
}
