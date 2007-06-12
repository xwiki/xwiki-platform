package com.xpn.xwiki.watch.client;

import com.xpn.xwiki.gwt.api.client.app.XWikiGWTDefaultApp;
import com.xpn.xwiki.gwt.api.client.app.XWikiAsyncCallback;
import com.xpn.xwiki.gwt.api.client.app.XWikiGWTAppConstants;
import com.xpn.xwiki.watch.client.ui.UserInterface;
import com.xpn.xwiki.watch.client.ui.dialog.AnalysisDialog;
import com.xpn.xwiki.watch.client.ui.dialog.WatchDialog;
import com.xpn.xwiki.watch.client.ui.dialog.PressReviewDialog;
import com.xpn.xwiki.watch.client.ui.wizard.ConfigWizard;
import com.xpn.xwiki.watch.client.data.DataManager;
import com.xpn.xwiki.watch.client.data.Keyword;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;

import java.util.Date;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


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

public class Watch extends XWikiGWTDefaultApp implements EntryPoint {
    protected Config config;
    protected UserInterface userInterface;
    protected DataManager dataManager;
    private FilterStatus filterStatus = new FilterStatus();
    protected  NewArticlesMonitoring newArticlesMonitoring;
    protected String watchSpace = null;
    
    public Watch() {
    }

    /**
     * Allows to access the name of the translations page provided in gwt parameters
     * @return
     */
    public String getTranslationPage() {
        return getParam("translations", Constants.DEFAULT_TRANSLATIONS_PAGE);
    }

    public String getLocale() {
        return getParam("locale", Constants.DEFAULT_LOCALE);
    }
    
    public String getCSSPrefix() {
        return Constants.CSS_PREFIX;
    }

    public String getStyleName(String cssname) {
        return getCSSPrefix() + "-" + cssname;
    }

    public String getStyleName(String module, String cssname) {
        return getCSSPrefix() + "-" + module + "-" + cssname;
    }

    public Config getConfig() {
        return config;
    }

    public DataManager getDataManager() {
        return dataManager;
    }

    public UserInterface getUserInterface() {
        return userInterface;
    }

    public void startServerLoading() {
        Map map = filterStatus.getMap();
        map.put("space", getWatchSpace());
        map.put("confirm", "1");
        getXWikiServiceInstance().getDocumentContent(Constants.PAGE_LOADING_STATUS, true, map, new AsyncCallback() {
            public void onFailure(Throwable throwable) {
            }
            public void onSuccess(Object object) {
            }
        });
    }

    /**
     * Current watch space
     * @return
     */

    public String getWatchSpace() {
        return (watchSpace==null) ? getParam("watchspace", Constants.DEFAULT_WATCH_SPACE) : watchSpace;
    }

    public void setWatchSpace(String watchSpace) {
        this.watchSpace = watchSpace;
    }

    public String getSkinFile(String file) {
        String name = "watch-" + file;
        if ("1".equals(getParam("useskin")))
         return super.getSkinFile(name);
        else {
            String imagePath = getParam("resourcepath", "");
            if (imagePath.equals(""))
             return name;
            else
             return imagePath + "/" + name;
        }
    }

    public FilterStatus getFilterStatus() {
        return filterStatus;
    }

    public void setFilterStatus(FilterStatus filterStatus) {
        this.filterStatus = filterStatus;
    }

    public void onModuleLoad() {
        if (!GWT.isScript()) {
            getXWikiServiceInstance().login("Admin", "admin", true, new XWikiAsyncCallback(this) {
                public void onFailure(Throwable caught) {
                    super.onFailure(caught);
                }
                public void onSuccess(Object result) {
                    super.onSuccess(result);    
                    onModuleLoad(false);
                }
            });
        } else {
            onModuleLoad(false);
        }
    }

    
    public void onModuleLoad(boolean translationDone) {
        if (!translationDone) {
            checkTranslator(new XWikiAsyncCallback(this) {
                public void onFailure(Throwable caught) {
                    super.onFailure(caught);
                    onModuleLoad(true);
                }

                public void onSuccess(Object result) {
                    super.onSuccess(result);
                    onModuleLoad(true);
                }
            });
            return;
        }
        // Launch monitoring of new incoming article which is updating the title bar
        newArticlesMonitoring = new NewArticlesMonitoring(this);

        config = new Config(this);
        userInterface = new UserInterface(this);
        dataManager = new DataManager(this);
        
        // Launch the UI
        RootPanel.get("Watch").add(userInterface);

        // Load the feed list and other info
        config.refreshConfig(new XWikiAsyncCallback(this) {
            public void onSuccess(Object object) {
                super.onSuccess(object);
                // Refresh the Feed Tree UI
                userInterface.init();
                userInterface.refreshData("feedtree");
                // Load the number of articles for each feed
                config.refreshArticleNumber();
                // Refresh tag cloud aynchronously
                refreshTagCloud();
                refreshKeywords();
                refreshArticleList();
                // Make sure server has started loading feeds
                startServerLoading();
                userInterface.resizeWindow();
            }
        });
    }

    private void refreshKeywords() {
        userInterface.refreshData("keywords");
    }

    public void refreshTagCloud() {
        userInterface.refreshData("tagcloud");
    }

    public void refreshOnTagActivated(String tagName) {
        FilterStatus fstatus = getFilterStatus();
        List tags = filterStatus.getTags();
        if (tags.contains(tagName))
         tags.remove(tagName);
        else
         tags.add(tagName);
        getFilterStatus().setStart(0);
        refreshArticleList();
        userInterface.resetSelections("tagcloud");
    }

    
    public String getTitleBarText() {
        int nbArticles = newArticlesMonitoring.getArticlesNumber();
        Date lastChange =  newArticlesMonitoring.lastChangeDate();

        String[] args = new String[2];
        args[0] = "" + nbArticles;
        args[1] = (lastChange==null) ? "" : lastChange.toString();
        return getTranslation("title", args);
    }

    /**
     * Refresh the Article List
     */
    public void refreshArticleList() {
        userInterface.refreshData("articlelist");
        newArticlesMonitoring.stopBlinking();
    }

    public void refreshConfig() {
        config.refreshConfig(new XWikiAsyncCallback(this) {
            public void onSuccess(Object object) {
                super.onSuccess(object);
                userInterface.refreshData("feedtree");
                // Load the number of articles for each feed
                config.refreshArticleNumber();
                // Refresh tag cloud aynchronously
            }
        });        
    }

    public void refreshOnSearch(String text) {
        FilterStatus fstatus = getFilterStatus();
        fstatus.setKeyword(text);
        refreshArticleList();
    }

    /**
     * A feed has been clicked. We need to:
     *  - invalidate the group setting
     *  - invalidate the start number
     *  - set the Feed that has been clicked
     *  - refreshData the article list
     *  - refreshData UI elements visuals
     * @param feed to activate
     */
    public void refreshOnFeedChange(Feed feed) {
        getFilterStatus().setFeed(feed);
        getFilterStatus().setGroup(null);
        getFilterStatus().setStart(0);
        refreshArticleList();
    }

    /**
     * A group has been clicked. We need to:
     *  - invalidate the feed setting
     *  - invalidate the start number
     *  - set the Group that has been clicked
     *  - refreshData the article list
     *  - refreshData UI elements visuals
     * @param groupName group to activate
     */
    public void refreshOnGroupChange(String groupName) {
        getFilterStatus().setFeed(null);
        if (groupName.equals(getTranslation("all")))
            getFilterStatus().setGroup(null);
        else
            getFilterStatus().setGroup(groupName);
        getFilterStatus().setStart(0);
        refreshArticleList();
    }

    /**
     * Previous has been clicked. We need to:
     *  - add the number of articles to the "start" filter setting
     *  - refreshData the article list
     *  - refreshData UI elements visuals
     */
    public void refreshOnPrevious() {
        int currentStart = getFilterStatus().getStart();
        int start =  currentStart - getArticleNbParam();
        start = (start<0) ? 0 : start;
        if (currentStart!=start) {
            getFilterStatus().setStart(start);
            refreshArticleList();
        }
    }

    /**
     * Next has been clicked. We need to:
     *  - substract the number of articles to the "start" filter setting
     *  - refreshData the article list
     *  - refreshData UI elements visuals
     */
    public void refreshOnNext() {
        getFilterStatus().setStart(getFilterStatus().getStart() + getArticleNbParam());
        refreshArticleList();
    }

    public void refreshOnHideReadArticles() {
        FilterStatus fstatus = getFilterStatus();
        fstatus.setRead(-1);
        refreshArticleList();
    }

    public void refreshOnShowReadArticles() {
        FilterStatus fstatus = getFilterStatus();
        fstatus.setRead(0);
        refreshArticleList();
    }

    public void refreshOnNewFeed() {
        refreshConfig();        
    }

    public void refreshOnNewKeyword() {
        refreshConfig();
    }

    public void refreshOnNewGroup() {
        refreshConfig();
    }

    public void refreshOnResetFilter() {
        FilterStatus fstatus = getFilterStatus();
        fstatus.setFlagged(0);
        fstatus.setKeyword(null);
        fstatus.setRead(0);
        fstatus.setStart(0);
        fstatus.setTags(new ArrayList());
        fstatus.setTrashed(-1);
        refreshArticleList();
        userInterface.resetSelections();        
    }

    public void refreshOnActivateKeyword(Keyword keyword) {
        FilterStatus fstatus = getFilterStatus();
        fstatus.setKeyword(keyword.getName());
        fstatus.setGroup(keyword.getGroup());
        fstatus.setStart(0);
        refreshArticleList();
        userInterface.resetSelections("keywords");                        
    }

    public String getFavIcon(Feed feed) {
        if (getParam("feeds_favicon", Constants.DEFAULT_FEEDS_FAVICON).equals("remote")) {
            String url = feed.getUrl();
            int i=url.indexOf("/", 10);
            if (i==-1)
                return null;
            else
                return url.substring(0,i) + "/favicon.ico";
        } else {
            return getDownloadUrl(feed.getPageName(), feed.getName() + ".ico");
        }
    }

    public String getDownloadUrl(String pageName, String filename) {
        return XWikiGWTAppConstants.XWIKI_DEFAULT_BASE_URL + "/" + XWikiGWTAppConstants.XWIKI_DEFAULT_ACTION_PATH
               + "/download/" + pageName.replaceAll("\\.", "/") + "/" + filename;
    }

    public String getViewUrl(String pageName, String querystring) {
        return XWikiGWTAppConstants.XWIKI_DEFAULT_BASE_URL + "/" + XWikiGWTAppConstants.XWIKI_DEFAULT_ACTION_PATH
               + "/view/" + pageName.replaceAll("\\.", "/") +  "?" + querystring;
    }

    public void openPressReviewWizard() {
        // Placeholder for PR
        PressReviewDialog pressReviewDialog = new PressReviewDialog(this, "pressreview", WatchDialog.BUTTON_CANCEL | WatchDialog.BUTTON_NEXT, Constants.DEFAULT_CODE_SPACE + "." + Constants.PAGE_PRESSREVIEW);
        pressReviewDialog.setNextText("sendpressreview");
        pressReviewDialog.show();
    }

    public void openAnalysisWizard() {
        // Placeholder for Analysis
        AnalysisDialog analysisDialog = new AnalysisDialog(this, "analysis", WatchDialog.BUTTON_CANCEL);
        analysisDialog.show();
    }

    public void launchConfig(String tabName) {
        if (tabName.equals("feeds")) {
            // Launch the add feed wizard
            ConfigWizard addfeedwizard = new ConfigWizard(this, new AsyncCallback() {
                public void onFailure(Throwable throwable) {
                        Window.alert("failed: " + throwable.getMessage());
                }

                public void onSuccess(Object object) {
                }
            });
            addfeedwizard.launchWizard();
        }
    }

    public void addFeed(final Feed feed, final AsyncCallback cb) {
        getDataManager().addFeed(feed, new XWikiAsyncCallback(this) {
            public void onFailure(Throwable caught) {
                super.onFailure(caught);
                cb.onFailure(caught);
            }

            public void onSuccess(Object result) {
                super.onSuccess(result);
                // getConfig().addFeed((String) result, feed);
                refreshOnNewFeed();
                cb.onSuccess(result);
            }
        });
    }

    public void addKeyword(final String keyword, final String group, final AsyncCallback cb) {
        getDataManager().addKeyword(keyword, group, new XWikiAsyncCallback(this) {
            public void onFailure(Throwable caught) {
                super.onFailure(caught);
                cb.onFailure(caught);
            }

            public void onSuccess(Object result) {
                super.onSuccess(result);
                // getConfig().addKeyword((String) result, keyword, group);
                refreshOnNewKeyword();
                cb.onSuccess(result);
            }
        });
    }

    public void addGroup(final String group, final AsyncCallback cb) {
        getDataManager().addGroup(group, new XWikiAsyncCallback(this) {
            public void onFailure(Throwable caught) {
                super.onFailure(caught);
                cb.onFailure(caught);
            }

            public void onSuccess(Object result) {
                super.onSuccess(result);
                // getConfig().addGroup((String) result, group);
                refreshOnNewGroup();
                cb.onSuccess(result);
            }
        });
    }

    public int getArticleNbParam() {
        return getParamAsInt("nb_articles_per_page", Constants.DEFAULT_PARAM_NB_ARTICLES_PER_PAGE);
    }

    public void refreshOnShowOnlyFlaggedArticles() {
        FilterStatus fstatus = getFilterStatus();
        fstatus.setFlagged(1);
        fstatus.setStart(0);
        refreshArticleList();
    }

    public void refreshOnNotShowOnlyFlaggedArticles() {
        FilterStatus fstatus = getFilterStatus();
        fstatus.setFlagged(0);
        fstatus.setStart(0);
        refreshArticleList();
    }

    public void refreshOnShowOnlyReadArticles() {
        FilterStatus fstatus = getFilterStatus();
        fstatus.setRead(1);
        fstatus.setStart(0);
        refreshArticleList();
    }

    public void refreshOnNotShowOnlyReadArticles() {
        FilterStatus fstatus = getFilterStatus();
        fstatus.setRead(0);
        fstatus.setStart(0);
        refreshArticleList();
    }

    public void refreshOnShowOnlyUnReadArticles() {
        FilterStatus fstatus = getFilterStatus();
        fstatus.setRead(-1);
        fstatus.setStart(0);
        refreshArticleList();
    }

    public void refreshOnNotShowOnlyUnReadArticles() {
        FilterStatus fstatus = getFilterStatus();
        fstatus.setRead(0);
        fstatus.setStart(0);
        refreshArticleList();
    }

    public void refreshOnShowOnlyTrashedArticles() {
        FilterStatus fstatus = getFilterStatus();
        fstatus.setTrashed(1);
        fstatus.setStart(0);
        refreshArticleList();
    }

    public void refreshOnNotShowOnlyTrashedArticles() {
        FilterStatus fstatus = getFilterStatus();
        fstatus.setTrashed(-1);
        fstatus.setStart(0);
        refreshArticleList();
    }
}
