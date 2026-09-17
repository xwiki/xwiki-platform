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
package org.xwiki.repository.script;

import java.util.Collection;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.context.Execution;
import org.xwiki.extension.ExtensionException;
import org.xwiki.extension.ExtensionSupportPlans;
import org.xwiki.extension.repository.ExtensionRepository;
import org.xwiki.extension.repository.ExtensionRepositoryManager;
import org.xwiki.extension.version.Version;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.query.QueryException;
import org.xwiki.repository.internal.ExtensionStore;
import org.xwiki.repository.internal.RepositoryManager;
import org.xwiki.script.service.ScriptService;
import org.xwiki.security.authorization.AuthorizationManager;
import org.xwiki.security.authorization.Right;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.api.Object;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

@Component
@Named("repository")
@Singleton
public class RepositoryScriptService implements ScriptService
{
    /**
     * The key under which the last encountered error is stored in the current execution context.
     */
    private static final String REPOSITORYERROR_KEY = "scriptservice.repository.error";

    @Inject
    private RepositoryManager repositoryManager;

    @Inject
    private ExtensionRepositoryManager extensionRepositoryManager;

    @Inject
    private ExtensionStore extensionStore;

    @Inject
    private AuthorizationManager authorization;

    /**
     * Provides access to the current context.
     */
    @Inject
    private Execution execution;

    @Inject
    private Provider<XWikiContext> contextProvider;

    /**
     * Store a caught exception in the context, so that it can be later retrieved using {@link #getLastError()}.
     * 
     * @param e the exception to store, can be {@code null} to clear the previously stored exception
     * @see #getLastError()
     */
    private void setError(Exception e)
    {
        this.execution.getContext().setProperty(REPOSITORYERROR_KEY, e);
    }

    /**
     * Get the error generated while performing the previously called action.
     * 
     * @return an eventual exception or {@code null} if no exception was thrown
     */
    public Exception getLastError()
    {
        return (Exception) this.execution.getContext().getProperty(REPOSITORYERROR_KEY);
    }

    public void validateExtensions()
    {
        setError(null);

        try {
            this.repositoryManager.validateExtensions();
        } catch (Exception e) {
            setError(e);
        }
    }

    public DocumentReference importExtension(String extensionId, String repositoryId)
    {
        setError(null);

        try {
            ExtensionRepository repository = this.extensionRepositoryManager.getRepository(repositoryId);

            if (repository == null) {
                throw new ExtensionException("Can't find any registered repository with id [" + repositoryId + "]");
            }

            return this.repositoryManager.importExtension(extensionId, repository, Version.Type.STABLE);
        } catch (Exception e) {
            setError(e);
        }

        return null;
    }

    /**
     * @param supportPlanIds the identifier of the support plans
     * @return the {@link ExtensionSupportPlans} instance describing how an extension is supported
     * @since 16.8.0RC1
     */
    public ExtensionSupportPlans resolveExtensionSupportPlans(Collection<String> supportPlanIds)
    {
        return this.extensionStore.resolveExtensionSupportPlans(supportPlanIds);
    }

    /**
     * @param extensionId the identifier of the extension
     * @param version the version for which to find the object
     * @return the object holding the extension version metadata, or null if none could be found
     * @throws QueryException when failing to get the version object
     * @throws XWikiException when failing to get the version object
     * @since 17.9.0RC1
     */
    public Object getVersionObject(String extensionId, String version) throws QueryException, XWikiException
    {
        XWikiDocument extensionDocument = this.extensionStore.getExistingExtensionDocumentById(extensionId);

        if (extensionDocument == null || !canView(extensionDocument.getDocumentReference())) {
            return null;
        }

        XWikiContext xcontext = this.contextProvider.get();

        DocumentReference versionDocumentReference =
            this.extensionStore.getExtensionVersionDocumentReference(extensionDocument, version, xcontext);

        if (!canView(versionDocumentReference)) {
            return null;
        }

        XWikiDocument versionDocument = xcontext.getWiki().getDocument(versionDocumentReference, xcontext);
        BaseObject extensionVersionObject = this.extensionStore.getExtensionVersionObject(versionDocument, version);

        return extensionVersionObject != null ? new Object(extensionVersionObject, xcontext) : null;
    }

    /**
     * A script service is callable with only Script right, which does not imply the right to view every document of
     * the wiki, so version metadata must not be handed to a script whose author is not allowed to view the document
     * holding it.
     *
     * @param documentReference the reference of the document to read the version metadata from
     * @return {@code true} if the author of the current script is allowed to view the passed document
     */
    private boolean canView(DocumentReference documentReference)
    {
        return this.authorization.hasAccess(Right.VIEW, this.contextProvider.get().getAuthorReference(),
            documentReference);
    }
}
