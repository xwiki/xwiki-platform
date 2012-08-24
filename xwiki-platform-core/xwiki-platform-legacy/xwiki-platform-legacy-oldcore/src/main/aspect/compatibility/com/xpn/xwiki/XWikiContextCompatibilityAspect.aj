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
package compatibility.com.xpn.xwiki;
 
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocumentArchive;
import com.xpn.xwiki.objects.classes.BaseClass;
import org.apache.commons.lang3.StringUtils;

/**
 * Add a backward compatibility layer to the {@link com.xpn.xwiki.XWikiContext} class.
 * 
 * @version $Id$
 */
public privileged aspect XWikiContextCompatibilityAspect
{
    /**
     * @return true it's main wiki's context, false otherwise.
     * @deprecated replaced by {@link XWikiContext#isMainWiki()} since 1.4M1.
     */
    @Deprecated
    public boolean XWikiContext.isVirtual()
    {
        return !this.isMainWiki();
    }

    /**
     * @param virtual true it's main wiki's context, false otherwise.
     * @deprecated this methods is now useless because the virtuality of a wiki is resolved with a
     *             comparison between {@link XWikiContext#getDatabase()} and
     *             {@link XWikiContext#getMainXWiki()} since 1.4M1.
     */
    @Deprecated
    public void XWikiContext.setVirtual(boolean virtual)
    {
        // this.virtual = virtual;
    }

    /**
     * Add a {@link XWikiDocumentArchive document archive} in a cache associated with this context, so that future
     * access requests for the same document archive don't go through the database again.
     *
     * @param key the key used to identify a document archive in the cache
     * @param archive the {@link XWikiDocumentArchive document archive} to cache
     */
    @Deprecated
    public void XWikiContext.addDocumentArchive(String key, XWikiDocumentArchive archive)
    {
        // Don't do anything, see XWIKI-7585
    }

    /**
     * Get the cached {@link XWikiDocumentArchive document archive} from the context, if any.
     *
     * @param key the key used to identify a document archive in the cache
     * @return the document archive, if it does exist in the context cache, or {@code null} otherwise
     * @see #addDocumentArchive(String, XWikiDocumentArchive)
     */
    @Deprecated
    public XWikiDocumentArchive XWikiContext.getDocumentArchive(String key)
    {
        // Act as if the cache is empty, see XWIKI-7585
        return null;
    }

    /**
     * Remove the cached {@link XWikiDocumentArchive document archive} from the context.
     *
     * @param key the key used to identify a document archive in the cache
     * @see #addDocumentArchive(String, XWikiDocumentArchive)
     */
    @Deprecated
    public void XWikiContext.removeDocumentArchive(String key)
    {
        // Don't do anything, see XWIKI-7585
    }

    /**
     * Empty the document archive cache.
     *
     * @see #addDocumentArchive(String, XWikiDocumentArchive)
     */
    @Deprecated
    public void XWikiContext.flushArchiveCache()
    {
        // Don't do anything, see XWIKI-7585
    }

    /**
     * @deprecated since 2.2M2 use {@link #getBaseClass(DocumentReference)}
     */
    // Used to avoid recursive loading of documents if there are recursives usage of classes
    @Deprecated
    public BaseClass XWikiContext.getBaseClass(String name)
    {
        BaseClass baseClass = null;
        if (StringUtils.isNotEmpty(name)) {
            baseClass = this.classCache.get(this.currentMixedDocumentReferenceResolver.resolve(name));
        }
        return baseClass;
    }

    /**
     * @deprecated since 3.1M1 use {@link #setUserReference(DocumentReference)} instead
     */
    @Deprecated
    public void XWikiContext.setUser(String user, boolean main)
    {
        this.setUserInternal(user, main);
    }
}
