package com.xpn.xwiki.watch.client.data;

import com.xpn.xwiki.watch.client.Watch;
import com.xpn.xwiki.watch.client.FilterStatus;
import com.xpn.xwiki.watch.client.Constants;
import com.xpn.xwiki.watch.client.Feed;
import com.xpn.xwiki.gwt.api.client.app.XWikiAsyncCallback;
import com.xpn.xwiki.gwt.api.client.XWikiService;
import com.xpn.xwiki.gwt.api.client.XObject;
import com.xpn.xwiki.gwt.api.client.Document;
import com.google.gwt.user.client.rpc.AsyncCallback;

import java.util.*;

import org.gwtwidgets.client.util.SimpleDateFormat;

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

public class DataManager {
    protected Watch watch;
    protected boolean useLucene;

    public DataManager(Watch watch) {
        this.watch = watch;
    }

    public void addFeed(final Feed feed, final AsyncCallback cb) {
        if (feed==null)
            cb.onFailure(null);

        final String feedName = feed.getName();
        final String feedURL = feed.getUrl();
        final List feedGroups = feed.getGroups();
        watch.getXWikiServiceInstance().getUniquePageName(watch.getWatchSpace(), feedName, new XWikiAsyncCallback(watch) {
            public void onFailure(Throwable caught) {
                super.onFailure(caught);
                // We failed to get a unique page name
                // This should not happen
                cb.onFailure(caught);
            }

            public void onSuccess(Object result) {
                super.onSuccess(result);
                // Construct the full page name
                final String pageName = watch.getWatchSpace() + "." + result;
                XObject feedObj = new XObject();
                feedObj.setName(pageName);
                feedObj.setClassName(Constants.CLASS_AGGREGATOR_URL);
                feedObj.setNumber(0);
                feedObj.setProperty(Constants.PROPERTY_AGGREGATOR_URL_NAME, feedName);
                feedObj.setProperty(Constants.PROPERTY_AGGREGATOR_URL_URL, feedURL);
                feedObj.setProperty(Constants.PROPERTY_AGGREGATOR_URL_GROUPS, feedGroups);
                watch.getXWikiServiceInstance().saveObject(feedObj, new AsyncCallback() {
                    public void onFailure(Throwable throwable) {
                        cb.onFailure(throwable);
                    }

                    public void onSuccess(Object object) {
                        // We return the page name
                        if (!((Boolean)object).booleanValue())
                            cb.onFailure(null);
                        else
                            cb.onSuccess(pageName);
                    }
                });
            }
        });
    }

    public void updateFeed(Feed feed, final XWikiAsyncCallback cb) {
        if ((feed==null)||(feed.getPageName()==null)||(feed.getPageName().equals("")))
            cb.onFailure(null);

        // Construct the full page name
        final String pageName = feed.getPageName();
        XObject feedObj = new XObject();
        feedObj.setName(pageName);
        feedObj.setClassName(Constants.CLASS_AGGREGATOR_URL);
        feedObj.setNumber(0);
        feedObj.setProperty(Constants.PROPERTY_AGGREGATOR_URL_NAME, feed.getName());
        feedObj.setProperty(Constants.PROPERTY_AGGREGATOR_URL_URL, feed.getUrl());
        feedObj.setProperty(Constants.PROPERTY_AGGREGATOR_URL_GROUPS, feed.getGroups());
        watch.getXWikiServiceInstance().saveObject(feedObj, new AsyncCallback() {
            public void onFailure(Throwable throwable) {
                cb.onFailure(throwable);
            }

            public void onSuccess(Object object) {
                // We return the page name
                if (!((Boolean)object).booleanValue())
                    cb.onFailure(null);
                else
                    cb.onSuccess(pageName);
            }
        });
    }

    public void addKeyword(final String keyword, final String group, final AsyncCallback cb) {
        if (keyword==null)
            cb.onFailure(null);

        watch.getXWikiServiceInstance().getUniquePageName(watch.getWatchSpace(), "Keyword_" + keyword, new XWikiAsyncCallback(watch) {
            public void onFailure(Throwable caught) {
                super.onFailure(caught);
                // We failed to get a unique page name
                // This should not happen
                cb.onFailure(caught);
            }

            public void onSuccess(Object result) {
                super.onSuccess(result);
                // Construct the full page name
                final String pageName = watch.getWatchSpace() + "." + result;
                XObject feedObj = new XObject();
                feedObj.setName(pageName);
                feedObj.setClassName(Constants.CLASS_AGGREGATOR_KEYWORD);
                feedObj.setNumber(0);
                feedObj.setProperty(Constants.PROPERTY_KEYWORD_NAME, keyword);
                feedObj.setProperty(Constants.PROPERTY_KEYWORD_GROUP, group);
                watch.getXWikiServiceInstance().saveObject(feedObj, new AsyncCallback() {
                    public void onFailure(Throwable throwable) {
                        cb.onFailure(throwable);
                    }

                    public void onSuccess(Object object) {
                        if (!((Boolean)object).booleanValue())
                            cb.onFailure(null);
                        else
                            cb.onSuccess(pageName);
                    }
                });
            }
        });
    }

    public void addGroup(final String group, final AsyncCallback cb) {
        if (group==null)
            cb.onFailure(null);

        watch.getXWikiServiceInstance().getUniquePageName(watch.getWatchSpace(), "Group_" + group, new XWikiAsyncCallback(watch) {
            public void onFailure(Throwable caught) {
                super.onFailure(caught);
                // We failed to get a unique page name
                // This should not happen
                cb.onFailure(caught);
            }

            public void onSuccess(Object result) {
                super.onSuccess(result);
                // Construct the full page name
                final String pageName = watch.getWatchSpace() + "." + result;
                XObject feedObj = new XObject();
                feedObj.setName(pageName);
                feedObj.setClassName(Constants.CLASS_AGGREGATOR_GROUP);
                feedObj.setNumber(0);
                feedObj.setProperty(Constants.PROPERTY_KEYWORD_NAME, group);
                watch.getXWikiServiceInstance().saveObject(feedObj, new AsyncCallback() {
                    public void onFailure(Throwable throwable) {
                        cb.onFailure(throwable);
                    }
                    public void onSuccess(Object object) {
                        // We return the pageName
                        if (!((Boolean)object).booleanValue())
                            cb.onFailure(null);
                        else
                            cb.onSuccess(pageName);
                    }
                });
            }
        });
    }

    public void removeFeed(Feed feed, final AsyncCallback cb) {
        try {
            if ((feed.getPageName()==null)||(feed.getPageName().equals("")))
                cb.onFailure(null);

            watch.getXWikiServiceInstance().deleteDocument(feed.getPageName(), new XWikiAsyncCallback(watch) {
                public void onFailure(Throwable caught) {
                    super.onFailure(caught);
                    cb.onFailure(caught);
                }

                public void onSuccess(Object result) {
                    super.onSuccess(result);
                    cb.onSuccess(result);
                }
            });
        } catch(Exception e) {
            cb.onFailure(e);
        }
    }

    public void removeGroup(String group, final AsyncCallback cb) {
        try {
            if ((group==null)||(group.equals("")))
                cb.onFailure(null);

            watch.getXWikiServiceInstance().deleteDocument(group, new XWikiAsyncCallback(watch) {
                public void onFailure(Throwable caught) {
                    super.onFailure(caught);
                    cb.onFailure(caught);
                }

                public void onSuccess(Object result) {
                    super.onSuccess(result);
                    cb.onSuccess(result);
                }
            });
        } catch(Exception e) {
            cb.onFailure(e);
        }
    }

    public void removeKeyword(String keyword, final AsyncCallback cb) {
        try {
            if ((keyword==null)||(keyword.equals("")))
                cb.onFailure(null);

            watch.getXWikiServiceInstance().deleteDocument(keyword, new XWikiAsyncCallback(watch) {
                public void onFailure(Throwable caught) {
                    super.onFailure(caught);
                    cb.onFailure(caught);
                }

                public void onSuccess(Object result) {
                    super.onSuccess(result);
                    cb.onSuccess(result);
                }
            });
        } catch(Exception e) {
            cb.onFailure(e);
        }
    }

    public void addComment(FeedArticle article, String text, final AsyncCallback cb) {
        watch.getXWikiServiceInstance().addComment(article.getPageName(), text, new XWikiAsyncCallback(watch) {
            public void onFailure(Throwable throwable) {
                super.onFailure(throwable);
                cb.onFailure(throwable);
            }
            public void onSuccess(Object object) {
                super.onSuccess(object);
                cb.onSuccess(object);
            }
        });
    }

    public void getFeedList(final XWikiAsyncCallback cb) {
        watch.getXWikiServiceInstance().getDocuments(", BaseObject as obj where doc.fullName=obj.name and obj.className in ('XWiki.AggregatorURLClass','XWiki.AggregatorGroupClass', 'XWiki.KeywordClass') and doc.web='" + watch.getWatchSpace() + "'",
                0, 0, true, true, false, cb);
    }


    public void getArticles(FilterStatus filterStatus, int nb, int start, final AsyncCallback cb) {
        try {
            String sql = prepareSQLQuery(filterStatus);
            watch.getXWikiServiceInstance().getDocuments(sql, nb, start, true, true, false, cb);
        } catch(Exception e) {
            cb.onFailure(e);
        }
    }

    private String prepareSQLQuery(FilterStatus filterStatus) {
        String skeyword = (filterStatus.getKeyword() ==null) ? null : filterStatus.getKeyword().replaceAll("'", "''");
        String sql = ", BaseObject as obj, XWiki.FeedEntryClass as feedentry ";
        String wheresql = "where doc.fullName=obj.name and obj.className='XWiki.FeedEntryClass' and obj.id=feedentry.id ";

        if ((filterStatus.getTags() !=null)&&(filterStatus.getTags().size()>0)) {
            for(int i=0;i< filterStatus.getTags().size();i++) {
                String tag = (String) filterStatus.getTags().get(i);
                wheresql += " and '" + tag.replaceAll("'","''") + "' in elements(feedentry.tags) ";
            }
        }

        if ((filterStatus.getKeyword() !=null)&&(!filterStatus.getKeyword().trim().equals(""))) {
            wheresql  += " and (feedentry.title like '%" + skeyword + "%' "
                    + " or feedentry.content like '%" + skeyword + "%' "
                    + " or feedentry.fullContent like '%" + skeyword + "%') ";
        }

        if (filterStatus.getFlagged() ==1) {
            wheresql += " and feedentry.flag=1";
        } else if ((filterStatus.getFlagged() ==-1)&&(filterStatus.getTrashed() ==-1)) {
            wheresql += " and (feedentry.flag=0 or feedentry.flag is null)";
        } else if (filterStatus.getTrashed() ==1) {
            wheresql += " and feedentry.flag=-1";
        } else if (filterStatus.getTrashed() ==-1) {
            wheresql += " and (feedentry.flag>-1 or feedentry.flag is null)";
        } else if (filterStatus.getFlagged() ==-1) {
            wheresql += " and (feedentry.flag<1 or feedentry.flag is null)";
        }

        Feed feed = filterStatus.getFeed();
        String feedurl = (feed==null) ? null : feed.getUrl();
        if ((feedurl !=null)&&(!feedurl.trim().equals(""))) {
            wheresql += " and feedentry.feedurl='" + feedurl.replaceAll("'","''") + "'";
        } else if ((filterStatus.getGroup() !=null)&&(!filterStatus.getGroup().trim().equals(""))) {
            wheresql += " and feedentry.feedurl in ("
                    + "select feed.url from XWiki.AggregatorURLClass as feed where '" + filterStatus.getGroup().replaceAll("'","''") + "' in elements(feed.group))";
        }

        if (filterStatus.getDate() !=null) {
            wheresql += " and feedentry.date >= '" + filterStatus.getDate() + "' ";
        } else {
            if ("1".equals(watch.getParam("withdatelimit"))) {
                Date date = new Date();
                date = new Date(date.getTime() - 3 * 24 * 60 * 60 * 1000);
                SimpleDateFormat format = new SimpleDateFormat("yyyy/MM/dd HH:mm");
                String sdate = format.format(date);
                wheresql += " and feedentry.date >= '" + sdate + "' ";
            }
        }

        if (filterStatus.getRead() ==1) {
            wheresql += " and feedentry.read=1";
        } else if (filterStatus.getRead() ==-1) {
            wheresql += " and (feedentry.read is null or feedentry.read=0)";
        }

        sql += wheresql + " and doc.web='" + watch.getWatchSpace() + "' order by feedentry.date desc";
        return sql;
    }

    /**
     * Retrieves one article from the server
     * @param pageName
     * @param cb
     * @return
     */
    public void getArticle(String pageName, final AsyncCallback cb) {
        watch.getXWikiServiceInstance().getDocument(pageName, true, true, false, new XWikiAsyncCallback(watch) {
            public void onFailure(Throwable caught) {
                super.onFailure(caught);
                cb.onFailure(caught);
            }

            public void onSuccess(Object result) {
                super.onSuccess(result);
                // We encapsulate the result in a FeedArticle object
                FeedArticle article = new FeedArticle((Document) result);
                cb.onSuccess(article);
            }
        });
    }

    public void updateTags(FeedArticle article, String tags, AsyncCallback cb) {
        List taglist = new ArrayList();
        tags = (tags==null) ? "" : tags;
        String[] tagarray = tags.split(" ");
        for (int i=0;i<tagarray.length;i++)
            taglist.add(tagarray[i]);
        watch.getXWikiServiceInstance().updateProperty(article.getPageName(), "XWiki.FeedEntryClass", "tags", taglist, cb);
    }

    public void getTagsList(AsyncCallback cb) {
        Map params = new HashMap();
        params.put("space", watch.getWatchSpace());
        watch.getXWikiServiceInstance().customQuery(Constants.DEFAULT_QUERIES_SPACE + "." + Constants.QUERY_PAGE_TAGSLIST, params, 0, 0, cb);
    }

    public void getNewArticles(AsyncCallback cb) {
        Map params = new HashMap();
        params.put("space", watch.getWatchSpace());
        watch.getXWikiServiceInstance().customQuery(Constants.DEFAULT_QUERIES_SPACE + "." + Constants.QUERY_PAGE_NEWARTICLES, params, 0, 0, cb);
    }

    public void getArticleCount(AsyncCallback cb) {
        Map params = new HashMap();
        params.put("space", watch.getWatchSpace());
        watch.getXWikiServiceInstance().customQuery(Constants.DEFAULT_QUERIES_SPACE + "." + Constants.QUERY_PAGE_ARTICLENUMBER, params, 0, 0, cb);
    }

    public void updateArticleFlagStatus(FeedArticle article, int newflagstatus, XWikiAsyncCallback cb) {
        watch.getXWikiServiceInstance().updateProperty(article.getPageName(), "XWiki.FeedEntryClass", "flag", newflagstatus, cb);
    }

    public void updateArticleReadStatus(FeedArticle article, AsyncCallback cb) {
        watch.getXWikiServiceInstance().updateProperty(article.getPageName(), "XWiki.FeedEntryClass", "read", 1, cb);
    }

    public void getAnalysisHTML(FilterStatus filterStatus, AsyncCallback cb) {
        Map map = filterStatus.getMap();
        map.put("space", watch.getWatchSpace());
        watch.getXWikiServiceInstance().getDocumentContent(Constants.DEFAULT_CODE_SPACE + "." + Constants.PAGE_TAGCLOUD, true, map, cb);
    }

    public void getPressReview(FilterStatus filterStatus, String pressReviewPage, AsyncCallback cb) {
        Map map = filterStatus.getMap();
        map.put("space", watch.getWatchSpace());
        watch.getXWikiServiceInstance().getDocumentContent(pressReviewPage, true, map, cb);
    }

}
