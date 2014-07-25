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
package org.xwiki.crypto.password.internal.kdf.factory;

import javax.inject.Named;

import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.ASN1Sequence;
import org.xwiki.component.annotation.Component;
import org.xwiki.crypto.password.KeyDerivationFunction;
import org.xwiki.crypto.password.KeyDerivationFunctionFactory;

/**
 * Abstract base class for key derivation function factory based on Bouncy Castle.
 *
 * @version $Id$
 * @since 5.4M1
 */
public abstract class AbstractBcKDFFactory implements KeyDerivationFunctionFactory
{
    @Override
    public String getKDFAlgorithmName()
    {
        String hint = null;
        Named named = this.getClass().getAnnotation(Named.class);
        if (named != null) {
            hint = named.value();
        } else {
            Component component = this.getClass().getAnnotation(Component.class);
            if (component != null && component.hints().length > 0) {
                hint = component.hints()[0];
            }
        }

        return hint;
    }

    @Override
    public KeyDerivationFunction getInstance(byte[] encoded)
    {
        return getInstance(ASN1Sequence.getInstance(encoded));
    }

    /**
     * Get a Bouncy Castle based key derivation function from ASN.1 parameters.
     *
     * The ASN.1 parameters will be parsed as a {@link org.bouncycastle.asn1.pkcs.KeyDerivationFunc}.
     *
     * @param parameters ASN.1 encodable parameters to initialize the function.
     * @return a initialized key derivation function.
     */
    public abstract KeyDerivationFunction getInstance(ASN1Encodable parameters);
}
