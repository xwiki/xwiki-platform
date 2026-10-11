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
package org.xwiki.model.reference.internal;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.util.List;

import javax.inject.Named;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.crypto.BinaryStringEncoder;
import org.xwiki.crypto.Digest;
import org.xwiki.crypto.DigestFactory;
import org.xwiki.model.reference.BlockReference;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.signature.internal.BlockDumper;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit test for {@link SignedMacroBlockReferenceResolver}.
 *
 * @version $Id$
 * @since 6.1M2
 */
@ComponentTest
class SignedMacroBlockReferenceResolverTest
{
    private static final byte[] DIGEST_BLOCK = "digest".getBytes();

    private static final Block BLOCK = new XDOM(List.of());

    private static final String ENCODED = "abcd";

    @InjectMockComponents
    private SignedMacroBlockReferenceResolver resolver;

    @MockComponent
    @Named("SHA-1")
    private DigestFactory digestFactory;

    @MockComponent
    @Named("Base64")
    private BinaryStringEncoder encoder;

    @MockComponent
    @Named("macro")
    private BlockDumper dumper;

    private OutputStream stream;

    @BeforeEach
    void setUp() throws Exception
    {
        Digest digest = mock(Digest.class);
        this.stream = new ByteArrayOutputStream();

        when(digest.getOutputStream()).thenReturn(this.stream);
        when(digest.digest()).thenReturn(DIGEST_BLOCK);
        when(this.digestFactory.getInstance()).thenReturn(digest);
        when(this.encoder.encode(DIGEST_BLOCK)).thenReturn(ENCODED);
    }

    @Test
    void resolveWithoutParent() throws Exception
    {
        assertEquals(new BlockReference(ENCODED), this.resolver.resolve(BLOCK));
        verify(this.dumper).dump(this.stream, BLOCK);
    }

    @Test
    void resolveWithParent() throws Exception
    {
        DocumentReference parent = new DocumentReference("wiki", "space", "name");

        assertEquals(new BlockReference(ENCODED, parent), this.resolver.resolve(BLOCK, parent));
        verify(this.dumper).dump(this.stream, BLOCK);
    }

    @Test
    void resolveWithNullParent() throws Exception
    {
        assertEquals(new BlockReference(ENCODED), this.resolver.resolve(BLOCK, new Object[] { null }));
        verify(this.dumper).dump(this.stream, BLOCK);
    }
}
