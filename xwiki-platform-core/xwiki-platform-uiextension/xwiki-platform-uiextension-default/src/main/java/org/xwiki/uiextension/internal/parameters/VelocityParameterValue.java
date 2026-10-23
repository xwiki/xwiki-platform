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

import java.io.StringReader;
import java.io.StringWriter;

import org.xwiki.velocity.VelocityManager;
import org.xwiki.velocity.VelocityTemplate;
import org.xwiki.velocity.XWikiVelocityException;

/**
 * A parameter value evaluated with the Velocity engine. The value is compiled on its first evaluation only, and the
 * compiled template (or the compilation failure) is reused afterwards.
 *
 * @version $Id$
 * @since 18.10.0RC1
 */
final class VelocityParameterValue implements UIXParameterValue
{
    private final String raw;

    private final String namespace;

    private final VelocityManager velocityManager;

    /**
     * Compiling the same value twice when two threads evaluate it concurrently for the first time is harmless, so the
     * compilation result is only published through a volatile field instead of synchronizing. The immutable result is
     * only ever replaced as a whole, never modified, so volatile is exactly what is needed to publish it safely.
     */
    @SuppressWarnings("java:S3077")
    private volatile CompilationResult compilationResult;

    private record CompilationResult(VelocityTemplate template, XWikiVelocityException error)
    {
    }

    /**
     * @param raw the value as written in the UI extension object
     * @param namespace the Velocity namespace (and template name) to use for the evaluation
     * @param velocityManager the Velocity manager used to compile the value
     */
    VelocityParameterValue(String raw, String namespace, VelocityManager velocityManager)
    {
        this.raw = raw;
        this.namespace = namespace;
        this.velocityManager = velocityManager;
    }

    @Override
    public String raw()
    {
        return this.raw;
    }

    @Override
    public boolean requiresVelocity()
    {
        return true;
    }

    @Override
    public String evaluate(VelocityParameterSession session) throws XWikiVelocityException
    {
        VelocityTemplate template = getTemplate();

        StringWriter writer = new StringWriter();
        session.engine().evaluate(session.newContext(), writer, this.namespace, template);

        return writer.toString();
    }

    private VelocityTemplate getTemplate() throws XWikiVelocityException
    {
        CompilationResult result = this.compilationResult;

        if (result == null) {
            try {
                result = new CompilationResult(
                    this.velocityManager.compile(this.namespace, new StringReader(this.raw)), null);
            } catch (XWikiVelocityException e) {
                result = new CompilationResult(null, e);
            } catch (Exception e) {
                // Velocity reports parse errors as runtime exceptions.
                result = new CompilationResult(null, new XWikiVelocityException(
                    String.format("Failed to compile the Velocity code with namespace [%s]", this.namespace), e));
            }

            this.compilationResult = result;
        }

        if (result.error() != null) {
            throw result.error();
        }

        return result.template();
    }
}
