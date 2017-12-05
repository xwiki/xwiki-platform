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
package com.xpn.xwiki.internal.velocity;

import java.io.StringWriter;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.rendering.transformation.RenderingContext;
import org.apache.velocity.VelocityContext;
import org.xwiki.velocity.VelocityManager;
import org.xwiki.rendering.internal.transformation.MutableRenderingContext;
import org.xwiki.component.annotation.Component;

import com.xpn.xwiki.XWikiException;

/**
 * Default implementation of {@link VelocityEvaluator}.
 * 
 * @version $Id$
 * @since 9.11RC1
 */
@Component
@Singleton
public class DefaultVelocityEvaluator implements VelocityEvaluator
{
    @Inject
    private RenderingContext renderingContext;

    @Inject
    private VelocityManager velocityManager;
    
    @Override
    public String evaluateVelocity(String content, String namespace, VelocityContext vcontext) throws XWikiException
    {
        StringWriter writer = new StringWriter();

        boolean renderingContextPushed = false;
        try {
            // Switch current namespace if needed
            String currentNamespace = renderingContext.getTransformationId();
            if (namespace != null && !StringUtils.equals(namespace, currentNamespace)) {
                if (renderingContext instanceof MutableRenderingContext) {
                    // Make the current velocity template id available
                    ((MutableRenderingContext) renderingContext).push(renderingContext.getTransformation(),
                            renderingContext.getXDOM(), renderingContext.getDefaultSyntax(), namespace,
                            renderingContext.isRestricted(), renderingContext.getTargetSyntax());

                    renderingContextPushed = true;
                }
            }

            velocityManager.getVelocityEngine().evaluate(vcontext, writer, namespace, content);

            return writer.toString();
        } catch (Exception e) {
            Object[] args = { namespace };
            throw new XWikiException(XWikiException.MODULE_XWIKI_RENDERING,
                    XWikiException.ERROR_XWIKI_RENDERING_VELOCITY_EXCEPTION, "Error while parsing velocity page {0}", e,
                    args);
        } finally {
            // Get rid of temporary rendering context
            if (renderingContextPushed) {
                ((MutableRenderingContext) this.renderingContext).pop();
            }
        }
    }
}
