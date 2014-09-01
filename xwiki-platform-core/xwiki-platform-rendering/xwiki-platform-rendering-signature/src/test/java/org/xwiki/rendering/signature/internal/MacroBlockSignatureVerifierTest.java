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
package org.xwiki.rendering.signature.internal;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Collections;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.xwiki.crypto.pkix.CertificateProvider;
import org.xwiki.crypto.pkix.ChainingCertificateProvider;
import org.xwiki.crypto.signer.CMSSignedDataVerifier;
import org.xwiki.crypto.signer.param.CMSSignedDataVerified;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.MacroBlock;
import org.xwiki.rendering.block.MacroMarkerBlock;
import org.xwiki.rendering.block.WordBlock;
import org.xwiki.rendering.signature.BlockSignatureVerifier;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.AdditionalMatchers.or;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit test for {@link MacroBlockSignatureVerifier}.
 *
 * @version $Id$
 * @since 6.1M2
 */
public class MacroBlockSignatureVerifierTest
{
    @Rule
    public final MockitoComponentMockingRule<BlockSignatureVerifier> mocker =
        new MockitoComponentMockingRule<BlockSignatureVerifier>(MacroBlockSignatureVerifier.class);

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private static final Block MACRO_BLOCK = new MacroBlock("macro", Collections.<String, String>emptyMap(), "content", false);
    private static final Block MARKER_BLOCK = new MacroMarkerBlock("macro", Collections.<String, String>emptyMap(), "content", Collections.<Block>emptyList(), false);
    private static final byte[] DUMPED_BLOCK = "macro\0\0content\04:wiki5:space6:source\0".getBytes();
    private static final CertificateProvider CERT_PROVIDER = new ChainingCertificateProvider();
    private static final byte[] BLOCK_SIGNATURE = "Signature".getBytes();
    private static final CMSSignedDataVerified VERIFIED = mock(CMSSignedDataVerified.class);

    private BlockDumper dumper;
    private CMSSignedDataVerifier cmsVerifier;
    private BlockSignatureVerifier verifier;

    @Before
    public void setUp() throws Exception
    {
        verifier = mocker.getComponentUnderTest();
        dumper = mocker.getInstance(BlockDumper.class, "macro");
        cmsVerifier = mocker.getInstance(CMSSignedDataVerifier.class);

        doAnswer(new Answer() {
            public Object answer(InvocationOnMock invocation) throws IOException {
                Object[] args = invocation.getArguments();
                OutputStream out = (OutputStream) args[0];
                out.write(DUMPED_BLOCK);
                return null;
            }
        }).when(dumper).dump(any(OutputStream.class),  or(eq(MACRO_BLOCK), eq(MARKER_BLOCK)));
        when(dumper.dump(or(eq(MACRO_BLOCK), eq(MARKER_BLOCK)))).thenReturn(DUMPED_BLOCK);

        when(cmsVerifier.verify(BLOCK_SIGNATURE, DUMPED_BLOCK, CERT_PROVIDER)).thenReturn(VERIFIED);
    }

    @Test
    public void testMacroBlockSignatureVerification() throws Exception
    {
        assertThat(verifier.verify(BLOCK_SIGNATURE, MACRO_BLOCK, CERT_PROVIDER), equalTo(VERIFIED));
    }

    @Test
    public void testMacroMarkerBlockSignatureVerification() throws Exception
    {
        assertThat(verifier.verify(BLOCK_SIGNATURE, MARKER_BLOCK, CERT_PROVIDER), equalTo(VERIFIED));
    }

    @Test
    public void testIncompatibleBlockVerification() throws Exception
    {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Unsupported block [org.xwiki.rendering.block.WordBlock].");

        assertThat(verifier.verify(BLOCK_SIGNATURE, new WordBlock("macro"), CERT_PROVIDER), equalTo(VERIFIED));
    }
}
