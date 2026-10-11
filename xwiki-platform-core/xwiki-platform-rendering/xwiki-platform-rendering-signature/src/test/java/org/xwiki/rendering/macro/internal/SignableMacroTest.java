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
import java.util.List;
import java.util.Map;

import javax.inject.Named;
import javax.inject.Singleton;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.component.annotation.Component;
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
import org.xwiki.rendering.signature.BlockSignatureGenerator;
import org.xwiki.rendering.signature.BlockSignatureVerifier;
import org.xwiki.rendering.transformation.MacroTransformationContext;
import org.xwiki.test.annotation.BeforeComponent;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test implementation of signable macro based on {@link org.xwiki.rendering.macro.AbstractSignableMacro}
 *
 * @version $Id$
 * @since 6.1M2
 */
@ComponentTest
class SignableMacroTest
{
    private static final BlockReference BLOCK_REFERENCE = new BlockReference("blockName");

    private static final Block BLOCK = new MacroBlock("testMacro", Map.of(), true);

    private static final byte[] SIGNATURE = "Signature".getBytes();

    private static final CMSSignedDataGeneratorParameters PARAMETERS = new CMSSignedDataGeneratorParameters();

    private static final CMSSignedDataVerified VERIFIED = new CMSSignedDataVerified()
    {
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

    @InjectMockComponents
    private TestSignable macro;

    @MockComponent
    private BeanManager beanManager;

    @MockComponent
    private SignatureStore store;

    @MockComponent
    @Named("macro")
    private BlockSignatureGenerator signer;

    @MockComponent
    @Named("macro")
    private BlockSignatureVerifier verifier;

    @MockComponent
    @Named("currentsignedmacro")
    private BlockReferenceResolver<Block> blockReferenceResolver;

    @BeforeComponent
    void beforeComponent()
    {
        when(this.beanManager.getBeanDescriptor(Object.class)).thenReturn(new DefaultBeanDescriptor(Object.class));
    }

    @BeforeEach
    void setUp() throws Exception
    {
        when(this.store.retrieve(BLOCK_REFERENCE)).thenReturn(SIGNATURE);
        when(this.signer.generate(BLOCK, PARAMETERS)).thenReturn(SIGNATURE);
        when(this.verifier.verify(SIGNATURE, BLOCK, null)).thenReturn(VERIFIED);
        when(this.blockReferenceResolver.resolve(BLOCK)).thenReturn(BLOCK_REFERENCE);
    }

    @Test
    void macroSigning() throws Exception
    {
        this.macro.sign(BLOCK, PARAMETERS);
        verify(this.store).store(BLOCK_REFERENCE, SIGNATURE);
    }

    @Test
    void macroVerifying() throws Exception
    {
        assertSame(VERIFIED, this.macro.verify(BLOCK, null));
    }
}
