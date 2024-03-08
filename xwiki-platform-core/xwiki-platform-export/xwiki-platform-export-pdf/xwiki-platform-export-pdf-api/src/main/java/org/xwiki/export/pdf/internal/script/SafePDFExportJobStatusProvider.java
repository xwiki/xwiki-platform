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
package org.xwiki.export.pdf.internal.script;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.export.pdf.job.PDFExportJobStatus;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.script.safe.ScriptSafeProvider;
import org.xwiki.user.CurrentUserReference;
import org.xwiki.user.UserReferenceResolver;

/**
 * Provide a safe {@link PDFExportJobStatus}.
 * 
 * @version $Id$
 * @since 14.4.2
 * @since 14.5
 */
@Component
@Singleton
public class SafePDFExportJobStatusProvider implements ScriptSafeProvider<PDFExportJobStatus>
{
    /**
     * The provider of instances safe for public scripts.
     */
    @Inject
    @SuppressWarnings("rawtypes")
    private ScriptSafeProvider defaultSafeProvider;

    @Inject
    @Named("document")
    private UserReferenceResolver<DocumentReference> userResolver;

    @Inject
    private UserReferenceResolver<CurrentUserReference> currentUserResolver;

    @Override
    @SuppressWarnings("unchecked")
    public <S> S get(PDFExportJobStatus unsafe)
    {
        return (S) new SafePDFExportJobStatus(unsafe, this.defaultSafeProvider, this.userResolver,
            this.currentUserResolver);
    }
}
