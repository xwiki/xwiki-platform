/*
 * Copyright 2005 Jens Krämer
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
 * Created on 15.04.2005
 */
package net.jkraemer.xwiki.plugins.emailnotify;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

/**
 * Container for all modifications done to a specific page between two
 * notification runs.
 * @author <a href="mailto:jk@jkraemer.net">Jens Kraemer </a>
 */
public class PageModifications
{
    private final List modifications = new ArrayList();
    private final PageData pageData;
    
    private static final Logger LOG = Logger.getLogger (PageModifications.class);   
    
    public PageModifications (PageModification firstModification)
    {
        modifications.add (firstModification);
        pageData = firstModification.getPageData();
    }

    public synchronized void addModification (PageModification pm)
    {
        modifications.add (pm);
    }

    /**
     * returns a list of modifications which is created by removing subsequent
     * modifications done by the same user (which occur mainly because of backup saves)
     * to make the notification mails less noisy. should keep only the last modification 
     * of such a series...
     * @return stripped down list of modifications
     * @todo implement this
     */
    public List getSummarizedModifications ()
    {
        // TODO: walk through modifications and sum up subsequent modifications
        // in a time interval (10 minutes or so)
        // done by the same user
   	
        return null;
    }
    
    public PageModification getLastModification ()
    {
        return (PageModification) modifications.get(modifications.size()-1);
    }

    /**
     * @param user
     * @return Last modification that was not made by user and that isn't
     * marked as "don't send now". Returns null if no such modification found.
     */
    public PageModification getLastModificationNotBy(String user)
    {
    	if (LOG.isDebugEnabled()) LOG.debug("In getLastModificationNotBy " + user );
    	int modIndex=modifications.size()-1;
    	PageModification retpm = null;
    	while (modIndex>=0) {
    		PageModification pm = (PageModification) modifications.get(modIndex);
    		if (LOG.isDebugEnabled()) LOG.debug("Checking mod by " + pm.getModifier() + "against " + user );
    		if (!user.equals(pm.getModifier()) && !pm.isDontSendNow()) {
    			retpm=pm;
    			break;
    		}
    		modIndex--;
    	}
    	if (LOG.isDebugEnabled()) LOG.debug("returning " + retpm );
    	return retpm;
    }    
    
    /**
     * @return
     */
    public List getModifications ()
    {
        return modifications;
    }
    
    /**
     * @return 
     */
    public PageData getPageData ()
    {
        return pageData;
    }
}
