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

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;

import java.util.Date;

public class UpdateThread implements Runnable {
    protected boolean fullContent;
    protected String space;
    protected FeedPlugin feedPlugin;
    protected XWikiContext context;
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


    public UpdateThread(String space, boolean fullContent, int scheduleTimer, FeedPlugin feedPlugin, XWikiContext context) {
        this.fullContent = fullContent;
        this.space = space;
        this.feedPlugin = feedPlugin;
        this.scheduleTimer = scheduleTimer;
        this.context = context;
    }

    public void run() {
        while (true) {
            update();
            if (stopUpdate) {
                feedPlugin.removeUpdateThread(space, this);
                break;
            }
            try {
                Thread.sleep(scheduleTimer);
            } catch (InterruptedException e) {
                break;
            }
        }
    }

    public void update() {
        if (!stopUpdate) {
            if (updateInProgress==false) {
                updateInProgress = true;
                nbLoadedFeeds = 0;
                nbLoadedFeedsErrors = 0;
                exception = null;
                nbLoadedArticles = 0;
                endDate = null;
                startDate = new Date();
                try {
                    XWikiContext context = (XWikiContext) this.context.clone();
                    context.getWiki().getStore().cleanUp(context);
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
                if ((forceUpdate==true)&&(stopUpdate==false)) {
                    forceUpdate = false;
                    update();
                }
            } else {
                // let's schedule an update at the end of the current update
                forceUpdate = true;
            }
        }
    }

    public String getSpace() {
        return space;
    }

    public boolean isUpdateInProgress() {
        return updateInProgress;
    }

    public Date getStartDate() {
        return startDate;
    }

    public Date getEndDate() {
        return endDate;
    }

    public int getNbLoadedArticles() {
        return nbLoadedArticles;
    }

    public Exception getException() {
        return exception;
    }

    public void stopUpdate() {
        if (!updateInProgress) {
            feedPlugin.removeUpdateThread(space, this);             
        }
        stopUpdate = true;
    }

    public int getNbLoadedFeeds() {
        return nbLoadedFeeds;
    }

    public void setNbLoadedFeeds(int nbLoadedFeeds) {
        this.nbLoadedFeeds = nbLoadedFeeds;
    }

    public int getNbLoadedFeedsErrors() {
        return nbLoadedFeedsErrors;
    }

    public void setNbLoadedFeedsErrors(int nbLoadedFeedsErrors) {
        this.nbLoadedFeedsErrors = nbLoadedFeedsErrors;
    }
}
