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

import com.xpn.xwiki.render.XWikiVelocityRenderer;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.user.api.XWikiRightService;

import org.apache.velocity.VelocityContext;

import org.xwiki.component.annotation.Component;
import org.xwiki.context.Execution;
import org.xwiki.model.reference.DocumentReference;

import javax.inject.Inject;
import java.util.Set;
import java.util.HashSet;

/**
 * This renderer will elevate the privileges of a hard coded set of templates to delegate programming rights independent
 * of the context document.  Note that this set of templates will currently not be able to access the contex document at
 * all.
 * 
 * @version $Id$
 * @since 4.5M1
 */
@Component
public class DefaultPrivilegedTemplateRenderer implements PrivilegedTemplateRenderer
{
    /**
     * The reference of the superadmin user document.
     */
    private static final DocumentReference SUPERADMIN_REFERENCE = new DocumentReference("xwiki", "XWiki",
        XWikiRightService.SUPERADMIN_USER);

    /** The execution. */
    @Inject
    private Execution execution;

    /** The set of privileged templates. */
    private final Set<String> privilegedTemplates = new HashSet<String>();

    {
        // TODO: Add the "distribution" template to the set.
        privilegedTemplates.add("/templates/suggest.vm");
    }


    @Override
    public String evaluateTemplate(String content, String templateName)
    {
        XWikiContext context = (XWikiContext) execution.getContext().getProperty(XWikiContext.EXECUTIONCONTEXT_KEY);

        if (privilegedTemplates.contains(templateName)) {
            return evaluatePrivileged(content, templateName, context);
        } else {
            return evaluate(content, templateName, context);
        }
    }

    /**
     * Evaluate the template with delegation of programming rights enabled.
     *
     * @param content The template content.
     * @param templateName The template nam
     * @param context The context.
     * @return The rendered result.
     */
    private String evaluatePrivileged(String content, String templateName, XWikiContext context) {
        XWikiDocument oldDocument = context.getDoc();

        // Make sure to have programming rights
        // 
        // This is the same hack as is currently used in DistributionAction.
        //
        // Since the context document is replaced to delegate programming rights to the template, the template will not
        // be able to access the original context document.
        //
        // In the branch 'feature-authorization-context' a nicer solution will be possible, where programming rights can
        // be delegated to the template, while automatically dropping the programming rights when rendering a document
        // which doesn't have programming rights.
        XWikiDocument document =
            new XWikiDocument(new DocumentReference(context.getDatabase(),
                              SUPERADMIN_REFERENCE.getLastSpaceReference().getName(), "Distribution"));
        document.setContentAuthorReference(SUPERADMIN_REFERENCE);
        document.setAuthorReference(SUPERADMIN_REFERENCE);
        document.setCreatorReference(SUPERADMIN_REFERENCE);
        context.setDoc(document);

        try {
            return evaluate(content, templateName, context);
        } finally {
            context.setDoc(oldDocument);
        }
    }

    /**
     * Evaluate the template.
     *
     * @param content The template content.
     * @param templateName The template nam
     * @param context The context.
     * @return The rendered result.
     */
    private String evaluate(String content, String templateName, XWikiContext context)
    {
        return XWikiVelocityRenderer
            .evaluate(content, templateName, (VelocityContext) context.get("vcontext"), context);
    }
}