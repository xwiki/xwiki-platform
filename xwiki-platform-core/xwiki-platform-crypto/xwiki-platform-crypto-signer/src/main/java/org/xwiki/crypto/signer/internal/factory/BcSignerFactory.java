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
package org.xwiki.crypto.signer.internal.factory;

import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.xwiki.crypto.params.cipher.CipherParameters;
import org.xwiki.crypto.signer.Signer;

/**
 * Bouncy Castle based signer factory private interface.
 *
 * @version $Id$
 * @since 5.4
 */
public interface BcSignerFactory
{
    /**
     * Create a new initialized signer from AlgorithmIdentifier.
     *
     * @param forSigning if true the signer is initialised for signing, if false for verifying.
     * @param parameters the key and other data required by the cipher used by the signer.
     * @param algId the signature algorithm identifier and parameters.
     * @return an initialized signer ready to process data.
     */
    Signer getInstance(boolean forSigning, CipherParameters parameters, AlgorithmIdentifier algId);
}
