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

package org.xwiki.crypto.cipher.internal.symmetric.factory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.xwiki.crypto.cipher.Cipher;
import org.xwiki.crypto.cipher.CipherFactory;
import org.xwiki.crypto.params.cipher.asymmetric.AsymmetricCipherParameters;
import org.xwiki.crypto.params.cipher.symmetric.SymmetricCipherParameters;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

/**
 * Abstract base class for cipher tests.
 *
 * @version $Id$
 */
public abstract class AbstractSymmetricCipherFactoryTest
{
    /** Length = 272 byte = 17 * 16 */
    private static final String TEXT = "Congress shall make no law respecting an establishment of religion, or "
        + "prohibiting the free exercise thereof; or abridging the freedom of speech, "
        + "or of the press; or the right of the people peaceably to assemble, and to "
        + "petition the Government for a redress of grievances.";

    protected static final byte[] BYTES = TEXT.getBytes();

    /** Length = 113 byte = 7 * 16 + 1 */
    private static final String ANOTHER_TEXT = "The length of this text is 113 byte. This is 1 byte more "
        + "than a multiple of block size for 128 bit block ciphers.";

    protected static final byte[] ANOTHER_BYTES = ANOTHER_TEXT.getBytes();

    /** A poor 8bits key. */
    protected static final byte[] KEY8 = {
        0x58, 0x57, 0x69, 0x6b, 0x69, 0x20, 0x69, 0x73 };

    /** A poor 16bits key. */
    protected static final byte[] KEY16 = {
        0x58, 0x57, 0x69, 0x6b, 0x69, 0x20, 0x69, 0x73,
        0x20, 0x74, 0x68, 0x65, 0x20, 0x6b, 0x65, 0x79 };

    /** A poor 32bits key. */
    protected static final byte[] KEY32 = {
        0x58, 0x57, 0x69, 0x6b, 0x69, 0x20, 0x69, 0x73,
        0x20, 0x74, 0x68, 0x65, 0x20, 0x6b, 0x65, 0x79,
        0x79, 0x65, 0x6b, 0x20, 0x65, 0x68, 0x74, 0x20,
        0x73, 0x69, 0x20, 0x69, 0x6b, 0x69, 0x57, 0x58 };

    /** A poor 8bits IV. */
    protected static final byte[] IV8 = { 0x12, 0x34, 0x56, 0x78, 0x78, 0x56, 0x34, 0x12 };

    /** A poor 16bits IV. */
    protected static final byte[] IV16 = {
        0x12, 0x34, 0x56, 0x78, 0x78, 0x56, 0x34, 0x12,
        0x12, 0x34, 0x56, 0x78, 0x78, 0x56, 0x34, 0x12 };

    protected CipherFactory factory;

    protected String CIPHER_ALGO;
    protected int BLOCK_SIZE;
    protected int KEY_SIZE;
    protected int[] SUPPORTED_KEY_SIZE;

    protected int BYTES_ENCRYPTED_SIZE;
    protected int ANOTHER_BYTES_ENCRYPTED_SIZE;

    @Test
    public void testGetCipherFactoryProperties() throws Exception
    {
        assertThat(factory.getCipherAlgorithmName(),equalTo(CIPHER_ALGO));
        assertThat(factory.getIVSize(),equalTo(BLOCK_SIZE));
        assertThat(factory.getKeySize(),equalTo(KEY_SIZE));
        assertThat(factory.getSupportedKeySizes(),equalTo(SUPPORTED_KEY_SIZE));
    }

    @Test
    public void testGetCipherProperties() throws Exception
    {
        Cipher cipher = getCipher(true);

        assertThat(cipher.getAlgorithmName(),equalTo(CIPHER_ALGO));
        assertThat(cipher.getOutputBlockSize(),equalTo(BLOCK_SIZE));
        assertThat(cipher.isForEncryption(),is(true));

        cipher = getCipher(false);
        assertThat(cipher.getAlgorithmName(),equalTo(CIPHER_ALGO));
        assertThat(cipher.getOutputBlockSize(),equalTo(BLOCK_SIZE));
        assertThat(cipher.isForEncryption(),is(false));
    }

    private static Cipher encryptCipher;
    private static Cipher decryptCipher;

    protected static byte[] encrypted;
    protected static byte[] anotherEncrypted;

    @BeforeClass
    public static void cleanUpCaches() {
        encryptCipher = null;
        decryptCipher = null;
        encrypted = null;
        anotherEncrypted = null;
    }

    /**
     * Cache the cipher to avoid recreating it between test uselessly since cypher setup may be long.
     * @return a cipher for encryption or decryption using KEY32 and IV16.
     */
    protected Cipher getCipher(boolean forEncryption)
    {
        return getCipher(forEncryption, false);
    }

    abstract Cipher getCipherInstance(boolean forEncryption);

    private Cipher getCipher(boolean forEncryption, boolean reset)
    {
        Cipher cipher = (forEncryption) ? encryptCipher : decryptCipher;

        if (reset || cipher == null) {
            cipher = getCipherInstance(forEncryption);
            if (forEncryption) {
                encryptCipher = cipher;
            } else {
                decryptCipher = cipher;
            }
        }
        return cipher;
    }

    /**
     * Cache the encrypted results for BYTES, for faster comparison, or use initialized constant.
     * @return the encrypted bytes of BYTES.
     */
    protected byte[] getEncrypted() throws Exception
    {
        if (encrypted == null) {
            encrypted = getCipher(true).doFinal(BYTES);
        }
        return encrypted;
    }

    /**
     * Cache the encrypted results for ANOTHER_BYTES, for faster comparison, or use initialized constant.
     * @return the encrypted bytes of ANOTHER_BYTES.
     */
    protected byte[] getAnotherEncrypted() throws Exception
    {
        if (anotherEncrypted == null) {
            anotherEncrypted = getCipher(true).doFinal(ANOTHER_BYTES);
        }
        return anotherEncrypted;
    }

    @Test
    public void testCipherOneShotEncryption() throws Exception
    {
        Cipher cipher = getCipher(true);

        assertThat(getEncrypted().length,equalTo(BYTES_ENCRYPTED_SIZE));
        assertThat(cipher.doFinal(BYTES),equalTo(getEncrypted()));

        assertThat(getAnotherEncrypted().length,equalTo(ANOTHER_BYTES_ENCRYPTED_SIZE));
        assertThat(cipher.doFinal(ANOTHER_BYTES),equalTo(getAnotherEncrypted()));
    }

    @Test
    public void testCipherOneShotDecryption() throws Exception
    {
        Cipher cipher = getCipher(false);

        byte[] result = cipher.doFinal(getEncrypted());
        assertThat(result.length, equalTo(BYTES.length));
        assertThat(result,equalTo(BYTES));

        result = cipher.doFinal(getAnotherEncrypted());
        assertThat(result.length, equalTo(ANOTHER_BYTES.length));
        assertThat(result, equalTo(ANOTHER_BYTES));
    }

    private byte[] getProgressive(boolean forEncryption, byte[] bytes, int size) throws Exception
    {
        Cipher cipher = getCipher(forEncryption);
        byte[] result = new byte[size];
        byte[] tmp;
        int len = 0;

        tmp = cipher.update(bytes, 0, BLOCK_SIZE + 1);
        assertThat(tmp, not(nullValue()));
        System.arraycopy(tmp, 0, result, 0, len = tmp.length);

        assertThat(cipher.update(bytes, BLOCK_SIZE + 1, BLOCK_SIZE - 1), nullValue());

        tmp = cipher.update(bytes, BLOCK_SIZE * 2, 1);
        assertThat(tmp, not(nullValue()));
        System.arraycopy(tmp, 0, result, len, tmp.length);
        len += tmp.length;

        tmp = cipher.update(bytes, ((BLOCK_SIZE * 2) + 1), bytes.length - ((BLOCK_SIZE * 2) + 1));
        assertThat(tmp, not(nullValue()));
        System.arraycopy(tmp, 0, result, len, tmp.length);
        len += tmp.length;

        tmp = cipher.doFinal();
        if (forEncryption || tmp != null) {
            assertThat(tmp, not(nullValue()));
            System.arraycopy(tmp, 0, result, len, tmp.length);
            len += tmp.length;
        }

        return result;
    }

    @Test
    public void testCipherProgressiveEncryption() throws Exception
    {
        assertThat(getProgressive(true, BYTES, BYTES_ENCRYPTED_SIZE), equalTo(getEncrypted()));
        assertThat(getProgressive(true, ANOTHER_BYTES, ANOTHER_BYTES_ENCRYPTED_SIZE), equalTo(getAnotherEncrypted()));
    }

    @Test
    public void testCipherProgressiveDecryption() throws Exception
    {
        assertThat(getProgressive(false, getEncrypted(), BYTES.length), equalTo(BYTES));
        assertThat(getProgressive(false, getAnotherEncrypted(), ANOTHER_BYTES.length), equalTo(ANOTHER_BYTES));
    }

    @Test
    public void testCipherOutputStreamEncryption() throws Exception
    {
        ByteArrayOutputStream baos = new ByteArrayOutputStream(BYTES_ENCRYPTED_SIZE);
        OutputStream encos = getCipher(true).getOutputStream(baos);
        encos.write(BYTES);
        encos.close();

        assertThat(baos.toByteArray(), equalTo(getEncrypted()));

        baos = new ByteArrayOutputStream(ANOTHER_BYTES_ENCRYPTED_SIZE);
        encos = getCipher(true).getOutputStream(baos);
        encos.write(ANOTHER_BYTES);
        encos.close();

        assertThat(baos.toByteArray(), equalTo(getAnotherEncrypted()));
    }

    @Test
    public void testCipherOutputStreamDecryption() throws Exception
    {
        ByteArrayOutputStream baos = new ByteArrayOutputStream(BYTES.length);
        OutputStream encos = getCipher(false).getOutputStream(baos);
        encos.write(getEncrypted());
        encos.close();

        assertThat(baos.toByteArray(), equalTo(BYTES));

        baos = new ByteArrayOutputStream(ANOTHER_BYTES.length);
        encos = getCipher(false).getOutputStream(baos);
        encos.write(getAnotherEncrypted());
        encos.close();

        assertThat(baos.toByteArray(), equalTo(ANOTHER_BYTES));
    }

    private int readAll(InputStream decis, byte[] out) throws IOException
    {
        int readLen = 0, len = 0;
        while( (readLen = decis.read(out, len, BLOCK_SIZE + 1)) > 0 ) {
            len += readLen;
        }
        decis.close();
        return len;
    }

    @Test
    public void testCipheInputStreamEncryption() throws Exception
    {
        ByteArrayInputStream bais = new ByteArrayInputStream(BYTES);
        InputStream decis = getCipher(true).getInputStream(bais);
        byte[] buf = new byte[BYTES_ENCRYPTED_SIZE];
        assertThat(readAll(decis, buf), equalTo(BYTES_ENCRYPTED_SIZE));
        assertThat(buf, equalTo(getEncrypted()));

        bais = new ByteArrayInputStream(ANOTHER_BYTES);
        decis = getCipher(true).getInputStream(bais);
        buf = new byte[ANOTHER_BYTES_ENCRYPTED_SIZE];
        assertThat(readAll(decis, buf), equalTo(ANOTHER_BYTES_ENCRYPTED_SIZE));
        assertThat(buf, equalTo(getAnotherEncrypted()));
    }

    @Test
    public void testCipheInputStreamDecryption() throws Exception
    {
        ByteArrayInputStream bais = new ByteArrayInputStream(getEncrypted());
        InputStream decis = getCipher(false).getInputStream(bais);
        byte[] buf = new byte[BYTES.length];
        assertThat(readAll(decis, buf), equalTo(BYTES.length));
        assertThat(buf, equalTo(BYTES));

        bais = new ByteArrayInputStream(getAnotherEncrypted());
        decis = getCipher(false).getInputStream(bais);
        buf = new byte[ANOTHER_BYTES.length];
        assertThat(readAll(decis, buf), equalTo(ANOTHER_BYTES.length));
        assertThat(buf, equalTo(ANOTHER_BYTES));
    }

    @Rule public ExpectedException thrown = ExpectedException.none();

    class WrongParameters implements SymmetricCipherParameters
    { }

    @Test
    public void testCipherWithWrongParameters() throws Exception
    {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Invalid parameters for cipher: " + WrongParameters.class.getName());
        factory.getInstance(true, new WrongParameters());
    }

    class AsymmetricParameters implements AsymmetricCipherParameters
    { }

    @Test
    public void testCipherWithAsymmetricParameters() throws Exception
    {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Unexpected parameters received for a symmetric cipher: "
            + AsymmetricParameters.class.getName());
        factory.getInstance(true, new AsymmetricParameters());
    }
}
