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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.crypto.internal.asymmetric.keyfactory.BcRSAKeyFactory;
import org.xwiki.crypto.internal.digest.factory.BcSHA1DigestFactory;
import org.xwiki.crypto.internal.digest.factory.BcSHA224DigestFactory;
import org.xwiki.crypto.internal.digest.factory.BcSHA256DigestFactory;
import org.xwiki.crypto.internal.digest.factory.BcSHA384DigestFactory;
import org.xwiki.crypto.internal.digest.factory.BcSHA512DigestFactory;
import org.xwiki.crypto.internal.encoder.Base64BinaryStringEncoder;
import org.xwiki.crypto.signer.Signer;
import org.xwiki.crypto.signer.SignerFactory;
import org.xwiki.crypto.signer.params.PssSignerParameters;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

@ComponentList({Base64BinaryStringEncoder.class, BcRSAKeyFactory.class, BcSHA1DigestFactory.class,
    BcSHA224DigestFactory.class, BcSHA256DigestFactory.class, BcSHA384DigestFactory.class,
    BcSHA512DigestFactory.class})
public class BcRsaSsaPssSignerFactoryTest extends AbstractRsaSignerFactoryTest
{
    @Rule
    @SuppressWarnings("unchecked")
    public final MockitoComponentMockingRule<SignerFactory> mocker =
        new MockitoComponentMockingRule(BcRsaSsaPssSignerFactory.class);

    @Before
    public void configure() throws Exception
    {
        factory = mocker.getComponentUnderTest();
        setupTest(mocker);
    }

    @Test
    public void testDefaultSignatureVerification() throws Exception
    {
        testSignatureVerification(
            factory.getInstance(true, privateKey),
            factory.getInstance(false, publicKey)
        );
    }

    @Test
    public void testSha224SignatureVerification() throws Exception
    {
        testSignatureVerification(
            factory.getInstance(true, new PssSignerParameters(privateKey,"SHA-224", -1)),
            factory.getInstance(false, new PssSignerParameters(publicKey,"SHA-224", -1))
        );
    }

    @Test
    public void testSha256SignatureVerification() throws Exception
    {
        testSignatureVerification(
            factory.getInstance(true, new PssSignerParameters(privateKey, "SHA-256", -1)),
            factory.getInstance(false, new PssSignerParameters(publicKey, "SHA-256", -1))
        );
    }

    @Test
    public void testSha384SignatureVerification() throws Exception
    {
        testSignatureVerification(
            factory.getInstance(true, new PssSignerParameters(privateKey,"SHA-384", -1)),
            factory.getInstance(false, new PssSignerParameters(publicKey,"SHA-384", -1))
        );
    }

    @Test
    public void testSha512SignatureVerification() throws Exception
    {
        testSignatureVerification(
            factory.getInstance(true, new PssSignerParameters(privateKey,"SHA-512", -1)),
            factory.getInstance(false, new PssSignerParameters(publicKey,"SHA-512", -1))
        );
    }

    @Test
    public void testEncodedDefaultSignatureVerification() throws Exception
    {
        Signer signer = factory.getInstance(true, privateKey);
        Signer verifier = factory.getInstance(false, publicKey, signer.getEncoded());

        testSignatureVerification(signer, verifier);
    }

    @Test
    public void testEncodedSha224SignatureVerification() throws Exception
    {
        Signer signer = factory.getInstance(true, new PssSignerParameters(privateKey,"SHA-224", -1));
        Signer verifier = factory.getInstance(false, publicKey, signer.getEncoded());

        testSignatureVerification(signer, verifier);
    }

    @Test
    public void testEncodedSha256SignatureVerification() throws Exception
    {
        Signer signer = factory.getInstance(true, new PssSignerParameters(privateKey,"SHA-256", -1));
        Signer verifier = factory.getInstance(false, publicKey, signer.getEncoded());

        testSignatureVerification(signer, verifier);
    }

    @Test
    public void testEncodedSha384SignatureVerification() throws Exception
    {
        Signer signer = factory.getInstance(true, new PssSignerParameters(privateKey,"SHA-384", -1));
        Signer verifier = factory.getInstance(false, publicKey, signer.getEncoded());

        testSignatureVerification(signer, verifier);
    }

    @Test
    public void testEncodedSha512SignatureVerification() throws Exception
    {
        Signer signer = factory.getInstance(true, new PssSignerParameters(privateKey,"SHA-512", -1));
        Signer verifier = factory.getInstance(false, publicKey, signer.getEncoded());

        testSignatureVerification(signer, verifier);
    }

    private void progressiveUpdateSignature(Signer signer, byte[] bytes, int blockSize) throws Exception
    {
        signer.update(bytes, 0, blockSize + 1);
        signer.update(bytes, blockSize + 1, blockSize - 1);
        signer.update(bytes, blockSize * 2, 1);
        signer.update(bytes[(blockSize * 2) + 1]);
        signer.update(bytes, ((blockSize * 2) + 2), bytes.length - ((blockSize * 2) + 2));
    }

    @Test
    public void testProgressiveSignatureVerification() throws Exception
    {
        Signer signer = factory.getInstance(true, privateKey);
        progressiveUpdateSignature(signer, text, 17);

        byte[] signature = signer.generate();

        Signer verifier = factory.getInstance(true, publicKey);
        progressiveUpdateSignature(verifier, text, 15);

        assertTrue(verifier.verify(signature));
    }

    @Test
    public void testPartialBufferVerification() throws Exception
    {
        Signer signer = factory.getInstance(true, privateKey);

        byte[] source = new byte[text.length * 2];
        System.arraycopy(text, 0, source, text.length - 10, text.length);

        byte[] signature = signer.generate(source, text.length - 10, text.length);

        byte[] sign = new byte[signature.length * 2];
        System.arraycopy(signature, 0, sign, signature.length - 5, signature.length);

        Signer verifier = factory.getInstance(true, publicKey);
        assertTrue(
            verifier.verify(sign, signature.length - 5, signature.length, source, text.length - 10, text.length));
    }

    private int readAll(InputStream decis, byte[] out) throws IOException
    {
        int readLen = 0, len = 0;
        while( (readLen = decis.read(out, len, Math.min(15, out.length - len))) > 0 ) {
            len += readLen;
        }
        decis.close();
        return len;
    }

    @Test
    public void testStreamSignatureVerification() throws Exception
    {
        ByteArrayInputStream bais = new ByteArrayInputStream(text);

        Signer signer = factory.getInstance(true, privateKey);
        InputStream input = signer.getInputStream(bais);

        byte[] buf = new byte[text.length];
        assertThat(readAll(input, buf), equalTo(text.length));
        assertThat(buf, equalTo(text));

        byte[] signature = signer.generate();

        Signer verifier = factory.getInstance(false, publicKey);
        OutputStream output = verifier.getOutputStream();

        output.write(text);
        assertTrue(verifier.verify(signature));
    }
}
