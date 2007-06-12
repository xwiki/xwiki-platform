package com.xpn.xwiki.watch.client.test;

import com.google.gwt.junit.client.GWTTestCase;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.xpn.xwiki.watch.client.Watch;
import com.xpn.xwiki.watch.client.Feed;
import com.xpn.xwiki.watch.client.data.DataManager;
import com.xpn.xwiki.gwt.api.client.XWikiService;
import com.xpn.xwiki.gwt.api.client.XWikiServiceAsync;
import com.xpn.xwiki.gwt.api.client.Document;
import com.xpn.xwiki.gwt.api.client.app.XWikiAsyncCallback;

import java.util.List;

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

public class FeedTest extends GWTTestCase {
    protected Watch watch;
    protected boolean finished = false;
    protected DataManager datamgr;
    protected Feed currentFeed;

    public String getModuleName() {
        return "com.xpn.xwiki.watch.Watch";
    }

    protected void setUp() throws Exception {
        super.setUp();
        watch = new Watch();
        watch.setWatchSpace("WatchTest");
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void loginAndCleanup(final boolean loggedIn, final String testName) {
        XWikiServiceAsync svc = watch.getXWikiServiceInstance();
        if (!loggedIn) {
            svc.login("Admin", "admin", true, new AsyncCallback() {
                public void onFailure(Throwable throwable) {
                    assertTrue("Error logging in", false);
                }
                public void onSuccess(Object object) {
                    loginAndCleanup(true, testName);
                }
            });
        } else {
            svc.deleteDocuments(", BaseObject as obj where doc.fullName=obj.name and obj.className in ('XWiki.AggregatorURLClass','XWiki.AggregatorGroupClass', 'XWiki.KeywordClass') and doc.web='" + watch.getWatchSpace() + "'", new AsyncCallback() {
                public void onFailure(Throwable throwable) {
                    assertTrue("Error deleting", false);
                }
                public void onSuccess(Object object) {
                    // relaunch the test after login
                    launchTest(testName);
                }
            });
        }
        // This will be the real end of the test code..
        delayTestFinish(20000);
    }

    private void launchTest(String testName) {
        if (testName.equals("testAddFeed"))
         testAddFeed(true);
        else if (testName.equals("testRemoveFeed"))
         testRemoveFeed(true);
    }

    public void testAddFeed() {
        testAddFeed(false);
    }

    public void testAddFeed(boolean ready) {
        // Make sure we are ready
        if (!ready) {
            loginAndCleanup(false, "testAddFeed");
            return;
        }

        final String feedName = "Test";
        final String feedURL = "http://www.xwiki.com/";

        assertEquals("test", watch.getTranslation("test"));
        datamgr = new DataManager(watch);
        Feed feed = new Feed();
        feed.setName(feedName);
        feed.setUrl(feedURL);
        datamgr.addFeed(feed, new XWikiAsyncCallback(watch) {
            public void onFailure(Throwable throwable) {
                super.onFailure(throwable);
                assertTrue("Exception adding object", false);
            }

            public void onSuccess(Object object) {
                super.onSuccess(object);
                assertTrue("Error adding object", ((Boolean)object).booleanValue());
                checkHasOneFeed(feedName, feedURL, false);
            }
        });

        delayTestFinish(10000);
    }

    public void testRemoveFeed() {
        testRemoveFeed(false);
    }

    public void testRemoveFeed(boolean ready) {
        // Make sure we are ready
        if (!ready) {
            loginAndCleanup(false, "testRemoveFeed");
            return;
        }

        final String feedName = "Test";
        final String feedURL = "http://www.xwiki.com/";

        assertEquals("test", watch.getTranslation("test"));
        datamgr = new DataManager(watch);
        Feed feed = new Feed();
        feed.setName(feedName);
        feed.setUrl(feedURL);
        datamgr.addFeed(feed, new XWikiAsyncCallback(watch) {
            public void onFailure(Throwable throwable) {
                super.onFailure(throwable);
                assertTrue("Exception adding object", false);
            }

            public void onSuccess(Object object) {
                super.onSuccess(object);
                assertTrue("Error adding object", ((Boolean)object).booleanValue());
                // We will re-read server data and then run the remove test
                checkHasOneFeed(feedName, feedURL, true);
            }
        });
    }

    private void runRemove() {
        datamgr.removeFeed(currentFeed, new XWikiAsyncCallback(watch) {
            public void onFailure(Throwable throwable) {
                super.onFailure(throwable);
                assertTrue("Exception removing object", false);
            }

            public void onSuccess(Object object) {
                super.onSuccess(object);
                assertTrue("Error removing object", ((Boolean)object).booleanValue());
                checkHasNoFeed();
            }
        });        
    }

    private void checkHasOneFeed(final String feedName, final String feedURL, final boolean runRemove) {
        DataManager datamgr = new DataManager(watch);
        datamgr.getFeedList(new XWikiAsyncCallback(watch) {
            public void onFailure(Throwable caught) {
                super.onFailure(caught);
                assertTrue("Error retrieving feed list", false);
            }

            public void onSuccess(Object result) {
                super.onSuccess(result);
                List feedList = (List) result;
                assertEquals("There should be one feed", 1, feedList.size());
                currentFeed = new Feed((Document) feedList.get(0));
                assertEquals("Feed name is incorrect", feedName, currentFeed.getName());
                assertEquals("Feed url is incorrect", feedURL, currentFeed.getUrl());
                assertEquals("Feed page name is incorrect", "WatchTest." + feedName, currentFeed.getPageName());
                if (!runRemove)
                 finishTest();
                else
                 runRemove();
            }
        });
    }

    private void checkHasNoFeed() {
        DataManager datamgr = new DataManager(watch);
        datamgr.getFeedList(new XWikiAsyncCallback(watch) {
            public void onFailure(Throwable caught) {
                super.onFailure(caught);
                assertTrue("Error retrieving feed list", false);
            }

            public void onSuccess(Object result) {
                super.onSuccess(result);
                List feedList = (List) result;
                assertEquals("There should be zero feed", 0, feedList.size());
                finishTest();
            }
        });
    }

}