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
package org.xwiki.crypto.internal.digest.factory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.junit.BeforeClass;
import org.junit.Test;
import org.xwiki.crypto.Digest;
import org.xwiki.crypto.DigestFactory;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;

public abstract class AbstractDigestFactoryTest extends AbstractDigestFactoryTestConstants
{
    private static final byte[] BYTES = TEXT.getBytes();

    protected DigestFactory factory;

    protected static Digest digest;

    protected String digestAlgo;
    protected AlgorithmIdentifier digestAlgId;
    protected int digestSize;
    protected byte[] digestResult;

    @BeforeClass
    public static void cleanUpCaches() {
        digest = null;
    }

    Digest getDigestInstance()
    {
        if (digest == null) {
            digest = factory.getInstance();
        }
        return digest;
    }

    @Test
    public void testDigestFactoryProperties() throws Exception
    {
        assertThat(factory.getDigestAlgorithmName(), equalTo(digestAlgo));
        assertThat(factory.getDigestSize(), equalTo(digestSize));
    }

    @Test
    public void testDigestProperties() throws Exception
    {
        assertThat(getDigestInstance().getAlgorithmName(), equalTo(digestAlgo));
        assertThat(getDigestInstance().getDigestSize(), equalTo(digestSize));
        assertThat(getDigestInstance().getEncoded(), equalTo(digestAlgId.getEncoded()));
    }

    @Test
    public void testDigestOneShot() throws Exception
    {
        assertThat(getDigestInstance().digest(BYTES), equalTo(digestResult));
        assertThat(getDigestInstance().digest(BYTES, 10, 20), not(equalTo(digestResult)));
    }

    @Test
    public void testDigestMultiplePart() throws Exception
    {
        Digest dig = getDigestInstance();
        byte[] b = BYTES;

        dig.update(b);
        assertThat(dig.digest(), equalTo(digestResult));

        dig.update(b, 0, 13);
        dig.update(b, 13, 23);
        dig.update(b, 36, b.length - 36);
        assertThat(dig.digest(), equalTo(digestResult));
    }

    @Test
    public void testDigestOutputStream() throws Exception
    {
        OutputStream os = getDigestInstance().getOutputStream();
        os.write(BYTES);
        assertThat(getDigestInstance().digest(), equalTo(digestResult));
    }

    private int readAll(InputStream is, byte[] out) throws IOException
    {
        int readLen, len = 0;
        int blen = 17;
        while( blen > 0 && (readLen = is.read(out, len, blen)) > 0 ) {
            len += readLen;
            if (len + blen > out.length) {
                blen = out.length - len;
            }
        }
        is.close();
        return len;
    }

    @Test
    public void testDigestInputStream() throws Exception
    {
        ByteArrayInputStream bais = new ByteArrayInputStream(BYTES);
        InputStream is = getDigestInstance().getInputStream(bais);
        byte[] out = new byte[BYTES.length];

        assertThat(readAll(is, out), equalTo(BYTES.length));
        assertThat(out, equalTo(BYTES));
        assertThat(getDigestInstance().digest(), equalTo(digestResult));
    }
}
