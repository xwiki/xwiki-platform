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
package org.xwiki.rendering.macro.internal;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.inject.Named;
import javax.inject.Singleton;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.util.DefaultParameterizedType;
import org.xwiki.crypto.pkix.params.CertifiedPublicKey;
import org.xwiki.crypto.signer.param.CMSSignedDataGeneratorParameters;
import org.xwiki.crypto.signer.param.CMSSignedDataVerified;
import org.xwiki.crypto.signer.param.CMSSignerVerifiedInformation;
import org.xwiki.crypto.store.SignatureStore;
import org.xwiki.model.reference.BlockReference;
import org.xwiki.model.reference.BlockReferenceResolver;
import org.xwiki.properties.BeanManager;
import org.xwiki.properties.internal.DefaultBeanDescriptor;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.MacroBlock;
import org.xwiki.rendering.macro.AbstractNoParameterSignableMacro;
import org.xwiki.rendering.macro.MacroExecutionException;
import org.xwiki.rendering.macro.SignableMacro;
import org.xwiki.rendering.signature.BlockSignatureGenerator;
import org.xwiki.rendering.signature.BlockSignatureVerifier;
import org.xwiki.rendering.transformation.MacroTransformationContext;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test implementation of signable macro based on {@link org.xwiki.rendering.macro.AbstractSignableMacro}
 *
 * @version $Id$
 * @since 6.1M2
 */
public class SignableMacroTest
{
    private static final BlockReference BLOCK_REFERENCE = new BlockReference("blockName");
    private static final Block BLOCK = new MacroBlock("testMacro", Collections.<String, String>emptyMap(), true);
    private static final byte[] SIGNATURE = "Signature".getBytes();
    private static final CMSSignedDataGeneratorParameters PARAMETERS = new CMSSignedDataGeneratorParameters();
    private static final CMSSignedDataVerified VERIFIED = new CMSSignedDataVerified() {
        @Override
        public Collection<CMSSignerVerifiedInformation> getSignatures()
        {
            return null;
        }

        @Override
        public Collection<CertifiedPublicKey> getCertificates()
        {
            return null;
        }

        @Override
        public String getContentType()
        {
            return null;
        }

        @Override
        public byte[] getContent()
        {
            return new byte[0];
        }

        @Override
        public boolean isVerified()
        {
            return false;
        }
    };

    @Component(staticRegistration = false)
    @Named("testmacro")
    @Singleton
    public static class TestSignable extends AbstractNoParameterSignableMacro
    {
        public TestSignable()
        {
            super("testmacro");
        }

        @Override
        public boolean supportsInlineMode()
        {
            return false;
        }

        @Override
        public List<Block> execute(Object o, String s, MacroTransformationContext macroTransformationContext)
            throws MacroExecutionException
        {
            return null;
        }
    }

    @Rule
    public final MockitoComponentMockingRule<SignableMacro> mocker =
        new MockitoComponentMockingRule<SignableMacro>(TestSignable.class);

    private SignableMacro macro;
    private SignatureStore store;

    @Before
    public void setUp() throws Exception
    {
        BeanManager beanManager = mocker.getInstance(BeanManager.class);
        when(beanManager.getBeanDescriptor(Object.class)).thenReturn(new DefaultBeanDescriptor(Object.class));

        store = mocker.registerMockComponent(SignatureStore.class);
        when(store.retrieve(BLOCK_REFERENCE)).thenReturn(SIGNATURE);

        BlockSignatureGenerator signer = mocker.registerMockComponent(BlockSignatureGenerator.class, "macro");
        when(signer.generate(BLOCK, PARAMETERS)).thenReturn(SIGNATURE);

        BlockSignatureVerifier verifier = mocker.registerMockComponent(BlockSignatureVerifier.class, "macro");
        when(verifier.verify(SIGNATURE, BLOCK, null)).thenReturn(VERIFIED);

        BlockReferenceResolver<Block> resolver =
            mocker.registerMockComponent(new DefaultParameterizedType(null, BlockReferenceResolver.class, Block.class),
                "currentsignedmacro");
        when(resolver.resolve(BLOCK)).thenReturn(BLOCK_REFERENCE);

        macro = mocker.getComponentUnderTest();
    }

    @Test
    public void testMacroSigning() throws Exception
    {
        macro.sign(BLOCK, PARAMETERS);
        verify(store).store(BLOCK_REFERENCE, SIGNATURE);
    }

    @Test
    public void testMacroVerifying() throws Exception
    {
        assertThat(macro.verify(BLOCK, null), equalTo(VERIFIED));
    }
}
