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
package org.xwiki.uiextension.internal.parameters;

import java.util.function.Supplier;

/**
 * A parameter value consisting of literal text around a single call that is executed directly in Java instead of
 * through the Velocity engine, producing the same output as Velocity would.
 *
 * @param raw the value as written in the UI extension object
 * @param prefix the literal text before the call
 * @param call the Java equivalent of the Velocity call
 * @param suffix the literal text after the call
 * @version $Id$
 * @since 18.10.0RC1
 */
record ShortcutParameterValue(String raw, String prefix, Supplier<String> call, String suffix)
    implements UIXParameterValue
{
    @Override
    public String evaluate(VelocityParameterSession session)
    {
        String result = this.call.get();

        // Velocity outputs the reference as written when a call returns null.
        return result != null ? this.prefix + result + this.suffix : this.raw;
    }
}
