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
package org.xwiki.crypto.signer;

import org.xwiki.component.annotation.Role;
import org.xwiki.crypto.params.cipher.CipherParameters;
import org.xwiki.stability.Unstable;

/**
 * Factory creating signer for signing and verifying data integrity.
 *
 * @version $Id$
 * @since 5.4RC1
 */
@Role
@Unstable
public interface SignerFactory
{
    /**
     * Create a new initialized signer from parameters.
     *
     * @param forSigning if true the signer is initialised for signing, if false for verifying.
     * @param parameters the key and other data required by the cipher used by the signer.
     * @return an initialized signer ready to process data.
     */
    Signer getInstance(boolean forSigning, CipherParameters parameters);

    /**
     * Create a new initialized signer from encoded parameters.
     *
     * @param forSigning if true the signer is initialised for signing, if false for verifying.
     * @param parameters the key and other data required by the cipher used by the signer.
     * @param encoded the signature algorithm identifier and parameters in ASN.1 encoded form.
     * @return an initialized signer ready to process data.
     */
    Signer getInstance(boolean forSigning, CipherParameters parameters, byte[] encoded);
}
