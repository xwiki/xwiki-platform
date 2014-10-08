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

import java.util.Stack;
import java.util.concurrent.Callable;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.model.reference.DocumentReference;

import com.google.common.base.Objects;
import com.xpn.xwiki.XWikiConstant;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;

/**
 * Allow executing some code with the right of a provided user.
 * 
 * @version $Id$
 * @since 6.3M1
 */
@Component(roles = SUExecutor.class)
@Singleton
public class SUExecutor
{
    /**
     * Contain the informations to restore.
     * 
     * @version $Id$
     */
    public static final class SUExecutorContext
    {
        private XWikiDocument currentDocument;

        private Object xwikiContextDropPermissionHack;

        private Object documentDropPermissionHack;

        private SUExecutorContext()
        {
            // Only SUExecutor can create SUExecutorContext
        }
    }

    @Inject
    private Provider<XWikiContext> xcontextProvider;

    @Inject
    private Execution execution;

    @Inject
    private Logger logger;

    /**
     * Execute the passed {@link Callable} with the rights of the passed user.
     * 
     * @param callable the the task to execute
     * @param author the user to check rights on
     * @return computed result
     * @throws Exception if unable to compute a result
     * @param <V> the result type of method <tt>call</tt>
     */
    public <V> V call(Callable<V> callable, DocumentReference author) throws Exception
    {
        SUExecutorContext context = before(author);

        try {
            return callable.call();
        } finally {
            after(context);
        }
    }

    /**
     * Setup the context so that following code is executed with provided user rights.
     * <p>
     * The returned {@link SUExecutorContext} should be given to {@link #after(SUExecutorContext)} to restore the
     * context to its previous state.
     * 
     * @param user the user to check rights on
     * @return the context to restore
     * @see #after(SUExecutorContext)
     */
    public SUExecutorContext before(DocumentReference user)
    {
        SUExecutorContext suContext;

        XWikiContext xwikiContext = this.xcontextProvider.get();

        if (xwikiContext != null) {
            suContext = new SUExecutorContext();

            suContext.currentDocument = xwikiContext.getDoc();

            if (suContext.currentDocument != null
                && Objects.equal(suContext.currentDocument.getContentAuthorReference(), user)) {
                return suContext;
            }

            // Make sure to not modify the instance stored in the document cache
            XWikiDocument document;
            if (suContext.currentDocument != null) {
                document = suContext.currentDocument.clone();
                xwikiContext.setDoc(document);
            } else {
                document =
                    new XWikiDocument(new DocumentReference(xwikiContext.getMainXWiki() != null
                        ? xwikiContext.getMainXWiki() : "xwiki", "SUSpace", "SUPage"));
                document.setContentAuthorReference(user);
                document.setAuthorReference(user);
                document.setCreatorReference(user);
                xwikiContext.setDoc(document);
            }

            // Set the new temporary content author so that programming right is tested on it
            document.setContentAuthorReference(user);

            // Make sure to disable XWikiContext#dropPermission hack
            suContext.xwikiContextDropPermissionHack = xwikiContext.remove(XWikiConstant.DROPPED_PERMISSIONS);

            // Make sure to disable Document#dropPermission hack
            ExecutionContext econtext = this.execution.getContext();
            if (econtext != null) {
                suContext.documentDropPermissionHack = econtext.getProperty(XWikiConstant.DROPPED_PERMISSIONS);
                econtext.removeProperty(XWikiConstant.DROPPED_PERMISSIONS);
            }
        } else {
            suContext = null;
        }

        return suContext;
    }

    /**
     * Restore the context to it's previous state as defined by the provided {@link SUExecutorContext}.
     * 
     * @param suContext the context to restore
     * @see #before(DocumentReference)
     */
    public void after(SUExecutorContext suContext)
    {
        XWikiContext xwikiContext = this.xcontextProvider.get();

        if (xwikiContext != null) {
            // Restore context document's content author
            xwikiContext.setDoc(suContext.currentDocument);

            // Restore XWikiContext#dropPermission hack
            if (suContext.xwikiContextDropPermissionHack != null) {
                xwikiContext.put(XWikiConstant.DROPPED_PERMISSIONS, suContext.xwikiContextDropPermissionHack);
            }

            // Restore Document#dropPermission hack
            if (suContext.documentDropPermissionHack != null) {
                ExecutionContext econtext = this.execution.getContext();
                econtext.setProperty(XWikiConstant.DROPPED_PERMISSIONS, suContext.documentDropPermissionHack);
            }
        }
    }

    /**
     * Setup the context so that following code is executed with provided user rights.
     * <p>
     * {@link #pop()} should be called to restore the context to its previous state.
     * 
     * @param user the user to check rights on
     * @see #pop()
     */
    public void push(DocumentReference user)
    {
        // Modify the context for that following code is executed with the right of wiki macro author
        SUExecutorContext sucontext = before(user);

        // Put it in an hidden context property to restore it later
        ExecutionContext econtext = this.execution.getContext();
        // Use a stack in case a wiki macro calls another wiki macro
        Stack<SUExecutorContext> backup = (Stack<SUExecutorContext>) econtext.getProperty(SUExecutor.class.getName());
        if (backup == null) {
            backup = new Stack<SUExecutorContext>();
            econtext.setProperty(SUExecutor.class.getName(), backup);
        }
        backup.push(sucontext);
    }

    /**
     * Restore the context to it's previous state as defined by the provided {@link SUExecutorContext}.
     */
    public void pop()
    {
        // Get the su context to restore
        ExecutionContext econtext = this.execution.getContext();
        // Use a stack in case a wiki macro calls another wiki macro
        Stack<SUExecutorContext> backup = (Stack<SUExecutorContext>) econtext.getProperty(SUExecutor.class.getName());
        if (backup != null && !backup.isEmpty()) {
            // Restore the context execution rights
            after(backup.pop());
        } else {
            this.logger.error("Can't find any backed up execution right information in the execution context");
        }
    }
}
