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
package org.xwiki.crypto.signer.internal.factory;

import javax.inject.Named;

import org.xwiki.component.annotation.Component;
import org.xwiki.crypto.signer.SignerFactory;

/**
 * Abstract base class for signer factory.
 *
 * @version $Id$
 * @since 5.4RC1
 */
public abstract class AbstractSignerFactory implements SignerFactory
{
    protected String getSignerAlgorithmName()
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
}
