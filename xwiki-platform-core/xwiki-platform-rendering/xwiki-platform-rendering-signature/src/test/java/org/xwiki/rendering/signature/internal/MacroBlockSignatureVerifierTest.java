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

import java.io.OutputStream;
import java.util.List;
import java.util.Map;

import javax.inject.Named;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.crypto.pkix.CertificateProvider;
import org.xwiki.crypto.pkix.ChainingCertificateProvider;
import org.xwiki.crypto.signer.CMSSignedDataVerifier;
import org.xwiki.crypto.signer.param.CMSSignedDataVerified;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.MacroBlock;
import org.xwiki.rendering.block.MacroMarkerBlock;
import org.xwiki.rendering.block.WordBlock;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.AdditionalMatchers.or;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit test for {@link MacroBlockSignatureVerifier}.
 *
 * @version $Id$
 * @since 6.1M2
 */
@ComponentTest
class MacroBlockSignatureVerifierTest
{
    private static final Block MACRO_BLOCK = new MacroBlock("macro", Map.of(), "content", false);

    private static final Block MARKER_BLOCK = new MacroMarkerBlock("macro", Map.of(), "content", List.of(), false);

    private static final byte[] DUMPED_BLOCK = "macro\0\0content\04:wiki5:space6:source\0".getBytes();

    private static final CertificateProvider CERT_PROVIDER = new ChainingCertificateProvider();

    private static final byte[] BLOCK_SIGNATURE = "Signature".getBytes();

    private static final CMSSignedDataVerified VERIFIED = mock(CMSSignedDataVerified.class);

    @InjectMockComponents
    private MacroBlockSignatureVerifier verifier;

    @MockComponent
    @Named("macro")
    private BlockDumper dumper;

    @MockComponent
    private CMSSignedDataVerifier cmsVerifier;

    @BeforeEach
    void setUp() throws Exception
    {
        doAnswer(invocation -> {
            OutputStream out = invocation.getArgument(0);
            out.write(DUMPED_BLOCK);
            return null;
        }).when(this.dumper).dump(any(OutputStream.class), or(eq(MACRO_BLOCK), eq(MARKER_BLOCK)));
        when(this.dumper.dump(or(eq(MACRO_BLOCK), eq(MARKER_BLOCK)))).thenReturn(DUMPED_BLOCK);
        when(this.cmsVerifier.verify(BLOCK_SIGNATURE, DUMPED_BLOCK, CERT_PROVIDER)).thenReturn(VERIFIED);
    }

    @Test
    void macroBlockSignatureVerification() throws Exception
    {
        assertSame(VERIFIED, this.verifier.verify(BLOCK_SIGNATURE, MACRO_BLOCK, CERT_PROVIDER));
    }

    @Test
    void macroMarkerBlockSignatureVerification() throws Exception
    {
        assertSame(VERIFIED, this.verifier.verify(BLOCK_SIGNATURE, MARKER_BLOCK, CERT_PROVIDER));
    }

    @Test
    void incompatibleBlockVerification()
    {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> this.verifier.verify(BLOCK_SIGNATURE, new WordBlock("macro"), CERT_PROVIDER));
        assertEquals("Unsupported block [org.xwiki.rendering.block.WordBlock].", exception.getMessage());
    }
}
