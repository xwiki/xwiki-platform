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
package org.xwiki.crypto.pkix.params.x509certificate;

import java.math.BigInteger;
import java.util.Date;

import org.xwiki.crypto.pkix.params.CertifiedPublicKey;
import org.xwiki.crypto.pkix.params.x509certificate.extension.X509Extensions;
import org.xwiki.stability.Unstable;

/**
 * Certified binding of a principal to a public key using an X.509 Certificate.
 *
 * @version $Id$
 * @since 5.4
 */
@Unstable
public interface X509CertifiedPublicKey extends CertifiedPublicKey
{
    /**
     * @return the date after which this certificate is not valid.
     */
    Date getNotAfter();

    /**
     * @return the date before which this certificate is not valid.
     */
    Date getNotBefore();

    /**
     * @return the X.509 version of the certificate (1, 2 or 3).
     */
    int getVersionNumber();

    /**
     * @return the serial number of the certificate.
     */
    BigInteger getSerialNumber();

    /**
     * Check that the certificate is valid on the given date.
     *
     * @param date the date to be checked.
     * @return true if the certificate is valid for the given date.
     */
    boolean isValidOn(Date date);

    /**
     * @return X.509 extension.
     */
    X509Extensions getExtensions();
}
