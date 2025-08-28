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
package com.xpn.xwiki.internal.template;

import java.io.StringReader;
import java.io.Writer;
import java.util.Objects;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.job.event.status.JobProgressManager;
import org.xwiki.model.EntityType;
import org.xwiki.rendering.internal.transformation.MutableRenderingContext;
import org.xwiki.rendering.transformation.RenderingContext;
import org.xwiki.security.authorization.DocumentAuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.template.Template;
import org.xwiki.template.TemplateContent;
import org.xwiki.velocity.VelocityEngine;
import org.xwiki.velocity.VelocityManager;
import org.xwiki.velocity.VelocityTemplate;
import org.xwiki.velocity.XWikiVelocityException;

import com.xpn.xwiki.internal.template.InternalTemplateManager.DefaultTemplateContent;

/**
 * Execute Velocity template content.
 * 
 * @version $Id$
 * @since 11.8RC1
 */
@Component(roles = VelocityTemplateEvaluator.class)
@Singleton
public class VelocityTemplateEvaluator
{
    @Inject
    private DocumentAuthorizationManager authorization;

    @Inject
    private RenderingContext renderingContext;

    @Inject
    private JobProgressManager progress;

    @Inject
    private VelocityManager velocityManager;

    /**
     * @param template the template to execute
     * @param content the template content to execute
     * @param writer the writer to writer the result to
     * @throws Exception when execution fail
     */
    public void evaluateContent(Template template, TemplateContent content, Writer writer) throws Exception
    {
        // Make sure the author of the template has script right (required to execute Velocity)
        if (content.isAuthorProvided()) {
            this.authorization.checkAccess(Right.SCRIPT, EntityType.DOCUMENT, content.getAuthorReference(),
                content.getDocumentReference());
        }

        // Use the Transformation id as the name passed to the Velocity Engine. This name is used internally
        // by Velocity as a cache index key for caching macros.
        String namespace = this.renderingContext.getTransformationId();

        boolean renderingContextPushed = false;
        if (namespace == null) {
            namespace = template.getId() != null ? template.getId() : "unknown namespace";

            if (this.renderingContext instanceof MutableRenderingContext) {
                // Make the current velocity template id available
                ((MutableRenderingContext) this.renderingContext).push(this.renderingContext.getTransformation(),
                    this.renderingContext.getXDOM(), this.renderingContext.getDefaultSyntax(), namespace,
                    this.renderingContext.isRestricted(), this.renderingContext.getTargetSyntax());

                renderingContextPushed = true;
            }
        }

        this.progress.startStep(template, "template.evaluateContent.message",
            "Evaluate content of template with id [{}]", template.getId());

        try {
            VelocityTemplate velocityTemplate = getVelocityTemplate(template, content);

            VelocityEngine velocityEngine = this.velocityManager.getVelocityEngine();
            if (velocityTemplate != null) {
                velocityEngine.evaluate(this.velocityManager.getVelocityContext(), writer, namespace, velocityTemplate);
            } else {
                velocityEngine.evaluate(this.velocityManager.getVelocityContext(), writer, namespace,
                    new StringReader(content.getContent()));
            }
        } finally {
            // Get rid of temporary rendering context
            if (renderingContextPushed) {
                ((MutableRenderingContext) this.renderingContext).pop();
            }

            this.progress.endStep(template);
        }
    }

    private VelocityTemplate getVelocityTemplate(Template template, TemplateContent content)
        throws XWikiVelocityException
    {
        if (content instanceof DefaultTemplateContent) {
            DefaultTemplateContent templateContent = (DefaultTemplateContent) content;

            // Check if the content already been compiled
            if (!(templateContent.compiledContent instanceof VelocityTemplate)) {
                // Velocity is not a fan of null template name
                String templateId = Objects.toString(template.getId(), "unknown template");

                // Compile the Velocity
                templateContent.compiledContent =
                    this.velocityManager.compile(templateId, new StringReader(content.getContent()));
            }

            return (VelocityTemplate) templateContent.compiledContent;
        }

        return null;
    }
}
