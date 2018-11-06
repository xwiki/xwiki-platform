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
package org.xwiki.job.handler.internal;

import java.io.IOException;
import java.io.OutputStream;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;

import javax.inject.Inject;

import org.apache.commons.io.IOUtils;
import org.xwiki.container.Container;
import org.xwiki.container.Response;
import org.xwiki.resource.ResourceReferenceHandlerException;
import org.xwiki.template.Template;
import org.xwiki.template.TemplateManager;

/**
 * Base class for all {@link JobResourceReferenceHandler} which are the result of a template execution.
 * 
 * @version $Id$
 */
public abstract class AbstractTemplateJobResourceReferenceHandler implements JobResourceReferenceHandler
{
    @Inject
    protected TemplateManager templates;

    @Inject
    protected Container container;

    protected boolean tryTemplates(String contentType, String... templateNames) throws ResourceReferenceHandlerException
    {
        for (String templateName : templateNames) {
            if (tryTemplate(contentType, templateName)) {
                return true;
            }
        }

        return false;
    }

    protected boolean tryTemplate(String defaultContentType, String templateName)
        throws ResourceReferenceHandlerException
    {
        Template template = this.templates.getTemplate("job/" + templateName);

        if (template == null) {
            return false;
        }

        Response response = this.container.getResponse();

        try {
            // Set default content type (can be overwritten by the template itself)
            if (defaultContentType != null) {
                response.setContentType(defaultContentType);
            }

            Writer writer = new StringWriter();
            this.templates.render(template, writer);

            sendContent(writer.toString());
        } catch (Exception e) {
            throw new ResourceReferenceHandlerException("Failed to execute template [" + templateName + "]", e);
        }

        return true;
    }

    protected void sendContent(String content) throws ResourceReferenceHandlerException
    {
        Response response = this.container.getResponse();

        try (OutputStream stream = response.getOutputStream()) {
            IOUtils.write(content, stream, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new ResourceReferenceHandlerException("Failed to send content", e);
        }
    }
}
