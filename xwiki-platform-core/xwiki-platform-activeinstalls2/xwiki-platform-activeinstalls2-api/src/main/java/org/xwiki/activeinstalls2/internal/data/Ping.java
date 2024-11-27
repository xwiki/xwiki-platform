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
package org.xwiki.activeinstalls2.internal.data;

import java.util.Collection;

/**
 * Represents a Ping.
 *
 * @version $Id$
 * @since 14.4RC1
 */
public class Ping
{
    private DatePing date;

    private DistributionPing distribution;

    private Collection<ExtensionPing> extensions;

    private JavaPing javaPing;

    private DatabasePing database;

    private OSPing os;

    private MemoryPing memory;

    private ServletContainerPing servletContainer;

    private UsersPing users;

    private WikisPing wikis;

    private DocumentsPing documents;

    /**
     * Empty constructor, you need to call setters to set ping data to send.
     */
    public Ping()
    {
        // Allows constructing a Ping object by calling the setters one by one.
    }

    /**
     * @return the XWiki-related distribution ping data, see {@link DistributionPing}
     */
    public DistributionPing getDistribution()
    {
        return this.distribution;
    }

    /**
     * @param distribution see {@link #getDistribution()}
     */
    public void setDistribution(DistributionPing distribution)
    {
        this.distribution = distribution;
    }

    /**
     * @return the date-related ping data, see {@link DatePing}
     */
    public DatePing getDate()
    {
        return this.date;
    }

    /**
     * @param date see {@link #getDate()}
     */
    public void setDate(DatePing date)
    {
        this.date = date;
    }

    /**
     * @return the XWiki Extension-related ping data, see {@link ExtensionPing}
     */
    public Collection<ExtensionPing> getExtensions()
    {
        return this.extensions;
    }

    /**
     * @param extensions see {@link #getExtensions()}
     */
    public void setExtensions(Collection<ExtensionPing> extensions)
    {
        this.extensions = extensions;
    }

    /**
     * @return the Java-related ping data, see {@link JavaPing}
     */
    public JavaPing getJava()
    {
        return this.javaPing;
    }

    /**
     * @param javaPing see {@link #getJava()}
     */
    public void setJava(JavaPing javaPing)
    {
        this.javaPing = javaPing;
    }

    /**
     * @return the memory-related ping data, see {@link MemoryPing}
     */
    public MemoryPing getMemory()
    {
        return this.memory;
    }

    /**
     * @param memory see {@link #getMemory()}
     */
    public void setMemory(MemoryPing memory)
    {
        this.memory = memory;
    }

    /**
     * @return the Servlet Container-related ping data, see {@link ServletContainerPing}
     */
    public ServletContainerPing getServletContainer()
    {
        return this.servletContainer;
    }

    /**
     * @param servletContainer see {@link #getServletContainer()}
     */
    public void setServletContainer(ServletContainerPing servletContainer)
    {
        this.servletContainer = servletContainer;
    }

    /**
     * @return the database-related ping data, see {@link DatabasePing}
     */
    public DatabasePing getDatabase()
    {
        return this.database;
    }

    /**
     * @param database see {@link #getDatabase()}
     */
    public void setDatabase(DatabasePing database)
    {
        this.database = database;
    }

    /**
     * @return the OS-related ping data, see {@link OSPing}
     */
    public OSPing getOS()
    {
        return this.os;
    }

    /**
     * @param os see {@link #getOS()}
     */
    public void setOS(OSPing os)
    {
        this.os = os;
    }

    /**
     * @return the users-related ping data, see {@link UsersPing}
     */
    public UsersPing getUsers()
    {
        return this.users;
    }

    /**
     * @param users see {@link #getUsers()}
     */
    public void setUsers(UsersPing users)
    {
        this.users = users;
    }

    /**
     * @return the users-related ping data, see {@link WikisPing}
     */
    public WikisPing getWikis()
    {
        return this.wikis;
    }

    /**
     * @param wikis see {@link #getWikis()}
     */
    public void setWikis(WikisPing wikis)
    {
        this.wikis = wikis;
    }

    /**
     * @return the documents-related ping data, see {@link DocumentsPing}
     */
    public DocumentsPing getDocuments()
    {
        return this.documents;
    }

    /**
     * @param documents see {@link #getDocuments()}
     */
    public void setDocuments(DocumentsPing documents)
    {
        this.documents = documents;
    }
}
