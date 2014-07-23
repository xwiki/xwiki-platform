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
import java.util.Collections;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.crypto.BinaryStringEncoder;
import org.xwiki.crypto.Digest;
import org.xwiki.crypto.DigestFactory;
import org.xwiki.model.reference.BlockReference;
import org.xwiki.model.reference.BlockReferenceResolver;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.signature.internal.BlockDumper;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit test for {@link SignedMacroBlockReferenceResolver}.
 *
 * @version $Id$
 * @since 6.1M2
 */
public class SignedMacroBlockReferenceResolverTest
{
    @Rule
    public final MockitoComponentMockingRule<BlockReferenceResolver<Block>> mocker =
        new MockitoComponentMockingRule<BlockReferenceResolver<Block>>(SignedMacroBlockReferenceResolver.class);

    private static final byte[] DIGEST_BLOCK = "digest".getBytes();
    private static final Block BLOCK = new XDOM(Collections.<Block>emptyList());
    private static final String ENCODED = "abcd";

    private BlockDumper dumper;
    private OutputStream stream;

    @Before
    public void setUp() throws Exception
    {
        DigestFactory digestFactory = mocker.getInstance(DigestFactory.class, "SHA-1");
        BinaryStringEncoder encoder = mocker.getInstance(BinaryStringEncoder.class, "Base64");
        dumper = mocker.getInstance(BlockDumper.class, "macro");
        Digest digest = mock(Digest.class);
        stream = new ByteArrayOutputStream();

        when(digest.getOutputStream()).thenReturn(stream);
        when(digest.digest()).thenReturn(DIGEST_BLOCK);
        when(digestFactory.getInstance()).thenReturn(digest);
        when(encoder.encode(DIGEST_BLOCK)).thenReturn(ENCODED);
    }

    @Test
    public void testResolveWithoutParent() throws Exception
    {
        assertThat(mocker.getComponentUnderTest().resolve(BLOCK), equalTo(new BlockReference(ENCODED)));
        verify(dumper).dump(stream, BLOCK);
    }

    @Test
    public void testResolveWithParent() throws Exception
    {
        DocumentReference parent = new DocumentReference("wiki","space","name");

        assertThat(mocker.getComponentUnderTest().resolve(BLOCK, parent), equalTo(new BlockReference(ENCODED, parent)));
        verify(dumper).dump(stream, BLOCK);
    }

    @Test
    public void testResolveWithNullParent() throws Exception
    {
        assertThat(mocker.getComponentUnderTest().resolve(BLOCK, new Object[] { null }), equalTo(new BlockReference(ENCODED)));
        verify(dumper).dump(stream, BLOCK);
    }
}
