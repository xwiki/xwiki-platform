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
package org.xwiki.rendering.internal.macro.cache;

import javax.inject.Inject;
import javax.inject.Named;

import org.xwiki.component.annotation.Component;
import org.xwiki.job.AbstractJob;
import org.xwiki.job.DefaultJobStatus;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.listener.MetaData;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.rendering.transformation.Transformation;
import org.xwiki.rendering.transformation.TransformationContext;
import org.xwiki.rendering.transformation.TransformationException;

@Component
@Named(CacheMacroRecomputationJob.JOBTYPE)
public class CacheMacroRecomputationJob
    extends AbstractJob<CacheMacroRecomputeRequest, DefaultJobStatus<CacheMacroRecomputeRequest>>
{
    /**
     * The id of the job.
     */
    public static final String JOBTYPE = "CacheMacroRecomputation";

    @Inject
    @Named("macro")
    private Transformation macroTransformation;

    @Override
    protected void runInternal() throws Exception
    {
        // Render the macro content by executing the Macro transformation on the static XDOM
        CacheMacroRecomputeRequest request = getRequest();
        XDOM staticXDOM = request.getStaticXDOM();
        XDOM transformedXDOM;
        try {
            transformedXDOM = transformContent(staticXDOM);
        } catch (TransformationException e) {
            // We failed to apply the macro transformation on the XDOM. We remove the entry from the cache so that
            // the next time a user calls the cache macro for that content, he'll get the error displayed and can act
            // on it.
            request.getCache().remove(request.getCacheKey());
            this.logger.error("Failed to compute new cached content", e);
            return;
        }

        // Update the cache entry with the newly computed XDOM
        request.getCache().set(request.getCacheKey(), new CacheValue(staticXDOM, transformedXDOM));
    }

    @Override
    public String getType()
    {
        return JOBTYPE;
    }

    private XDOM transformContent(XDOM xdom) throws TransformationException
    {
        // Make sure to set a new Execution Context so that the cached content executes in a clean context without
        // any context of user, etc. Imagine that the cache macro would contain a velocity script checking if the
        // user is admin and then show some private information if he has. It would mean that if an admin navigate to
        // the page the result would be cached for anyone else coming thereafter!

        TransformationContext txContext = new TransformationContext(xdom, extractSyntax(xdom));
        XDOM transformedXDOM = xdom.clone();
        this.macroTransformation.transform(transformedXDOM, txContext);
        return transformedXDOM;
    }

    private Syntax extractSyntax(XDOM xdom) throws TransformationException
    {
        Syntax syntax = null;

        // The parser who created the passed XDOM should have set the syntax in the XDOM, try to get it.
        MetaData metaData = xdom.getMetaData();
        if (metaData != null) {
            syntax = (Syntax) metaData.getMetaData(MetaData.SYNTAX);
        }

        if (syntax == null) {
            throw new TransformationException("Failed to extract syntax from XDOM");
        }

        return syntax;
    }
}
