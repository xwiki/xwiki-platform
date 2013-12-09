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
package org.xwiki.crypto.internal.asymmetric;

import java.io.IOException;

import org.bouncycastle.crypto.params.AsymmetricKeyParameter;
import org.bouncycastle.crypto.util.SubjectPublicKeyInfoFactory;
import org.xwiki.crypto.params.cipher.asymmetric.PublicKeyParameters;

/**
 * Encapsulate a Bouncy Castle asymmetric parameter of a public key.
 *
 * @version $Id$
 * @since 5.4M1
 */
public class BcPublicKeyParameters extends AbstractBcAsymmetricKeyParameters
                                   implements PublicKeyParameters
{
    /**
     * Encapsulate a given Bouncy Castle public key parameter.
     *
     * @param parameters a BC public key parameter.
     */
    public BcPublicKeyParameters(AsymmetricKeyParameter parameters)
    {
        super(parameters);
        if (isPrivate()) {
            throw new IllegalArgumentException("Private key assigned to a public key: "
                + parameters.getClass().getName());
        }
    }

    @Override
    public byte[] getEncoded()
    {
        try {
            return SubjectPublicKeyInfoFactory.createSubjectPublicKeyInfo(parameters).getEncoded();
        } catch (IOException e) {
            return null;
        }
    }
}
