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

import java.security.GeneralSecurityException;

import org.xwiki.component.annotation.Role;

/**
 * Service allowing components to sign text, determine the validity and signer of already signed text,
 * create keys, and register new certificates.
 * 
 * @version $Id$
 * @since 2.5M1
 */
@Role
public interface X509CryptoService
{
    /**
     * Creates an array of Base64 encoded DER formatted X509Certificates containing:
     * 1. A certificate from the given <a href="http://en.wikipedia.org/wiki/Spkac">SPKAC</a>
     * 2. A certificate authority certificate which will validate the first certificate in the array.
     *
     * Safari, Firefox, Opera, return through the <keygen> element an SPKAC request
     * (see the specification in html5)
     *
     * @param spkacSerialization a <a href="http://en.wikipedia.org/wiki/Spkac">SPKAC</a> Certificate Signing Request
     * @param daysOfValidity number of days before the certificate should become invalid.
     * @return an array of 2 X509Certificates in Base64 encoded DER format.
     * @throws GeneralSecurityException if something goes wrong while creating the certificate.
     */
    XWikiX509Certificate[] certsFromSpkac(final String spkacSerialization, final int daysOfValidity)
        throws GeneralSecurityException;

    /**
     * Creates an XWikiX509Certificate and matching private key.
     * This certificate will be self signed since it is expected to be used only on the server.
     * This certificate will also have it's non-repudiation bit cleared because it is expected to reside on the server
     * where it is more vulnerable than a client side certificate.
     *
     * @param daysOfValidity number of days before the certificate should become invalid.
     * @param password the password to set on the resulting XWikiX509KeyPair.
     * @return object containing certificate and private key.
     * @throws GeneralSecurityException if something goes wrong while creating the certificate.
     */
    XWikiX509KeyPair newCertAndPrivateKey(final int daysOfValidity, final String password) 
        throws GeneralSecurityException;

    /**
     * Produce a pkcs#7 signature for the given text.
     * Text will be signed with the key belonging to the author of the code which calls this.
     *
     * @param textToSign the text which the user wishes to sign.
     * @param toSignWith the certificate and matching private key to sign the text with.
     * @param password to access the private key in the key pair.
     * @return a signature which can be used to validate the signed text.
     * @throws GeneralSecurityException if anything goes wrong during signing.
     */
    String signText(final String textToSign, final XWikiX509KeyPair toSignWith, final String password)
        throws GeneralSecurityException;

    /**
     * Verify a pkcs#7 signature and return the certificate of the user who signed it.
     *
     * @param signedText the text which has been signed.
     * @param base64Signature the signature on the text in Base64 encoded DER format.
     * @return the certificate used to sign the text or null if it's invalid.
     * @throws GeneralSecurityException if anything goes wrong.
     */
    XWikiX509Certificate verifyText(final String signedText, final String base64Signature)
        throws GeneralSecurityException;

    /**
     * Deserialize an X509 certificate from a PEM formatted string.
     * @param pemFormatCert a String created by {@link org.xwiki.crypto.x509.XWikiX509Certificate#toPEMString()}
     *                      or from OpenSSL or any other standards compliant X509 certificate generator in PEM format.
     * @return an {@link org.xwiki.crypto.x509.XWikiX509Certificate} which extends 
     *         {@link java.security.cert.X509Certificate} and can be used by methods in this class as well as with
     *          third party encryption tools.
     * @throws GeneralSecurityException If there isn't a valid {@link XWikiX509Certificate#CERT_BEGIN} or
     *                                  {@link XWikiX509Certificate#CERT_END} tag, or if there is an exception parsing
     *                                  the content inbetween.
     */
    XWikiX509Certificate certFromPEM(final String pemFormatCert)
        throws GeneralSecurityException;

    /**
     * Deserialize an {@link XWikiX509KeyPair} from a base64 encoded String.
     *
     * @param keyPairAsBase64 a String created by calling 
     *                        {@link org.xwiki.crypto.x509.XWikiX509KeyPair#serializeAsBase64()}
     * @return a new {@link org.xwiki.crypto.x509.XWikiX509KeyPair}
     * @throws GeneralSecurityException if the data has become corrupted or
     *                                  if something fails during the deserialization process.
     */
    XWikiX509KeyPair keyPairFromBase64(final String keyPairAsBase64)
        throws GeneralSecurityException;
}
