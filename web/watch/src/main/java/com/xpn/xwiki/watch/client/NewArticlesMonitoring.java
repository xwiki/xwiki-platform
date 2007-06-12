package com.xpn.xwiki.watch.client;

import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.xpn.xwiki.gwt.api.client.XWikiService;
import com.xpn.xwiki.gwt.api.client.app.XWikiAsyncCallback;

import java.util.List;
import java.util.Date;
import java.util.Map;
import java.util.HashMap;

/**
 * Copyright 2006,XpertNet SARL,and individual contributors as indicated
 * by the contributors.txt.
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
 * @author ldubost
 */
public class NewArticlesMonitoring {

    private Watch watch;

    private Timer checkNbMessageTimer;
    private Timer blinkTimer;
    private String windowTitle;
    private String messageTitle;
    private int currentNbArticles;
    private Date lastChange;
    private boolean blinking = false;
    private boolean queryActive = false;

    public NewArticlesMonitoring() {}

    public NewArticlesMonitoring(Watch watch) {
        this.watch = watch;

        // Schedule the timer to run once every 10 seconds
        checkNbMessageTimer = new Timer() {
            public void run() {
                    onCheckNew();
            }
        };
        checkNbMessageTimer.scheduleRepeating(watch.getParamAsInt("newarticles_monitoring_timer", Constants.DEFAULT_PARAM_NEWARTICLES_MONITORING_TIMER));        
    }

    /**
     * Checking if we have articles
     */
    private void onCheckNew() {
        if (queryActive==false) {
            queryActive = true;
            watch.getDataManager().getNewArticles(new AsyncCallback() {
                public void onFailure(Throwable throwable) {
                    queryActive = false;
                }

                public void onSuccess(Object object) {
                    queryActive = false;
                    if (object!=null) {
                        List nblist= (List) ((List) object).get(0);
                        Integer nb = (nblist==null) ? null : (Integer) nblist.get(0);
                        if (nb!=null) {
                            int newNbArticles = nb.intValue();
                            if (currentNbArticles==-1)
                                currentNbArticles = newNbArticles;
                            else {
                                int newNb = newNbArticles - currentNbArticles;
                                if (newNb > 0) {
                                    String[] args1 = new String[1];
                                    args1[0] = "" + newNb;

                                    String[] args2 = new String[1];
                                    args2[0] = "" + currentNbArticles;
                                    // Let's update the last change date
                                    lastChange = new Date();
                                    startBlinking(watch.getTranslation("newarticles", args1), watch.getTranslation("articles", args2));
                                    // let's refresh the article numbers
                                    watch.getConfig().refreshArticleNumber();
                                }
                            }
                        }
                    }
                }
            });
        }
    }

    public void startBlinking(String message, String otherMessage) {
        if (otherMessage!=null)
            windowTitle = otherMessage;
        messageTitle = message;
        if (blinkTimer==null) {
            windowTitle = Window.getTitle();
            Window.setTitle(message);
            blinkTimer = new Timer() {
                boolean active = true;

                public void run() {
                    if (active) {
                        Window.setTitle(windowTitle);
                    } else {
                        Window.setTitle(messageTitle);
                    }
                    active = !active;
                }
            };
            if (!blinking) {
                blinking = true;
                blinkTimer.scheduleRepeating(2000);
            }
        }
    }

    public void stopBlinking() {
        if (blinkTimer!=null) {
            blinkTimer.cancel();
            blinking = false;
            blinkTimer = null;
            Window.setTitle(messageTitle);
        }
    }

    public int getArticlesNumber() {
        return currentNbArticles;
    }

    public Date lastChangeDate() {
        return lastChange;
    }
}
