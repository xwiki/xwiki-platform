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
package org.xwiki.crypto.x509;

import java.io.IOException;
import java.io.Serializable;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.GeneralSecurityException;

/**
 * Wrapper which contains a {@link PrivateKey} and the corresponding {@link XWikiX509Certificate}.
 * This class is capable of holding a chain of certificates from the user's certificate back to the root certificate.
 * 
 * @version $Id$
 * @since 2.5M1
 */
@Deprecated
public interface XWikiX509KeyPair extends Serializable
{
    /** This will be at the beginning of the output from {@link #serializeAsBase64()}. */
    String BASE64_HEADER = "-----BEGIN XWIKI CERTIFICATE AND PRIVATE KEY-----\n";

    /** This will be at the end of the output from {@link #serializeAsBase64()}. */
    String BASE64_FOOTER = "-----END XWIKI CERTIFICATE AND PRIVATE KEY-----";

    /**
     * Get the user's certificate. May throw a {@link RuntimeException} if the key pair was deserialized directly using
     * Java deserialization methods without checking that the certificate can be deserialized.
     * 
     * @return the user's certificate
     */
    XWikiX509Certificate getCertificate();

    /**
     * @return the public key
     */
    PublicKey getPublicKey();

    /**
     * Get the private key from the key pair.
     *
     * @param password the password needed to decrypt the private key.
     * @return the private key or null if the password is incorrect.
     * @throws GeneralSecurityException if the private key cannot be decrypted.
     */
    PrivateKey getPrivateKey(final String password) throws GeneralSecurityException;

    /**
     * @return certificate fingerprint
     */
    String getFingerprint();

    /**
     * @return this key pair as a byte array, the private key will remain password encrypted as it is in memory.
     * @throws IOException if something goes wrong within the serialization framework.
     */
    byte[] serialize() throws IOException;

    /**
     * @return this key pair {@link #serialize()}d and converted to a base-64 encoded String.
     * @throws IOException if something goes wrong within the serialization framework.
     */
    String serializeAsBase64() throws IOException;
}

