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

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.bouncycastle.crypto.params.AsymmetricKeyParameter;
import org.xwiki.crypto.params.cipher.asymmetric.AsymmetricKeyParameters;

/**
 * Encapsulate a Bouncy Castle asymmetric key parameters.
 *
 * @version $Id$
 * @since 5.4M1
 */
public abstract class AbstractBcAsymmetricKeyParameters implements AsymmetricKeyParameters, BcAsymmetricKeyParameters
{
    protected final AsymmetricKeyParameter parameters;

    /**
     * Encapsulate a given Bouncy Castle asymmetric key parameter.
     *
     * @param parameters a BC asymmetric key parameter.
     */
    public AbstractBcAsymmetricKeyParameters(AsymmetricKeyParameter parameters)
    {
        this.parameters = parameters;
    }

    @Override
    public boolean isPrivate()
    {
        return parameters.isPrivate();
    }

    @Override
    public AsymmetricKeyParameter getParameters()
    {
        return parameters;
    }

    @Override
    public boolean equals(Object obj)
    {
        return this == obj
            || (obj instanceof BcAsymmetricKeyParameters
                && EqualsBuilder.reflectionEquals(parameters, ((BcAsymmetricKeyParameters) obj).getParameters()));
    }

    @Override
    public int hashCode()
    {
        return parameters.hashCode();
    }
}
