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
package org.xwiki.extension.index.internal.job;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.xwiki.component.namespace.Namespace;
import org.xwiki.job.AbstractRequest;

/**
 * The request to use to configure the {@link ExtensionIndexJob} job.
 * 
 * @version $Id$
 * @since 12.10RC1
 */
public class ExtensionIndexRequest extends AbstractRequest
{
    /**
     * The identifier of the job.
     */
    public static final List<String> JOB_ID = Arrays.asList("extension", "index");

    private static final long serialVersionUID = 1L;

    private boolean localExtensionsEnabled;

    private final boolean remoteExtensionsEnabled;

    private final boolean versionsEnabled;

    private List<Namespace> namespaces;

    /**
     * @param localExtensionsEnabled true if local extensions should be loaded
     * @param remoteExtensionsEnabled true if remote extensions should be loaded
     * @param versionsEnabled true if available version should be updated
     * @param namespaces the namespaces on which to check the compatibility of indexed extensions
     */
    public ExtensionIndexRequest(boolean localExtensionsEnabled, boolean remoteExtensionsEnabled,
        boolean versionsEnabled, Collection<Namespace> namespaces)
    {
        setId(JOB_ID);

        setVerbose(false);
        setStatusLogIsolated(false);

        this.localExtensionsEnabled = localExtensionsEnabled;
        this.remoteExtensionsEnabled = remoteExtensionsEnabled;
        this.versionsEnabled = versionsEnabled;
        this.namespaces =
            namespaces != null ? Collections.unmodifiableList(new ArrayList<>(namespaces)) : Collections.emptyList();

        if (this.namespaces.size() == 1) {
            setId(getId(this.namespaces.get(0)));
        }
    }

    /**
     * @param request the request to copy
     */
    public ExtensionIndexRequest(ExtensionIndexRequest request)
    {
        this(request.isLocalExtensionsEnabled(), request.isRemoteExtensionsEnabled(), request.isVersionsEnabled(),
            request.getNamespaces());
    }

    /**
     * @param namespace the namespace for which to validate extensions
     * @return the id of the job
     */
    public static List<String> getId(Namespace namespace)
    {
        List<String> id = new ArrayList<>(JOB_ID);

        if (namespace != null) {
            String namespaceString = namespace.serialize();
            if (namespaceString != null) {
                id.add(namespace.serialize());
            }
        }

        return id;
    }

    /**
     * @param localExtensionsEnabled true if local extensions should be loaded
     */
    public void setLocalExtensionsEnabled(boolean localExtensionsEnabled)
    {
        this.localExtensionsEnabled = localExtensionsEnabled;
    }

    /**
     * @param namespace the namespace for which to validate extensions
     */
    public void addNamespace(Namespace namespace)
    {
        List<Namespace> list = new ArrayList<>();
        list.addAll(this.namespaces);
        list.add(namespace);

        this.namespaces = Collections.unmodifiableList(list);
    }

    /**
     * @param namespace the namespace for which to stop validate extensions
     */
    public void removeNamespace(Namespace namespace)
    {
        List<Namespace> list = new ArrayList<>();
        list.addAll(this.namespaces);
        list.remove(namespace);

        this.namespaces = Collections.unmodifiableList(list);
    }

    /**
     * @return true if local extensions should be loaded
     */
    public boolean isLocalExtensionsEnabled()
    {
        return this.localExtensionsEnabled;
    }

    /**
     * @return true if remote extensions should be loaded
     */
    public boolean isRemoteExtensionsEnabled()
    {
        return this.remoteExtensionsEnabled;
    }

    /**
     * @return true if available version should be updated
     */
    public boolean isVersionsEnabled()
    {
        return this.versionsEnabled;
    }

    /**
     * @return the namespaces on which to check the compatibility of indexed extensions
     */
    public List<Namespace> getNamespaces()
    {
        return this.namespaces;
    }
}
