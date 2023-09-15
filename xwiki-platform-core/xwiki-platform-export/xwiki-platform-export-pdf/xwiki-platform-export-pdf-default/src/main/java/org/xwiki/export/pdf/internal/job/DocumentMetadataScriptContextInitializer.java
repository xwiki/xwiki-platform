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
package org.xwiki.export.pdf.internal.job;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.script.ScriptContext;

import org.xwiki.component.annotation.Component;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.script.ScriptContextInitializer;

/**
 * Puts the metadata map in the script context to be used by the PDF template to indicate the metadata corresponding to
 * the current document. This metadata can then be displayed in the PDF header or footer.
 * 
 * @version $Id$
 * @since 14.10.17
 * @since 15.5.3
 * @since 15.8RC1
 */
@Component
@Named("org.xwiki.export.pdf.internal.job.DocumentMetadataScriptContextInitializer")
@Singleton
public class DocumentMetadataScriptContextInitializer implements ScriptContextInitializer
{
    @Inject
    private Execution execution;

    @Override
    public void initialize(ScriptContext context)
    {
        ExecutionContext executionContext = this.execution.getContext();
        if (executionContext != null
            && executionContext.hasProperty(DocumentMetadataExtractor.EXECUTION_CONTEXT_PROPERTY_METADATA)) {
            context.setAttribute("metadata",
                executionContext.getProperty(DocumentMetadataExtractor.EXECUTION_CONTEXT_PROPERTY_METADATA),
                ScriptContext.ENGINE_SCOPE);
        }
    }
}
