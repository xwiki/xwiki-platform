package com.xpn.xwiki.watch.client;

import com.xpn.xwiki.gwt.api.client.app.XWikiAsyncCallback;
import com.xpn.xwiki.gwt.api.client.Document;
import com.xpn.xwiki.gwt.api.client.XObject;
import com.xpn.xwiki.watch.client.data.Keyword;
import com.google.gwt.user.client.rpc.AsyncCallback;

import java.util.*;

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
public class Config {
    private Watch watch;
    private Map feedsList;
    private Map feedsByGroupList;
    private List keywords;
    private Map groups;

    public Config() {
    }

    public Config(Watch watch) {
        this.watch = watch;
        clearConfig();
    }

    public Map getFeedsList() {
        return feedsList;
    }

    public Map getFeedsByGroupList() {
        return feedsByGroupList;
    }

    public Map getGroups() {
        return groups;
    }

    public List getKeywords() {
        return keywords;
    }

    public void clearConfig() {
        feedsList = new HashMap();
        feedsByGroupList = new HashMap();
        keywords = new ArrayList();
        groups = new HashMap();
    }

    /**
     * Read the feed list, groups and keywords on the server
     */
    public void refreshConfig(final XWikiAsyncCallback cb) {
        watch.getDataManager().getFeedList(new XWikiAsyncCallback(watch) {
            public void onSuccess(Object result) {
                super.onSuccess(result);
                List feedDocuments = (List) result;
                clearConfig();
                for (int index=0;index<feedDocuments.size();index++) {
                        addToConfig((Document) feedDocuments.get(index));
                }
                if (cb!=null)
                 cb.onSuccess(result);
            }
        });
    }

    private void addToGroup(String group, String groupTitle, Feed feed) {
        Map feeds = (Map) feedsByGroupList.get(group);
        if (feeds == null) {
            feeds = new HashMap();
            feedsByGroupList.put(group, feeds);
        }
        if (feed!=null)
         feeds.put(feed.getName(), feed);
        if (!groups.containsKey(group))
          groups.put(group, groupTitle);
    }

    private void addToConfig(Document feedpage) {
        List fobjects = feedpage.getObjects("XWiki.AggregatorURLClass");
        if (fobjects!=null) {
            for (int i=0;i<fobjects.size();i++) {
                XObject xobj = (XObject) fobjects.get(i);
                Feed feed = new Feed(xobj);
                List feedgroups = feed.getGroups();
                if (feedgroups!=null) {
                    for (int j=0;j<feedgroups.size();j++) {
                        String groupFullName = (String) feedgroups.get(j);
                        addToGroup(groupFullName, groupFullName, feed);
                    }
                }
                String all = watch.getTranslation("all");
                addToGroup(all, all, feed);
                feedsList.put(feed.getName(), feed);
            }
        }
        List kobjects = feedpage.getObjects("XWiki.KeywordClass");
        if (kobjects!=null) {
            for (int j=0;j<kobjects.size();j++) {
                XObject xobj = (XObject) kobjects.get(j);
                Keyword keyword = new Keyword(xobj);
                keywords.add(keyword);
            }
         }

        List gobjects = feedpage.getObjects("XWiki.AggregatorGroupClass");
        if (gobjects!=null) {
            for (int j=0;j<gobjects.size();j++) {
                XObject xobj = (XObject) gobjects.get(j);
                String name = (String) xobj.getViewProperty("name");
                if ((name!=null)&&(!name.equals("")))
                    groups.put(feedpage.getFullName(), name);
            }
         }

    }

     public void refreshArticleNumber() {
         // Load the article counts
        watch.getDataManager().getArticleCount(new AsyncCallback() {
            public void onFailure(Throwable throwable) {
                // Silent error
            }
            public void onSuccess(Object result) {
                // Update the article list with the current results
                updateArticleNumbers((List) result);
                watch.getUserInterface().refreshData("feedtree");
            }
        });
     }

    public void updateArticleNumbers(List list) {
        if (list!=null) {
            for (int i=0;i<list.size();i++) {
                List result = (List) list.get(i);
                String feedname = (String) result.get(0);
                Integer count = (Integer)result.get(1);
                Feed feed = (Feed) feedsList.get(feedname);
                if (feed!=null) {
                   feed.setNb(count);
                }
            }
        }
        // watch.refreshFeedTreeUI();
    }

}
