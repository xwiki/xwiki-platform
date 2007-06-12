package com.xpn.xwiki.watch.client.ui.menu;

import com.xpn.xwiki.watch.client.ui.WatchWidget;
import com.xpn.xwiki.watch.client.ui.dialog.StandardFeedDialog;
import com.xpn.xwiki.watch.client.ui.dialog.WatchDialog;
import com.xpn.xwiki.watch.client.ui.dialog.ChoiceDialog;
import com.xpn.xwiki.watch.client.ui.dialog.ChoiceInfo;
import com.xpn.xwiki.watch.client.Watch;
import com.xpn.xwiki.watch.client.Feed;
import com.xpn.xwiki.watch.client.Constants;
import com.xpn.xwiki.gwt.api.client.app.XWikiAsyncCallback;
import com.google.gwt.user.client.ui.*;
import com.google.gwt.user.client.*;
import com.google.gwt.user.client.Timer;
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

public class FeedTreeWidget  extends WatchWidget {
    private Tree groupTree = new Tree();
    public FeedTreeWidget() {
        super();
    }

    public String getName() {
        return "feedtree";
    }
    
    public FeedTreeWidget(Watch watch) {
        super(watch);
        setPanel(new FlowPanel());
        initWidget(panel);
        init();
    }

    public void init() {
        super.init();
        HTML titleHTML = new HTML(watch.getTranslation("feedtree.title"));
        titleHTML.setStyleName(watch.getStyleName("feedtree", "title"));
        titleHTML.addClickListener(new ClickListener() {
            public void onClick(Widget widget) {
                watch.launchConfig("feeds");
            }
        });
        panel.add(titleHTML);

        Image configImage = new Image(watch.getSkinFile(Constants.IMAGE_CONFIG));
        configImage.setStyleName(watch.getStyleName("feedtree", "image"));
        configImage.setTitle(watch.getTranslation("config"));
        configImage.addClickListener(new ClickListener() {
            public void onClick(Widget widget) {
                watch.launchConfig("feeds");
            }
        });
        panel.add(configImage);
        groupTree.setStyleName(watch.getStyleName("feedtree","groups"));
        panel.add(groupTree);
    }

    public void refreshData() {
        // we need to make sure the feed tree has been prepared
        makeFeedTree();
    }

    private void  makeFeedTree() {
        // clear all trees
        groupTree.clear();

        Map feeds = watch.getConfig().getFeedsList();
        Map feedsbygroup = watch.getConfig().getFeedsByGroupList();
        Map groups = watch.getConfig().getGroups();

        List keys = new ArrayList(feedsbygroup.keySet());
        Collections.sort(keys);
        Iterator groupit = keys.iterator();
        while (groupit.hasNext()) {
            final String groupname = (String) groupit.next();
            String groupTitle = (String) groups.get(groupname);
            if (groupTitle==null)
             groupTitle = groupname;
            if ((groupname!=null)&&(!groupname.trim().equals(""))) {
                Map group = (Map) feedsbygroup.get(groupname);
                TreeItem groupItemTree = new TreeItem();
                Hyperlink link = new Hyperlink(groupTitle, "");
                link.setStyleName(watch.getStyleName("feedtree","link"));
                link.addClickListener(new ClickListener() {
                    public void onClick(Widget widget) {
                        watch.refreshOnGroupChange(groupname);
                    }
                });
                groupItemTree.setWidget(link);
                groupTree.addItem(groupItemTree);
                List feedList = new ArrayList(group.keySet());
                Collections.sort(feedList);
                Iterator feedgroupit = feedList.iterator();
                while (feedgroupit.hasNext()) {
                    String feedname = (String) feedgroupit.next();
                    Feed feed = (Feed) group.get(feedname);
                    addFeedToTree(groupItemTree, feed);
                }
                groupTree.addItem(groupItemTree);
            }
        }
    }

    /*
    private void addFeedToTree(Tree treeItem, final Feed feed) {
        String feedtitle =  feed.getName() + "(" + feed.getNb() + ")";
        String imgurl = getFavIcon(feed);
        if (imgurl!=null)
         feedtitle = "<img src=\"" + imgurl + "\" class=\"" + watch.getStyleName("feedtree","rssthumbnail") +"\" alt=\"\" />" + feedtitle;
        Hyperlink link = new Hyperlink(feedtitle, true, "");
        link.setStyleName(watch.getStyleName("feedtree","link"));
        link.addClickListener(new ClickListener() {
            public void onClick(Widget widget) {
                watch.refreshOnFeedChange(feed);
            }
        });
        treeItem.add(link);
    } */

    private String getFavIcon(Feed feed) {
        return watch.getFavIcon(feed);
    }

    private void addFeedToTree(TreeItem treeItem, final Feed feed) {

        String feedtitle =  feed.getName() + "(" + feed.getNb() + ")";
        String imgurl = getFavIcon(feed);     
        if (imgurl!=null)
         feedtitle = "<img src=\"" + imgurl + "\" class=\"" + watch.getStyleName("feedtree","logo-icon") + "\" alt=\"\" />" + feedtitle;
        Hyperlink link = new DoubleClickHyperlink(feed, feedtitle, true, "");
        link.setStyleName(watch.getStyleName("feedtree","link"));
        treeItem.addItem(link);
    }


    public void resizeWindow() {
        // Watch.setMaxHeight(panel);
    }

    /**
     * Class to handle double click
     */
    public class DoubleClickHyperlink extends Hyperlink {
        private Feed feed;
        private int clicksCount = 0;
        private Timer timer;
        StandardFeedDialog feedDialog;
        ChoiceDialog chooseActionDialog;
        boolean actionStarted = false;

        public DoubleClickHyperlink(Feed feed, String text, boolean asHTML, String targetHistoryToken) {
            super(text, asHTML, targetHistoryToken);
            this.feed = feed;
            sinkEvents(Event.ONDBLCLICK);
        }

        public void onBrowserEvent(Event event) {
            if (DOM.eventGetType(event) == Event.ONDBLCLICK) {
                // TODO: launch edit feed here
                if (timer!=null)
                    timer.cancel();
                clicksCount = 0;
                updateFeed();
            } else if (DOM.eventGetType(event) == Event.ONCLICK) {
                clicksCount++;
                if ( clicksCount >= 2 ) {
                    if (timer!=null)
                        timer.cancel();
                    clicksCount = 0;
                    chooseAction();
                } else {
                    if (timer==null)
                        timer = new Timer() {
                            public void run() {
                                clicksCount = 0;
                                // cancel the timer
                                cancel();
                                watch.refreshOnFeedChange(feed);
                            }
                        };
                    // Timer of double click delay
                    timer.scheduleRepeating(200);
                }
            }
        }

        private void chooseAction() {
            if (actionStarted==false) {
                actionStarted = true;
                chooseActionDialog = new ChoiceDialog(watch, "chooseaction", WatchDialog.BUTTON_CANCEL | WatchDialog.BUTTON_NEXT, false,
                        new AsyncCallback() {
                            public void onFailure(Throwable throwable) {
                                actionStarted = false;
                            }

                            public void onSuccess(Object object) {
                                ChoiceInfo choice = (ChoiceInfo) object;
                                if (choice.getName().equals("updatefeed")) {
                                    updateFeed();
                                } else if (choice.getName().equals("removefeed")){
                                    watch.getDataManager().removeFeed(feed, new XWikiAsyncCallback(watch) {
                                        public void onFailure(Throwable caught) {
                                            super.onFailure(caught);
                                            actionStarted = false;
                                        }
                                        public void onSuccess(Object result) {
                                            super.onSuccess(result);
                                            // We need to refreshData the tree
                                            watch.refreshOnNewFeed();
                                            actionStarted = false;
                                        }
                                    });
                                }  else {
                                    actionStarted = false;
                                }
                            }
                        });
            }
            chooseActionDialog.addChoice("updatefeed");
            chooseActionDialog.addChoice("removefeed");
            chooseActionDialog.show();
        }

        private void updateFeed() {
            feedDialog = new StandardFeedDialog(watch, "standardfeed", WatchDialog.BUTTON_CANCEL | WatchDialog.BUTTON_NEXT, feed);
            feedDialog.setAsyncCallback(new AsyncCallback() {
                public void onFailure(Throwable throwable) {
                    actionStarted = false;
                }
                public void onSuccess(Object object) {
                    Feed newfeed = (Feed) object;
                    watch.getDataManager().updateFeed(newfeed, new XWikiAsyncCallback(watch) {
                        public void onFailure(Throwable caught) {
                            super.onFailure(caught);
                            actionStarted = false;
                        }

                        public void onSuccess(Object result) {
                            super.onSuccess(result);
                            // We need to refreshData the tree
                            watch.refreshOnNewFeed();
                            actionStarted = false;
                        }
                    });
                }
            });
            feedDialog.show();
        }
    }
}
