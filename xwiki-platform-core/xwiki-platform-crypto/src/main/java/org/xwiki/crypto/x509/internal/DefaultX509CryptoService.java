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

import java.security.GeneralSecurityException;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;

import org.xwiki.crypto.internal.UserDocumentUtils;
import org.xwiki.crypto.passwd.PasswordCryptoService;
import org.xwiki.crypto.x509.X509CryptoService;
import org.xwiki.crypto.x509.XWikiX509Certificate;
import org.xwiki.crypto.x509.XWikiX509KeyPair;

/**
 * Service allowing a user to sign text, determine the validity and signer of already signed text, and create keys.
 * 
 * @version $Id$
 * @since 2.5M1
 */
@Component
@Singleton
public class DefaultX509CryptoService implements X509CryptoService
{
    /** Used for dealing with non cryptographic stuff like getting user document names and URLs. */
    @Inject
    private UserDocumentUtils userDocUtils;

    /** For encrypting the private key when a key pair is generated on the server side. */
    @Inject
    private PasswordCryptoService passwordCryptoService;

    /** Handles the generation of keys. */
    private final X509KeyService keyService = new X509KeyService();

    /** For signing and verifying signatures on text. */
    private final X509SignatureService signatureService = new X509SignatureService();

    @Override
    public XWikiX509Certificate[] certsFromSpkac(final String spkacSerialization, final int daysOfValidity)
        throws GeneralSecurityException
    {
        final String userName = userDocUtils.getCurrentUser();
        final String webID = userDocUtils.getUserDocURL(userName);
        return this.keyService.certsFromSpkac(spkacSerialization, daysOfValidity, webID, userName);
    }

    @Override
    public XWikiX509KeyPair newCertAndPrivateKey(final int daysOfValidity, final String password)
        throws GeneralSecurityException
    {
        final String userName = userDocUtils.getCurrentUser();
        final String webID = userDocUtils.getUserDocURL(userName);
        return this.keyService.newCertAndPrivateKey(daysOfValidity, webID, userName, password, passwordCryptoService);
    }

    @Override
    public String signText(final String textToSign, final XWikiX509KeyPair toSignWith, final String password)
        throws GeneralSecurityException
    {
        return this.signatureService.signText(textToSign, toSignWith, password);
    }

    @Override
    public XWikiX509Certificate verifyText(final String signedText, final String base64Signature)
        throws GeneralSecurityException
    {
        return this.signatureService.verifyText(signedText, base64Signature);
    }

    @Override
    public XWikiX509Certificate certFromPEM(final String pemFormatCert)
        throws GeneralSecurityException
    {
        return XWikiX509Certificate.fromPEMString(pemFormatCert);
    }

    @Override
    public XWikiX509KeyPair keyPairFromBase64(final String keyPairAsBase64)
        throws GeneralSecurityException
    {
        try {
            return DefaultXWikiX509KeyPair.fromBase64String(keyPairAsBase64);
        } catch (Exception e) {
            throw new GeneralSecurityException("Failed to deserialize key pair", e);
        }
    }
}
