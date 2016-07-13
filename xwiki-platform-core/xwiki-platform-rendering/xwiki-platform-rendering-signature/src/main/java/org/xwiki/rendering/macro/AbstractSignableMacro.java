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
package org.xwiki.rendering.macro;

import java.io.IOException;
import java.security.GeneralSecurityException;

import javax.inject.Inject;

import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.util.DefaultParameterizedType;
import org.xwiki.crypto.pkix.CertificateProvider;
import org.xwiki.crypto.signer.param.CMSSignedDataGeneratorParameters;
import org.xwiki.crypto.signer.param.CMSSignedDataVerified;
import org.xwiki.crypto.store.SignatureStore;
import org.xwiki.crypto.store.SignatureStoreException;
import org.xwiki.model.reference.BlockReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.macro.descriptor.ContentDescriptor;
import org.xwiki.rendering.signature.BlockSignatureGenerator;
import org.xwiki.rendering.signature.BlockSignatureVerifier;

/**
 * Helper to implement signable Macro, supplementing the default implementation provided
 * by {@link org.xwiki.rendering.macro.AbstractMacro} to ease the support of signatures.
 *
 * @param <P> the type of the macro parameters bean
 * @version $Id$
 * @since 6.1M2
 */
public abstract class AbstractSignableMacro<P> extends AbstractMacro<P> implements SignableMacro
{
    private static final String HINT = "macro";

    /**
     * Used to lazily load of other components, avoiding normal macro that never get signed to need these components.
     */
    @Inject
    private ComponentManager componentManager;

    // Lazily loaded components.
    private SignatureStore signatureStore;
    private BlockSignatureGenerator signer;
    private BlockSignatureVerifier verifier;
    private BlockReferenceResolver<Block> blockResolver;

    /**
     * Creates a new {@link Macro} instance.
     *5005
     * @param name the name of the macro (eg "Table Of Contents" for the TOC macro)
     */
    public AbstractSignableMacro(String name)
    {
        super(name);
    }

    /**
     * Creates a new {@link Macro} instance.
     *
     * @param name the name of the macro (eg "Table Of Contents" for the TOC macro)
     * @param description a string describing this macro.
     */
    public AbstractSignableMacro(String name, String description)
    {
        super(name, description);
    }

    /**
     * Creates a new {@link Macro} instance.
     *
     * @param name the name of the macro (eg "Table Of Contents" for the TOC macro)
     * @param description a string describing this macro.
     * @param contentDescriptor {@link ContentDescriptor} for this macro.
     */
    public AbstractSignableMacro(String name, String description,
        ContentDescriptor contentDescriptor)
    {
        super(name, description, contentDescriptor);
    }

    /**
     * Creates a new {@link Macro} instance.
     *
     * @param name the name of the macro (eg "Table Of Contents" for the TOC macro)
     * @param description a string describing this macro.
     * @param parametersBeanClass class of the parameters bean of this macro.
     */
    public AbstractSignableMacro(String name, String description, Class<?> parametersBeanClass)
    {
        super(name, description, parametersBeanClass);
    }

    /**
     * Creates a new {@link Macro} instance.
     *
     * @param name the name of the macro (eg "Table Of Contents" for the TOC macro)
     * @param description string describing this macro.
     * @param contentDescriptor the {@link ContentDescriptor} describing the content of this macro.
     * @param parametersBeanClass class of the parameters bean.
     */
    public AbstractSignableMacro(String name, String description,
        ContentDescriptor contentDescriptor, Class<?> parametersBeanClass)
    {
        super(name, description, contentDescriptor, parametersBeanClass);
    }

    /**
     * @return the component manager
     * @since 2.0M1 (moved from AbstractScriptMacro)
     */
    protected ComponentManager getComponentManager()
    {
        return this.componentManager;
    }

    /**
     * @return the default signature store. Lazy get to avoid strong dependency.
     * @throws ComponentLookupException if no instance has been found.
     */
    private SignatureStore getSignatureStore() throws ComponentLookupException
    {
        if (signatureStore == null) {
            signatureStore = getComponentManager().getInstance(SignatureStore.class);
        }
        return signatureStore;
    }

    /**
     * @return the macro block signer. Lazy get to avoid strong dependency.
     * @throws ComponentLookupException if no instance has been found.
     */
    private BlockSignatureGenerator getSigner() throws ComponentLookupException
    {
        if (signer == null) {
            signer = getComponentManager().getInstance(BlockSignatureGenerator.class, HINT);
        }
        return signer;
    }

    /**
     * @return the macro block verifier. Lazy get to avoid strong dependency.
     * @throws ComponentLookupException if no instance has been found.
     */
    private BlockSignatureVerifier getVerifier() throws ComponentLookupException
    {
        if (verifier == null) {
            verifier = getComponentManager().getInstance(BlockSignatureVerifier.class, HINT);
        }
        return verifier;
    }

    /**
     * @return the current signed macro block reference resolver. Lazy get to avoid strong dependency.
     * @throws ComponentLookupException if no instance has been found.
     */
    private BlockReferenceResolver<Block> getBlockResolver() throws ComponentLookupException
    {
        if (blockResolver == null) {
            blockResolver = getComponentManager().getInstance(
                new DefaultParameterizedType(null, BlockReferenceResolver.class, Block.class),
                "currentsignedmacro");
        }
        return blockResolver;
    }

    @Override
    public void sign(Block block, CMSSignedDataGeneratorParameters parameters) throws MacroSignatureException
    {
        EntityReference blockRef = getBlockReference(block);

        try {
            getSignatureStore().store(blockRef, getSigner().generate(block, parameters));
        } catch (SignatureStoreException e) {
            throw new MacroSignatureException(
                String.format("Unable to store the signature of macro block [%s].", blockRef), e);
        } catch (GeneralSecurityException e) {
            throw new MacroSignatureException(
                String.format("Unable to compute signature of macro block [%s].", blockRef), e);
        } catch (IOException e) {
            throw new MacroSignatureException(
                String.format("Unable to encode signature of macro block [%s].", blockRef), e);
        } catch (ComponentLookupException e) {
            throw new MacroSignatureException(
                String.format("Missing components to sign macro block [%s].", blockRef), e);
        }
    }

    @Override
    public CMSSignedDataVerified verify(Block block, CertificateProvider certificateProvider)
        throws MacroSignatureException
    {
        EntityReference blockRef = getBlockReference(block);

        try {
            return getVerifier().verify(getSignatureStore().retrieve(blockRef), block, certificateProvider);
        } catch (SignatureStoreException e) {
            throw new MacroSignatureException(
                String.format("Unable to retrieve the signature of macro block [%s].", blockRef), e);
        } catch (GeneralSecurityException e) {
            throw new MacroSignatureException(
                String.format("Unable to verify signature of macro block [%s].", blockRef), e);
        } catch (IOException e) {
            throw new MacroSignatureException(
                String.format("Unable to decode signature of macro block [%s].", blockRef), e);
        } catch (ComponentLookupException e) {
            throw new MacroSignatureException(
                String.format("Missing components to verify macro block [%s].", blockRef), e);
        }
    }

    private EntityReference getBlockReference(Block block) throws MacroSignatureException
    {
        EntityReference blockRef;

        try {
            blockRef = getBlockResolver().resolve(block);
        } catch (ComponentLookupException e) {
            throw new MacroSignatureException("Missing component to resolve macro block reference.", e);
        }

        if (blockRef == null) {
            throw new MacroSignatureException("Unable to determine the block reference of the macro block.");
        }

        return blockRef;
    }
}
