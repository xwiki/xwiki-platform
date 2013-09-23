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
package com.xpn.xwiki.plugin.webdav.resources.partial;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.jackrabbit.webdav.DavConstants;
import org.apache.jackrabbit.webdav.DavException;
import org.apache.jackrabbit.webdav.DavMethods;
import org.apache.jackrabbit.webdav.DavResource;
import org.apache.jackrabbit.webdav.DavResourceFactory;
import org.apache.jackrabbit.webdav.DavResourceLocator;
import org.apache.jackrabbit.webdav.DavServletResponse;
import org.apache.jackrabbit.webdav.DavSession;
import org.apache.jackrabbit.webdav.MultiStatusResponse;
import org.apache.jackrabbit.webdav.io.InputContext;
import org.apache.jackrabbit.webdav.lock.ActiveLock;
import org.apache.jackrabbit.webdav.lock.LockDiscovery;
import org.apache.jackrabbit.webdav.lock.LockInfo;
import org.apache.jackrabbit.webdav.lock.LockManager;
import org.apache.jackrabbit.webdav.lock.Scope;
import org.apache.jackrabbit.webdav.lock.SupportedLock;
import org.apache.jackrabbit.webdav.lock.Type;
import org.apache.jackrabbit.webdav.property.DavProperty;
import org.apache.jackrabbit.webdav.property.DavPropertyName;
import org.apache.jackrabbit.webdav.property.DavPropertyNameIterator;
import org.apache.jackrabbit.webdav.property.DavPropertyNameSet;
import org.apache.jackrabbit.webdav.property.DavPropertySet;
import org.apache.jackrabbit.webdav.property.DefaultDavProperty;
import org.apache.jackrabbit.webdav.property.ResourceType;

import com.xpn.xwiki.plugin.webdav.resources.XWikiDavResource;
import com.xpn.xwiki.plugin.webdav.resources.domain.DavTempFile;
import com.xpn.xwiki.plugin.webdav.utils.XWikiDavContext;

/**
 * The superclass for all XWiki WebDAV resources.
 * 
 * @version $Id$
 */
public abstract class AbstractDavResource implements XWikiDavResource
{
    /**
     * Name of this resource.
     */
    protected String name;

    /**
     * Resource locator for this resource. {@link DavResourceLocator}.
     */
    protected DavResourceLocator locator;

    /**
     * Parent resource (collection).
     */
    protected XWikiDavResource parentResource;

    /**
     * XWiki WebDAV Context. {@link XWikiDavContext}
     */
    private XWikiDavContext context;

    @Override
    public void init(XWikiDavResource parent, String name, String relativePath) throws DavException
    {
        DavResourceLocator locator =
            parent.getLocator().getFactory().createResourceLocator(parent.getLocator().getPrefix(),
                parent.getLocator().getWorkspacePath(), parent.getLocator().getResourcePath() + relativePath);
        init(name, locator, parent.getContext());
        this.parentResource = parent;

    }

    @Override
    public void init(String name, DavResourceLocator locator, XWikiDavContext context) throws DavException
    {
        this.name = name;
        this.locator = locator;
        this.context = context;
        // set fundamental properties (Will be overridden as necessary)
        // Some properties are cached and should not be overwritten.
        DavPropertySet propertySet = getVirtualProperties();
        if (propertySet.get(DavPropertyName.CREATIONDATE) == null) {
            String timeStamp = DavConstants.creationDateFormat.format(new Date());
            propertySet.add(new DefaultDavProperty(DavPropertyName.CREATIONDATE, timeStamp));
        }
        propertySet.add(new DefaultDavProperty(DavPropertyName.DISPLAYNAME, getDisplayName()));
        if (isCollection()) {
            propertySet.add(new ResourceType(ResourceType.COLLECTION));
            // Windows XP support
            propertySet.add(new DefaultDavProperty(DavPropertyName.ISCOLLECTION, "1"));
        } else {
            propertySet.add(new ResourceType(ResourceType.DEFAULT_RESOURCE));
            // Windows XP support
            propertySet.add(new DefaultDavProperty(DavPropertyName.ISCOLLECTION, "0"));
        }
        /*
         * set current lock information. If no lock is set to this resource, an empty lockdiscovery will be returned in
         * the response.
         */
        propertySet.add(new LockDiscovery(getLock(Type.WRITE, Scope.EXCLUSIVE)));
        /*
         * lock support information: all locks are lockable.
         */
        SupportedLock supportedLock = new SupportedLock();
        supportedLock.addEntry(Type.WRITE, Scope.EXCLUSIVE);
        propertySet.add(supportedLock);
    }

    /**
     * The default decode implementation assumes the next resource in chain to be a temporary resource. Sub classes
     * should override this method to provide their own implementation.
     */
    public XWikiDavResource decode(String[] tokens, int next) throws DavException
    {
        if (!isCollection()) {
           throw new DavException(DavServletResponse.SC_BAD_REQUEST);
        }
        String nextToken = tokens[next];
        boolean last = (next == tokens.length - 1);
        DavTempFile resource = new DavTempFile();
        String method = getContext().getMethod();
        if (method != null && DavMethods.getMethodCode(method) == DavMethods.DAV_MKCOL) {
            resource.setCollection();
        }
        resource.init(this, nextToken, "/" + nextToken);
        // Search inside session resources to see if we already have this resource stored
        int index = getVirtualMembers().indexOf(resource);
        if (index != -1) {
            // Use the old resource instead.
            resource = (DavTempFile) getVirtualMembers().get(index);
            // Re-init the old resource.
            resource.init(this, nextToken, "/" + nextToken);
        }
        return last ? resource : resource.decode(tokens, next + 1);
    }

    @Override
    public boolean isLockable(Type type, Scope scope)
    {
        return Type.WRITE.equals(type) && Scope.EXCLUSIVE.equals(scope);
    }

    @Override
    public ActiveLock getLock(Type type, Scope scope)
    {
        return getContext().getLockManager().getLock(type, scope, this);
    }

    @Override
    public ActiveLock[] getLocks()
    {
        ActiveLock writeLock = getLock(Type.WRITE, Scope.EXCLUSIVE);
        return (writeLock != null) ? new ActiveLock[] {writeLock} : new ActiveLock[0];
    }

    @Override
    public boolean hasLock(Type type, Scope scope)
    {
        return getLock(type, scope) != null;
    }

    @Override
    public ActiveLock lock(LockInfo reqLockInfo) throws DavException
    {
        ActiveLock lock = null;
        if (isLockable(reqLockInfo.getType(), reqLockInfo.getScope())) {
            lock = getContext().getLockManager().createLock(reqLockInfo, this);
        } else {
            throw new DavException(DavServletResponse.SC_PRECONDITION_FAILED);
        }
        return lock;
    }

    @Override
    public ActiveLock refreshLock(LockInfo reqLockInfo, String lockToken) throws DavException
    {
        if (!exists()) {
            throw new DavException(DavServletResponse.SC_NOT_FOUND);
        }
        ActiveLock lock = getLock(reqLockInfo.getType(), reqLockInfo.getScope());
        if (lock == null) {
            throw new DavException(DavServletResponse.SC_PRECONDITION_FAILED);
        }
        return getContext().getLockManager().refreshLock(reqLockInfo, lockToken, this);
    }

    @Override
    public void unlock(String lockToken) throws DavException
    {
        ActiveLock lock = getLock(Type.WRITE, Scope.EXCLUSIVE);
        if (lock != null && lock.isLockedByToken(lockToken)) {
            getContext().getLockManager().releaseLock(lockToken, this);
        }
    }

    @Override
    public void copy(DavResource destination, boolean shallow) throws DavException
    {
        throw new DavException(DavServletResponse.SC_NOT_IMPLEMENTED);
    }

    /**
     * Default implementation simply returns all the cached properties.
     * 
     * @return The set of properties associated with this resource.
     */
    public DavPropertySet getProperties()
    {
        return getVirtualProperties();
    }

    @Override
    public DavProperty getProperty(DavPropertyName name)
    {
        return getProperties().get(name);
    }

    @Override
    public DavPropertyName[] getPropertyNames()
    {
        return getProperties().getPropertyNames();
    }

    @Override
    public MultiStatusResponse alterProperties(DavPropertySet setProperties, DavPropertyNameSet removePropertyNames)
        throws DavException
    {
        getProperties().addAll(setProperties);
        DavPropertyNameIterator it = removePropertyNames.iterator();
        while (it.hasNext()) {
            removeProperty(it.nextPropertyName());
        }
        return createPropStat();
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public MultiStatusResponse alterProperties(List changeList) throws DavException
    {
        for (Object next : changeList) {
            if (next instanceof DavProperty) {
                DavProperty property = (DavProperty) next;
                setProperty(property);
            } else {
                DavPropertyName propertyName = (DavPropertyName) next;
                removeProperty(propertyName);
            }
        }
        return createPropStat();
    }

    /**
     * @return A {@link MultiStatusResponse} with all property statuses.
     */
    private MultiStatusResponse createPropStat()
    {
        DavPropertyNameSet propertyNameSet = new DavPropertyNameSet();
        for (DavPropertyName propertyName : getPropertyNames()) {
            propertyNameSet.add(propertyName);
        }
        return new MultiStatusResponse(this, propertyNameSet);
    }

    @Override
    public void removeProperty(DavPropertyName propertyName) throws DavException
    {
        getProperties().remove(propertyName);
    }

    @Override
    public void setProperty(DavProperty property) throws DavException
    {
        getProperties().add(property);
    }

    @Override
    public void addLockManager(LockManager lockmgr)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getDisplayName()
    {
        return this.name;
    }

    @Override
    public String getComplianceClass()
    {
        return COMPLIANCE_CLASS;
    }

    @Override
    public String getSupportedMethods()
    {
        return METHODS;
    }

    @Override
    public DavResourceFactory getFactory()
    {
        return getContext().getResourceFactory();
    }

    @Override
    public DavResourceLocator getLocator()
    {
        return this.locator;
    }

    @Override
    public String getResourcePath()
    {
        return this.locator.getResourcePath();
    }

    @Override
    public String getHref()
    {
        return this.locator.getHref(isCollection());
    }

    @Override
    public DavSession getSession()
    {
        return getContext().getDavSession();
    }

    @Override
    public DavResource getCollection()
    {
        return this.parentResource;
    }

    @Override
    public XWikiDavContext getContext()
    {
        return context;
    }

    @Override
    public List<XWikiDavResource> getVirtualMembers()
    {
        Map<String, List<XWikiDavResource>> vResourcesMap = getContext().getUserStorage().getResourcesMap();
        if (vResourcesMap.get(getResourcePath()) == null) {
            vResourcesMap.put(getResourcePath(), getInitMembers());
        }
        return vResourcesMap.get(getResourcePath());
    }

    @Override
    public DavPropertySet getVirtualProperties()
    {
        Map<String, DavPropertySet> vPropertiesMap = getContext().getUserStorage().getPropertiesMap();
        if (vPropertiesMap.get(getResourcePath()) == null) {
            vPropertiesMap.put(getResourcePath(), new DavPropertySet());
        }
        return vPropertiesMap.get(getResourcePath());
    }

    @Override
    public List<XWikiDavResource> getInitMembers()
    {
        return new ArrayList<XWikiDavResource>();
    }

    @Override
    public void clearCache()
    {
        Map<String, List<XWikiDavResource>> vResourcesMap = getContext().getUserStorage().getResourcesMap();
        Map<String, DavPropertySet> vPropertiesMap = getContext().getUserStorage().getPropertiesMap();
        vResourcesMap.remove(getResourcePath());
        vPropertiesMap.remove(getResourcePath());
    }

    /**
     * Utility method for adding virtual members.
     * 
     * @param resource {@link XWikiDavResource} instance.
     * @param inputContext {@link InputContext}
     */
    public void addVirtualMember(DavResource resource, InputContext inputContext) throws DavException
    {
        XWikiDavResource davResource = (XWikiDavResource) resource;
        boolean isFile = (inputContext.getInputStream() != null);
        long modificationTime = inputContext.getModificationTime();
        if (davResource instanceof DavTempFile) {
            DavTempFile tempFile = (DavTempFile) davResource;
            if (isFile) {
                byte[] data = null;
                data = getContext().getFileContentAsBytes(inputContext.getInputStream());
                tempFile.update(data, new Date(modificationTime));
            } else {
                tempFile.setModified(new Date(modificationTime));
            }
        }
        // It's possible that we are updating an existing resource.
        if (!getVirtualMembers().contains(davResource)) {
            getVirtualMembers().add(davResource);
        }
    }

    /**
     * Utility method for removing virtual members.
     * 
     * @param member {@link XWikiDavResource} to be removed.
     */
    public void removeVirtualMember(DavResource member) throws DavException
    {
        XWikiDavResource davResource = (XWikiDavResource) member;
        if (getVirtualMembers().contains(davResource)) {
            getVirtualMembers().remove(davResource);
            davResource.clearCache();
        } else {
            throw new DavException(DavServletResponse.SC_NOT_FOUND);
        }
    }

    /**
     * Checks if the given resource name corresponds to an excluded resource, ie a resource asked by the OS but that
     * we want to ignore. This is because some WebDAV clients (such as Mac OSX Finder) send PROPFIND requests for
     * special resources that are not real resources and thus we don't want to handle those.
     *
     * Note 1: Mac OSX sends a *lot* of unnecessary PROPFIND requests for special filesystem resources (a.k.a
     * <a href="http://en.wikipedia.org/wiki/Resource_fork">Resource Forks</a>) + some other exotic stuff. See a
     * <a href="http://code.google.com/p/sabredav/wiki/Finder">good description</a> for what's happening.
     *
     * Note 2: As a consequence this means that XWiki Document names cannot start with ".", end with "~" or match
     * "mach_kernel" or "Backups.backupdb".
     *
     * @param resourceName Name of the resource.
     * @return True if the resourceName corresponds to a temporary file / directory. False otherwise.
     */
    public boolean isTempResource(String resourceName)
    {
        return resourceName.startsWith(".")
            || resourceName.endsWith("~")
            || resourceName.equals("mach_kernel")
            || resourceName.equals("Backups.backupdb");
    }
    
    /**
     * {@inheritDoc}    
     */
    public int hashCode()
    {
        return getResourcePath().hashCode();
    }

    @Override
    public boolean equals(Object obj)
    {
        if (obj instanceof DavResource) {
            DavResource other = (DavResource) obj;
            return getResourcePath().equals(other.getResourcePath());
        }
        return false;
    }
}
