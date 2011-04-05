/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 * <p/>
 * This is free software;you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation;either version2.1of
 * the License,or(at your option)any later version.
 * <p/>
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY;without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.See the GNU
 * Lesser General Public License for more details.
 * <p/>
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software;if not,write to the Free
 * Software Foundation,Inc.,51 Franklin St,Fifth Floor,Boston,MA
 * 02110-1301 USA,or see the FSF site:http://www.fsf.org.
 *
 */

package com.xpn.xwiki.plugin.feed;

import java.util.Date;

import org.xwiki.context.Execution;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.util.AbstractXWikiRunnable;
import com.xpn.xwiki.web.Utils;

public class UpdateThread extends AbstractXWikiRunnable
{
    protected boolean fullContent;

    protected String space;

    protected FeedPlugin feedPlugin;

    protected int scheduleTimer;

    protected boolean updateInProgress = false;

    protected boolean forceUpdate = false;

    protected boolean stopUpdate = false;

    protected Date startDate;

    protected Date endDate;

    protected int nbLoadedArticles;

    protected int nbLoadedFeeds;

    protected int nbLoadedFeedsErrors;

    protected Exception exception;

    public UpdateThread(String space, boolean fullContent, int scheduleTimer, FeedPlugin feedPlugin,
        XWikiContext context)
    {
        super(XWikiContext.EXECUTIONCONTEXT_KEY, context);
        
        this.fullContent = fullContent;
        this.space = space;
        this.feedPlugin = feedPlugin;
        this.scheduleTimer = scheduleTimer;
    }

    public void update()
    {
        if (!stopUpdate) {
            if (updateInProgress == false) {
                updateInProgress = true;
                nbLoadedFeeds = 0;
                nbLoadedFeedsErrors = 0;
                exception = null;
                nbLoadedArticles = 0;
                endDate = null;
                startDate = new Date();
                XWikiContext context = getXWikiContext();
                try {
                    // Make sure store sessions are cleaned up
                    context.getWiki().getStore().cleanUp(context);
                    // update the feeds
                    nbLoadedArticles = feedPlugin.updateFeedsInSpace(space, fullContent, true, false, context);
                } catch (XWikiException e) {
                    exception = e;
                    e.printStackTrace();
                } finally {
                    updateInProgress = false;
                    endDate = new Date();
                    context.getWiki().getStore().cleanUp(context);
                }
                // an update has been schedule..
                if ((forceUpdate == true) && (stopUpdate == false)) {
                    forceUpdate = false;
                    update();
                }
            } else {
                // let's schedule an update at the end of the current update
                forceUpdate = true;
            }
        }
    }

    private XWikiContext getXWikiContext()
    {
        return (XWikiContext) Utils.getComponent(Execution.class).getContext().getProperty("xwikicontext");
    }
    
    public String getSpace()
    {
        return space;
    }

    public boolean isUpdateInProgress()
    {
        return updateInProgress;
    }

    public Date getStartDate()
    {
        return startDate;
    }

    public Date getEndDate()
    {
        return endDate;
    }

    public int getNbLoadedArticles()
    {
        return nbLoadedArticles;
    }

    public Exception getException()
    {
        return exception;
    }

    public void stopUpdate()
    {
        if (!updateInProgress) {
            feedPlugin.removeUpdateThread(space, this, getXWikiContext());
        }
        stopUpdate = true;
    }

    public int getNbLoadedFeeds()
    {
        return nbLoadedFeeds;
    }

    public void setNbLoadedFeeds(int nbLoadedFeeds)
    {
        this.nbLoadedFeeds = nbLoadedFeeds;
    }

    public int getNbLoadedFeedsErrors()
    {
        return nbLoadedFeedsErrors;
    }

    public void setNbLoadedFeedsErrors(int nbLoadedFeedsErrors)
    {
        this.nbLoadedFeedsErrors = nbLoadedFeedsErrors;
    }

    /**
     * {@inheritDoc}
     * 
     * @see {@link AbstractXWikiRunnable#runInternal()}
     */
    @Override
    protected void runInternal()
    {
        while (true) {
            update();
            if (stopUpdate) {
                feedPlugin.removeUpdateThread(space, this, getXWikiContext());
                break;
            }
            try {
                Thread.sleep(scheduleTimer);
            } catch (InterruptedException e) {
                break;
            }
        }
    }
}
