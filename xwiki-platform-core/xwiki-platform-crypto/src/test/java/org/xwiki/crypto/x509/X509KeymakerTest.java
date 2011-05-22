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
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.cert.X509Certificate;

import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.DEROctetString;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.junit.Assert;
import org.junit.Test;
import org.xwiki.crypto.x509.internal.X509Keymaker;

/**
 * Test the {@link X509Keymaker} class.
 * 
 * @version $Id$
 * @since 2.5M1
 */
public class X509KeymakerTest
{
    /** The tested key maker. */
    private final X509Keymaker keyMaker = new X509Keymaker();

    @Test
    public void testGenerateCertAuthority() throws GeneralSecurityException, IOException
    {
        KeyPair kp = FastKeySupplier.KEYPAIR; //keyMaker.newKeyPair();

        /*java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        java.io.ObjectOutputStream oos = new java.io.ObjectOutputStream(baos);
        oos.writeObject(kp);
        oos.close();
        System.out.println("\n\n");
        System.out.println(org.xwiki.crypto.internal.Convert.toBase64String(baos.toByteArray()));
        System.out.println("\n\n");*/



        X509Certificate cert = keyMaker.makeCertificateAuthority(kp, 1, "http://my.web.id.com/");

        // read Basic Constraints
        DEROctetString obj = (DEROctetString) new ASN1InputStream(cert.getExtensionValue("2.5.29.19")).readObject();
        ASN1Sequence seq = (ASN1Sequence) new ASN1InputStream(obj.getOctets()).readObject();
        BasicConstraints constraints = BasicConstraints.getInstance(seq);
        Assert.assertTrue(constraints.isCA());
    }

    @Test
    public void testClientAndAuthority() throws GeneralSecurityException
    {
        KeyPair kp = FastKeySupplier.KEYPAIR; //keyMaker.newKeyPair();
        X509Certificate[] certs = keyMaker.makeClientAndAuthorityCertificates(kp.getPublic(),
                                                                              1,
                                                                              true,
                                                                              "http://my.web.id.com/",
                                                                              "xwiki:XWiki.Me");
        // verify client
        certs[0].checkValidity();
        certs[0].verify(certs[1].getPublicKey());
        // verify authority
        certs[1].checkValidity();
        certs[1].verify(certs[1].getPublicKey());
    }
}
