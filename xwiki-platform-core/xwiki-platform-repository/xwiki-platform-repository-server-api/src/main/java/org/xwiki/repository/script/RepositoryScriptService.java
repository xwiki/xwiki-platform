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

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.api.Object;
import com.xpn.xwiki.doc.XWikiDocument;

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

    private ExtensionRepository getRepository(String repositoryId) throws ExtensionException
    {
        ExtensionRepository repository = this.extensionRepositoryManager.getRepository(repositoryId);

        if (repository == null) {
            throw new ExtensionException("Can't find any registered repository with id [" + repositoryId + "]");
        }

        return repository;
    }

    /**
     * Create or update an extension from the metadata found in the indicated repository.
     * 
     * @param extensionId the identifier of the extension to import
     * @param repositoryId the identifier of the repository from which to import the extension
     * @return the reference to the main document created or updated for the extension, or {@code null} if an error
     *         occurred
     */
    public DocumentReference importExtension(String extensionId, String repositoryId)
    {
        setError(null);

        try {
            return this.repositoryManager.importExtension(extensionId, getRepository(repositoryId),
                Version.Type.STABLE);
        } catch (Exception e) {
            setError(e);
        }

        return null;
    }

    /**
     * Create or update a project from the metadata found in the indicated repository.
     * 
     * @param projectId the identifier of the project to import
     * @param repositoryId the identifier of the repository from which to import the project
     * @return the reference to the main document created or updated for the project, or {@code null} if an error
     *         occurred
     * @since 18.5.0RC1
     * @since 18.4.2
     */
    public DocumentReference importProject(String projectId, String repositoryId)
    {
        setError(null);

        try {
            return this.repositoryManager.importProject(projectId, getRepository(repositoryId), Version.Type.STABLE);
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
     * @return the object holding the extension version metadata
     * @throws QueryException when failing to get the version object
     * @throws XWikiException when failing to get the version object
     * @since 17.9.0RC1
     */
    public Object getVersionObject(String extensionId, String version) throws QueryException, XWikiException
    {
        XWikiDocument extensionDocument = this.extensionStore.getExistingExtensionDocumentById(extensionId);

        XWikiContext context = this.contextProvider.get();
        // FIXME: add some checks
        XWikiDocument extensionVersionDocument =
            this.extensionStore.getExtensionVersionDocument(extensionDocument, version, context);
        return new Object(this.extensionStore.getExtensionVersionObject(extensionVersionDocument, version), context);
    }

    /**
     * @param projectId the identifier of the project
     * @param version the version for which to find the object
     * @return the object holding the project version metadata
     * @throws QueryException when failing to get the version object
     * @throws XWikiException when failing to get the version object
     * @since 18.5.0RC1
     * @since 18.4.2
     */
    public Object getProjectVersionObject(String projectId, String version) throws QueryException, XWikiException
    {
        XWikiDocument projectDocument = this.extensionStore.getExistingProjectDocumentById(projectId);

        XWikiContext context = this.contextProvider.get();
        // FIXME: add some checks
        XWikiDocument projectVersionDocument =
            this.extensionStore.getProjectVersionDocument(projectDocument, version, context);
        return new Object(this.extensionStore.getProjectVersionObject(projectVersionDocument, version), context);
    }
}
