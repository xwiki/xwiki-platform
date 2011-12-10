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
 *
 */
package compatibility.com.xpn.xwiki;
 
import com.xpn.xwiki.XWikiContext;

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
}
