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
package org.xwiki.crypto.x509.internal;

import java.net.URI;
import java.net.URISyntaxException;
import java.security.GeneralSecurityException;
import java.security.InvalidParameterException;
import java.security.KeyPair;
import java.security.Provider;
import java.security.cert.X509Certificate;

import org.xwiki.crypto.internal.Convert;
import org.xwiki.crypto.passwd.PasswordCryptoService;
import org.xwiki.crypto.x509.XWikiX509Certificate;
import org.xwiki.crypto.x509.XWikiX509KeyPair;

/**
 * Service allowing a user to create keys and X509 certificates.
 *
 * @version $Id$
 * @since 2.5M1
 */
public class X509KeyService
{
    /** Used for the actual key making, also holds any secrets. */
    private final X509Keymaker keymaker = new X509Keymaker();

    /**  The JCA provider to use. */
    private Provider provider;

    /**
     * @return the JCA provider in use.
     */
    public Provider getProvider()
    {
        return provider;
    }

    /**
     * Set the JCA provider to use.
     * @param provider a JCA provider
     * @return this object for easy call chaining.
     */
    public X509KeyService setProvider(Provider provider)
    {
        this.provider = provider;
        this.keymaker.setProvider(provider);
        return this;
    }

    /**
     * @param spkacSerialization a <a href="http://en.wikipedia.org/wiki/Spkac">SPKAC</a> Certificate Signing Request
     * @param daysOfValidity number of days before the certificate should become invalid.
     * @param webID the URL of the user's page. Used for FOAFSSL compatibility.
     * @param userName the String serialization of the user's page name.
     * @return 2 certificates, one a client cert and the other an authority cert which signed the client cert.
     * @throws GeneralSecurityException on errors
     * @see org.xwiki.crypto.x509.X509CryptoService#certsFromSpkac(String, int)
     */
    public XWikiX509Certificate[] certsFromSpkac(final String spkacSerialization,
        final int daysOfValidity,
        final String webID,
        final String userName)
        throws GeneralSecurityException
    {
        this.checkWebID(webID);

        if (spkacSerialization == null) {
            throw new InvalidParameterException("SPKAC parameter is null");
        }

        SpkacRequest spkacRequest;
        try {
            spkacRequest = new SpkacRequest(Convert.fromBase64String(spkacSerialization));

            X509Certificate[] certs = this.keymaker.makeClientAndAuthorityCertificates(
                spkacRequest.getPublicKey(provider),
                daysOfValidity,
                true,
                webID,
                userName);

            return new XWikiX509Certificate[] {
                new XWikiX509Certificate(certs[0]),
                new XWikiX509Certificate(certs[1])
            };
        } catch (Exception e) {
            throw new GeneralSecurityException("Failed to parse certificate request", e);
        }

    }

    /**
     * Create a fresh self-signed key pair.
     *
     * @param daysOfValidity number of days before the certificate should become invalid.
     * @param webID the URL of the user's page. Used for FOAFSSL compatibility.
     * @param userName the String serialization of the user's page name.
     * @param password the password to set on the resulting XWikiX509KeyPair.
     * @param passwordCryptoService the service to use for encrypting the private key so this object can safely be
     *                              serialized without allowing the private key to be read from the database.
     * @return a certificate and matching private key in an XWikiX509KeyPair object.
     * @throws GeneralSecurityException on errors
     * @see org.xwiki.crypto.x509.X509CryptoService#newCertAndPrivateKey(int, String)
     */
    public XWikiX509KeyPair newCertAndPrivateKey(final int daysOfValidity,
        final String webID,
        final String userName,
        final String password,
        final PasswordCryptoService passwordCryptoService)
        throws GeneralSecurityException
    {
        this.checkWebID(webID);

        final KeyPair pair = this.keymaker.newKeyPair();

        // In this case the non-repudiation bit is cleared because the private key is made on the server 
        // which is less secure.
        final X509Certificate certificate = this.keymaker.makeClientCertificate(pair.getPublic(),
            pair,
            daysOfValidity,
            false,
            webID,
            userName);

        return new DefaultXWikiX509KeyPair(new XWikiX509Certificate(certificate),
            pair.getPrivate(),
            password,
            passwordCryptoService);
    }

    /**
     * Prove that the webID is a valid URI.
     * Without this validation, it is possible to create a certificate with an invalid URI specified and serialize the
     * certificate, the exception is only thrown upon deserialization.
     *
     * @param webID the webID to make sure it's a valid URI.
     * @throws GeneralSecurityException if the webID is not a valid URI.
     */
    private void checkWebID(final String webID) throws GeneralSecurityException
    {
        try {
            URI uri = new URI(webID);
            if (!uri.isAbsolute()) {
                throw new GeneralSecurityException("webID must be an absolute URI, got: " + webID);
            }
        } catch (URISyntaxException e) {
            throw new GeneralSecurityException("webID must be valid URI, got: " + webID, e);
        }
    }
}
