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

import java.io.PrintWriter;
import java.io.StringWriter;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.lang3.Strings;
import org.apache.velocity.VelocityContext;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.rendering.internal.transformation.MutableRenderingContext;
import org.xwiki.rendering.transformation.RenderingContext;
import org.xwiki.rendering.util.ErrorBlockGenerator;
import org.xwiki.velocity.VelocityManager;

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

    @Inject
    private EntityReferenceSerializer<String> defaultEntityReferenceSerializer;

    @Override
    public String evaluateVelocity(String content, String namespace, VelocityContext vcontext) throws XWikiException
    {
        StringWriter writer = new StringWriter();

        boolean renderingContextPushed = false;
        try {
            // Switch current namespace if needed
            String currentNamespace = renderingContext.getTransformationId();
            if (namespace != null && !Strings.CS.equals(namespace, currentNamespace)) {
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
            Object[] args = {namespace};
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

    @Override
    public String evaluateVelocityNoException(String content, DocumentReference namespaceDocument)
    {
        String namespace =
            namespaceDocument != null ? this.defaultEntityReferenceSerializer.serialize(namespaceDocument)
                : this.renderingContext.getTransformationId();

        try {
            return evaluateVelocity(content, namespace, this.velocityManager.getVelocityContext());
        } catch (XWikiException e) {
            return renderError(e);
        }
    }

    private String renderError(Throwable throwable)
    {
        StringBuilder builder = new StringBuilder();

        beginDiv(builder, ErrorBlockGenerator.CLASS_ATTRIBUTE_MESSAGE_VALUE);
        builder.append("Failed to execute textarea");

        beginDiv(builder, ErrorBlockGenerator.CLASS_ATTRIBUTE_DESCRIPTION_VALUE);
        StringWriter writer = new StringWriter();
        throwable.printStackTrace(new PrintWriter(writer));
        builder.append(writer.toString().replace("\n", "<br/>"));
        endDiv(builder);

        endDiv(builder);

        return builder.toString();
    }

    private void beginDiv(StringBuilder builder, String cssClass)
    {
        builder.append("<div class=\"" + cssClass + "\">");        
    }

    private void endDiv(StringBuilder builder)
    {
        builder.append("</div>");        
    }
    
}
