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

import javax.servlet.http.HttpSession;

import org.apache.jackrabbit.webdav.DavConstants;
import org.apache.jackrabbit.webdav.DavException;
import org.apache.jackrabbit.webdav.DavResource;
import org.apache.jackrabbit.webdav.DavResourceFactory;
import org.apache.jackrabbit.webdav.DavResourceLocator;
import org.apache.jackrabbit.webdav.DavServletResponse;
import org.apache.jackrabbit.webdav.DavSession;
import org.apache.jackrabbit.webdav.lock.ActiveLock;
import org.apache.jackrabbit.webdav.lock.LockDiscovery;
import org.apache.jackrabbit.webdav.lock.LockInfo;
import org.apache.jackrabbit.webdav.lock.LockManager;
import org.apache.jackrabbit.webdav.lock.Scope;
import org.apache.jackrabbit.webdav.lock.SupportedLock;
import org.apache.jackrabbit.webdav.lock.Type;
import org.apache.jackrabbit.webdav.property.DavProperty;
import org.apache.jackrabbit.webdav.property.DavPropertyName;
import org.apache.jackrabbit.webdav.property.DavPropertySet;
import org.apache.jackrabbit.webdav.property.DefaultDavProperty;
import org.apache.jackrabbit.webdav.property.ResourceType;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.plugin.webdav.resources.XWikiDavResource;

/**
 * The superclass for all XWiki WebDAV resources.
 * 
 * @version $Id$
 */
public abstract class AbstractDavResource implements XWikiDavResource
{
    /**
     * Resource locator for this resource. {@link DavResourceLocator}.
     */
    protected DavResourceLocator locator;

    /**
     * Resource factory which created this resource. {@link DavResourceFactory}
     */
    protected DavResourceFactory factory;

    /**
     * Current session associated with this resource. {@link DavSession}
     */
    protected DavSession session;

    /**
     * Set of properties of this resource. {@link DavPropertySet}
     */
    protected DavPropertySet davPropertySet;

    /**
     * XWiki Context. {@link XWikiContext}
     */
    protected XWikiContext xwikiContext;

    /**
     * Lock manager (this is shared).
     */
    protected LockManager lockManager;

    /**
     * Parent resource (collection).
     */
    protected XWikiDavResource parentResource;

    /**
     * Name of this resource.
     */
    protected String name;

    /**
     * Default constructor.
     */
    public AbstractDavResource()
    {
        this.davPropertySet = new DavPropertySet();
    }

    /**
     * {@inheritDoc}
     */
    public void init(XWikiDavResource parent, String name, String relativePath)
        throws DavException
    {
        this.locator =
            parent.getLocator().getFactory().createResourceLocator(
                parent.getLocator().getPrefix(), parent.getLocator().getWorkspacePath(),
                parent.getLocator().getResourcePath() + relativePath);
        this.factory = parent.getFactory();
        this.session = parent.getSession();
        this.lockManager = parent.getLockManager();
        this.xwikiContext = parent.getXwikiContext();
        this.parentResource = parent;
        this.name = name;
        initProperties();
    }

    /**
     * {@inheritDoc}
     */
    public void init(String name, DavResourceLocator locator, DavResourceFactory factory,
        DavSession session, LockManager lockManager, XWikiContext xwikiContext) throws DavException
    {
        this.locator = locator;
        this.factory = factory;
        this.session = session;
        this.lockManager = lockManager;
        this.xwikiContext = xwikiContext;
        this.name = name;
        initProperties();
    }

    /**
     * Initializes the default properties.
     */
    public void initProperties()
    {
        // set fundamental properties (Will be overridden as necessary)
        String timeStamp = DavConstants.creationDateFormat.format(new Date());
        davPropertySet.add(new DefaultDavProperty(DavPropertyName.CREATIONDATE, timeStamp));
        davPropertySet.add(new DefaultDavProperty(DavPropertyName.SOURCE, locator.getPrefix()
            + getResourcePath()));
        if (getDisplayName() != null) {
            davPropertySet.add(new DefaultDavProperty(DavPropertyName.DISPLAYNAME,
                getDisplayName()));
        }
        if (isCollection()) {
            davPropertySet.add(new ResourceType(ResourceType.COLLECTION));
            // Windows XP support
            davPropertySet.add(new DefaultDavProperty(DavPropertyName.ISCOLLECTION, "1"));
        } else {
            davPropertySet.add(new ResourceType(ResourceType.DEFAULT_RESOURCE));
            // Windows XP support
            davPropertySet.add(new DefaultDavProperty(DavPropertyName.ISCOLLECTION, "0"));
        }
        /*
         * set current lock information. If no lock is set to this resource, an empty lockdiscovery
         * will be returned in the response.
         */
        davPropertySet.add(new LockDiscovery(getLock(Type.WRITE, Scope.EXCLUSIVE)));
        /*
         * lock support information: all locks are lockable.
         */
        SupportedLock supportedLock = new SupportedLock();
        supportedLock.addEntry(Type.WRITE, Scope.EXCLUSIVE);
        davPropertySet.add(supportedLock);
    }

    /**
     * @return The set of virtual resources (some clients need such resources).
     */
    @SuppressWarnings("unchecked")
    public List<XWikiDavResource> getSessionResources()
    {
        HttpSession httpSession = xwikiContext.getRequest().getSession();
        if (httpSession.getAttribute(getResourcePath()) == null) {
            httpSession.setAttribute(getResourcePath(), new ArrayList<XWikiDavResource>());
        }
        return (List<XWikiDavResource>) httpSession.getAttribute(getResourcePath());
    }

    /**
     * @return The set of properties associated with this resource.
     */
    public DavPropertySet getProperties()
    {
        return this.davPropertySet;
    }

    /**
     * {@inheritDoc}
     */
    public DavProperty getProperty(DavPropertyName name)
    {
        return getProperties().get(name);
    }

    /**
     * {@inheritDoc}
     */
    public DavPropertyName[] getPropertyNames()
    {
        return getProperties().getPropertyNames();
    }

    /**
     * {@inheritDoc}
     */
    public boolean isLockable(Type type, Scope scope)
    {
        return Type.WRITE.equals(type) && Scope.EXCLUSIVE.equals(scope);
    }

    /**
     * {@inheritDoc}
     */
    public ActiveLock getLock(Type type, Scope scope)
    {
        return lockManager.getLock(type, scope, this);
    }

    /**
     * {@inheritDoc}
     */
    public ActiveLock[] getLocks()
    {
        ActiveLock writeLock = getLock(Type.WRITE, Scope.EXCLUSIVE);
        return (writeLock != null) ? new ActiveLock[] {writeLock} : new ActiveLock[0];
    }

    /**
     * {@inheritDoc}
     */
    public boolean hasLock(Type type, Scope scope)
    {
        return getLock(type, scope) != null;
    }

    /**
     * {@inheritDoc}
     */
    public ActiveLock lock(LockInfo reqLockInfo) throws DavException
    {
        ActiveLock lock = null;
        if (isLockable(reqLockInfo.getType(), reqLockInfo.getScope())) {
            lock = lockManager.createLock(reqLockInfo, this);
        } else {
            throw new DavException(DavServletResponse.SC_PRECONDITION_FAILED);
        }
        return lock;
    }

    /**
     * {@inheritDoc}
     */
    public ActiveLock refreshLock(LockInfo reqLockInfo, String lockToken) throws DavException
    {
        if (!exists()) {
            throw new DavException(DavServletResponse.SC_NOT_FOUND);
        }
        ActiveLock lock = getLock(reqLockInfo.getType(), reqLockInfo.getScope());
        if (lock == null) {
            throw new DavException(DavServletResponse.SC_PRECONDITION_FAILED);
        }
        return lockManager.refreshLock(reqLockInfo, lockToken, this);
    }

    /**
     * {@inheritDoc}
     */
    public void unlock(String lockToken) throws DavException
    {
        ActiveLock lock = getLock(Type.WRITE, Scope.EXCLUSIVE);
        if (lock == null) {
            throw new DavException(DavServletResponse.SC_PRECONDITION_FAILED);
        } else if (lock.isLockedByToken(lockToken)) {
            lockManager.releaseLock(lockToken, this);
        } else {
            throw new DavException(DavServletResponse.SC_LOCKED);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void addLockManager(LockManager lockmgr)
    {
        this.lockManager = lockmgr;
    }

    /**
     * {@inheritDoc}
     */
    public String getComplianceClass()
    {
        return DavResource.COMPLIANCE_CLASS;
    }

    /**
     * {@inheritDoc}
     */
    public DavResourceFactory getFactory()
    {
        return this.factory;
    }

    /**
     * {@inheritDoc}
     */
    public DavResourceLocator getLocator()
    {
        return this.locator;
    }

    /**
     * {@inheritDoc}
     */
    public String getResourcePath()
    {
        return this.locator.getResourcePath();
    }

    /**
     * {@inheritDoc}
     */
    public DavSession getSession()
    {
        return this.session;
    }

    /**
     * {@inheritDoc}
     */
    public DavResource getCollection()
    {
        return this.parentResource;
    }

    /**
     * @return the xwikiContext
     */
    public XWikiContext getXwikiContext()
    {
        return xwikiContext;
    }

    /**
     * @return the lockManager
     */
    public LockManager getLockManager()
    {
        return lockManager;
    }
    
    /**
     * {@inheritDoc}
     */
    public boolean equals(Object obj)
    {
        if (obj instanceof DavResource) {
            DavResource other = (DavResource) obj;
            return getResourcePath().equals(other.getResourcePath());
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public int hashCode()
    {
        return super.hashCode();
    }
}
