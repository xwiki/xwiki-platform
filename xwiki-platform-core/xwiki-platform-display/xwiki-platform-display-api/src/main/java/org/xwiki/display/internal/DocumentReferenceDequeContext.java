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
package org.xwiki.display.internal;

import java.util.ArrayDeque;
import java.util.Deque;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.model.reference.DocumentReference;

/**
 * Provides a {@link Deque} that is stored in the execution context to store the stack of executed documents to avoid
 * infinite recursion.
 *
 * @version $Id$
 * @since 17.6.0RC1
 * @since 17.4.3
 * @since 16.10.10
 */
@Component(roles = DocumentReferenceDequeContext.class)
@Singleton
public class DocumentReferenceDequeContext
{
    /**
     * The key used to store on the XWiki context map the stack of references to documents that are currently
     * being evaluated (in the current execution context). This stack is used to prevent infinite recursion, which can
     * happen if the title displayer is called on the current document from the title field or from a script within the
     * first content heading.
     */
    private static final String DOCUMENT_REFERENCE_STACK_KEY = "internal.displayer.%s.documentReferenceStack";

    /**
     * Execution context handler, needed for accessing the XWiki context map.
     */
    @Inject
    private Execution execution;

    /**
     * @param displayerName the name of the displayer, like "title" or "content".
     * @return the stack of document references that are currently being evaluated
     */
    public Deque<DocumentReference> getDocumentReferenceDeque(String displayerName)
    {
        ExecutionContext econtext = this.execution.getContext();

        String contextKey = DOCUMENT_REFERENCE_STACK_KEY.formatted(displayerName);
        @SuppressWarnings("unchecked")
        Deque<DocumentReference> documentReferenceStack =
            (Deque<DocumentReference>) econtext.getProperty(contextKey);

        if (documentReferenceStack == null) {
            documentReferenceStack = new ArrayDeque<>();
            econtext.newProperty(contextKey).inherited().initial(documentReferenceStack).makeFinal().declare();
        }

        return documentReferenceStack;
    }
}
