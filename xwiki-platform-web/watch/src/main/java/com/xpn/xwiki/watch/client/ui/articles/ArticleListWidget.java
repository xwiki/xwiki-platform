package com.xpn.xwiki.watch.client.ui.articles;

import com.xpn.xwiki.watch.client.ui.WatchWidget;
import com.xpn.xwiki.watch.client.ui.menu.ActionBarWidget;
import com.xpn.xwiki.watch.client.ui.menu.NavigationBarWidget;
import com.xpn.xwiki.watch.client.ui.dialog.CommentAddDialog;
import com.xpn.xwiki.watch.client.ui.dialog.EditTagsDialog;
import com.xpn.xwiki.watch.client.Watch;
import com.xpn.xwiki.watch.client.FilterStatus;
import com.xpn.xwiki.watch.client.Constants;
import com.xpn.xwiki.watch.client.Feed;
import com.xpn.xwiki.watch.client.data.FeedArticle;
import com.xpn.xwiki.watch.client.data.FeedArticleComment;
import com.xpn.xwiki.gwt.api.client.app.XWikiAsyncCallback;
import com.xpn.xwiki.gwt.api.client.Document;
import com.xpn.xwiki.gwt.api.client.dialog.Dialog;
import com.google.gwt.user.client.ui.*;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;

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

public class ArticleListWidget extends WatchWidget {

    public ArticleListWidget() {
        super();
    }

    public String getName() {
        return "articlelist";
    }

    public ArticleListWidget(Watch watch) {
        super(watch);
        setPanel(new FlowPanel());
        initWidget(panel);
        init();
    }

    public void init() {
        super.init();
    }

    public void refreshData() {
        FilterStatus fstatus = watch.getFilterStatus();
        watch.getDataManager().getArticles(fstatus, watch.getArticleNbParam(), fstatus.getStart(), new XWikiAsyncCallback(watch) {
            public void onFailure(Throwable caught) {
                super.onFailure(caught);
            }

            public void onSuccess(Object result) {
                super.onSuccess(result);
                // Load article list
                List list = (List) result;
                showArticles(list);
                resizeWindow();
            }
        });
    }


    public void showArticles(List feedentries) {
        panel.clear();

        if ((feedentries==null)||(feedentries.size()==0)) {
            panel.add(new HTML(watch.getTranslation("articlelist_noarticles")));
            return;
        }

        panel.add(new ActionBarWidget(watch));
        panel.add(new NavigationBarWidget(watch));
        
        for (int i=0;i<feedentries.size();i++) {
            Document article = (Document) feedentries.get(i);
            showArticle(new FeedArticle(article));
        }
    }

    protected Widget getTitlePanel(FeedArticle article, Widget articlePanel, Widget contentZonePanel) {
        FlowPanel p = new FlowPanel();
        p.setStyleName(watch.getStyleName("article", "titlebar"));
        p.add(getLogoPanel(article));
        p.add(getTitleLinePanel(article, articlePanel, contentZonePanel));
        p.add(getActionsPanel(article));
        p.add(getFeedNamePanel(article));
        p.add(getDatePanel(article));
        p.add(getCommentsNbPanel(article));
        return p;
    }

    protected Widget getLogoPanel(FeedArticle article) {
        FlowPanel p = new FlowPanel();
        p.setStyleName(watch.getStyleName("article", "logo"));

        String feedName = article.getFeedName();
        if ((feedName!=null)&&(!feedName.equals(""))) {
            Feed feed = (Feed) watch.getConfig().getFeedsList().get(feedName);
            if (feed!=null) {
                String imgurl = watch.getFavIcon(feed);
                if (imgurl!=null) {
                    String imghtml = "<img src=\"" + imgurl + "\" class=\"" + watch.getStyleName("article", "logo-icon") + "\" alt=\"\" />";
                    p.add(new HTML(imghtml));
                }
            }
        }
        return p;
    }

    protected Panel getTitleLinePanel(final FeedArticle article, final Widget articlePanel, final Widget contentZonePanel) {
        FlowPanel p = new FlowPanel();
        p.setStyleName(watch.getStyleName("article", "titleline"));

        // Title text
        Hyperlink titlelink = new Hyperlink(article.getTitle(), "");
        p.add(titlelink);

        titlelink.addClickListener(new ClickListener() {
            public void onClick(Widget widget) {
                contentZonePanel.setVisible(!contentZonePanel.isVisible());
                resizeWindow();
                watch.getDataManager().updateArticleReadStatus(article, new AsyncCallback() {
                    public void onFailure(Throwable throwable) {
                    }
                    public void onSuccess(Object object) {
                        articlePanel.removeStyleName(watch.getStyleName("article", "unread"));
                        articlePanel.addStyleName(watch.getStyleName("article", "read"));
                    }
                });
            }
        });

        Image extLinkImage = new Image(watch.getSkinFile(Constants.IMAGE_EXT_LINK));
        extLinkImage.setStyleName(watch.getStyleName("article", "title-extlink"));
        extLinkImage.setTitle(watch.getTranslation("open_this_article"));
        extLinkImage.addClickListener(new ClickListener() {
            public void onClick(Widget widget) {
                Window.open(article.getUrl(), "_blank", "");
            }
        });
        p.add(extLinkImage);
        return p;
    }

    protected Widget getFeedNamePanel(FeedArticle article) {
        FlowPanel p = new FlowPanel();
        p.setStyleName(watch.getStyleName("article", "feedname"));
        HTML htmlFeedName = new HTML();
        htmlFeedName.setStyleName(watch.getStyleName("article", "feedname-text"));
        htmlFeedName.setHTML(article.getFeedName());
        p.add(htmlFeedName);
        return p;
    }

    protected Widget getDatePanel(FeedArticle article) {
        FlowPanel p = new FlowPanel();
        p.setStyleName(watch.getStyleName("article", "date"));
        HTML htmlDate = new HTML();
        htmlDate.setStyleName(watch.getStyleName("article", "date-text"));
        htmlDate.setHTML(article.getDate());
        p.add(htmlDate);
        return p;
    }

    protected Widget getActionsPanel(final FeedArticle article) {
        final FlowPanel actionsPanel = new FlowPanel();
        actionsPanel.setStyleName(watch.getStyleName("article", "actions"));
        updateActionsPanel(actionsPanel, article);
        return actionsPanel;
    }

    protected void updateActionsPanel(final FlowPanel actionsPanel, final FeedArticle article) {
        Image flagImage = new Image(watch.getSkinFile((article.getFlagStatus()==1) ? Constants.IMAGE_FLAG_ON : Constants.IMAGE_FLAG_OFF));
        flagImage.setStyleName(watch.getStyleName("article", "action-flag"));
        flagImage.setTitle(watch.getTranslation((article.getFlagStatus()==1) ? "unflag_this_article" : "flag_this_article"));
        flagImage.addClickListener(new ClickListener() {
            public void onClick(Widget widget) {
                    int flagstatus = article.getFlagStatus();
                    final int newflagstatus = (flagstatus==1) ? 0 : 1;
                    watch.getDataManager().updateArticleFlagStatus(article, newflagstatus, new XWikiAsyncCallback(watch) {
                        public void onSuccess(Object result) {
                             super.onSuccess(result);
                             article.setFlagStatus(newflagstatus);
                             actionsPanel.clear();
                             updateActionsPanel(actionsPanel, article);
                        }
                    });
                }
        });
        actionsPanel.add(flagImage);
        Image trashImage = new Image(watch.getSkinFile((article.getFlagStatus()==-1) ? Constants.IMAGE_TRASH_ON : Constants.IMAGE_TRASH_OFF));
        trashImage.setStyleName(watch.getStyleName("article", "action-trash"));
        trashImage.setTitle(watch.getTranslation((article.getFlagStatus()==-1) ? "untrash_this_article" : "trash_this_article"));
        trashImage.addClickListener(new ClickListener() {
            public void onClick(Widget widget) {
                // trash/untrash article
                int flagstatus = article.getFlagStatus();
                if (flagstatus!=-1) {
                 final int newflagstatus = -1;
                    watch.getDataManager().updateArticleFlagStatus(article, newflagstatus, new XWikiAsyncCallback(watch) {
                        public void onSuccess(Object result) {
                            super.onSuccess(result);
                             article.setFlagStatus(newflagstatus);
                             actionsPanel.clear();
                             updateActionsPanel(actionsPanel, article);
                        }
                    });
                }
            }
        });
        actionsPanel.add(trashImage);
    }

    protected Widget getCommentsNbPanel(FeedArticle article) {
        FlowPanel p = new FlowPanel();
        p.setStyleName(watch.getStyleName("article", "commentsnb"));
        return p;
    }

    protected Widget getTagsPanel(final FeedArticle article) {
        final FlowPanel tagsPanel = new FlowPanel();
        tagsPanel.setStyleName(watch.getStyleName("article", "tags"));
        FlowPanel tagsTitlePanel = new FlowPanel();
        tagsTitlePanel.setStyleName(watch.getStyleName("article", "tags-title"));
        Image tagsAddImage = new Image(watch.getSkinFile(Constants.IMAGE_MORE));
        tagsAddImage.setStyleName(watch.getStyleName("article", "tags-add-image"));
        tagsTitlePanel.add(tagsAddImage);
        String tagsTitleText = watch.getTranslation("tags") + ": " + article.getTags();
        final HTML tagsTitle = new HTML(tagsTitleText);
        tagsTitle.setStyleName(watch.getStyleName("article", "tags-text"));
        tagsTitlePanel.add(tagsTitle);
        tagsPanel.add(tagsTitlePanel);
        tagsAddImage.addClickListener(new ClickListener() {
            public void onClick(Widget widget) {
                // Here we launch the tags add dialog
                final EditTagsDialog tagsEditDialog = new EditTagsDialog(watch, "tagsadd", Dialog.BUTTON_CANCEL | Dialog.BUTTON_NEXT, article.getTags());
                tagsEditDialog.setNextText("finish");
                tagsEditDialog.setNextCallback(new AsyncCallback() {
                    public void onFailure(Throwable throwable) {
                        // cancel we just close the dialog
                    }

                    public void onSuccess(Object object) {
                        // next we send the tags to the server
                        final String tags = (String) object;
                        // sending the tags to the server
                        watch.getDataManager().updateTags(article, tags, new XWikiAsyncCallback(watch) {
                            public void onFailure(Throwable caught) {
                                // failure we show the exception
                                super.onFailure(caught);
                            }

                            public void onSuccess(Object result) {
                                super.onSuccess(result);
                                // sucesss - We need to refreshData the number of tags
                                article.setTags(tags);
                                String tagsTitleText = watch.getTranslation("tags") + ": " + article.getTags();
                                tagsTitle.setHTML(tagsTitleText);
                                watch.refreshTagCloud();                           
                            }
                        });
                    }
                });
                // Show the dialog
                tagsEditDialog.show();
            }
        });
        return tagsPanel;
    }

    protected Widget getContentPanel(FeedArticle article) {
        FlowPanel p = new FlowPanel();
        p.setStyleName(watch.getStyleName("article", "content"));
        p.add(new HTML(article.getContent()));
        return p;
    }

    protected Widget getCommentsPanel(FeedArticle article) {
        FlowPanel p = new FlowPanel();
        p.setStyleName(watch.getStyleName("article", "comments"));
        addCommentsPanel(p, article);
        return p;
    }

    protected void addCommentsPanel(FlowPanel commentPanel, final FeedArticle article) {
        commentPanel.clear();
        FlowPanel commentTitlePanel = new FlowPanel();
        commentTitlePanel.setStyleName(watch.getStyleName("article", "comments-title"));
        Image commentAddImage = new Image(watch.getSkinFile(Constants.IMAGE_MORE));
        commentAddImage.setStyleName(watch.getStyleName("article", "comments-add-image"));
        commentTitlePanel.add(commentAddImage);
        int nbcomments =  article.getCommentsNumber();
        HTML commentTitle = new HTML(watch.getTranslation("comments") + " (" + nbcomments + ")");
        commentTitle.setStyleName(watch.getStyleName("article", "comments-title-text"));
        commentTitlePanel.add(commentTitle);
        commentPanel.add(commentTitlePanel);
        List comments = article.getComments();
        if ((comments!=null)&&(comments.size()>0)) {
            FlowPanel commentsZonePanel = new FlowPanel();
            commentsZonePanel.setStyleName(watch.getStyleName("article", "comments-content"));
            for(int i=0;i<comments.size();i++) {
                FeedArticleComment comment = (FeedArticleComment) comments.get(i);
                commentsZonePanel.add(getCommentPanel(comment));
            }
            commentPanel.add(commentsZonePanel);
        }
        final FlowPanel commentPanel2 = commentPanel;
        commentAddImage.addClickListener(new ClickListener() {
            public void onClick(Widget widget) {
                // Here we launch the comment add dialog
                final CommentAddDialog commentAddDialog = new CommentAddDialog(watch, "commentadd", Dialog.BUTTON_CANCEL | Dialog.BUTTON_NEXT);
                commentAddDialog.setNextText("finish");
                commentAddDialog.setNextCallback(new AsyncCallback() {
                    public void onFailure(Throwable throwable) {
                        // cancel we just close the dialog
                        commentAddDialog.hide();
                    }

                    public void onSuccess(Object object) {
                        // next we send the comment to the server
                        String comment = (String) object;
                        // we need to hide the dialog first
                        commentAddDialog.hide();
                        // sending the comment to the server
                        watch.getDataManager().addComment(article, comment, new XWikiAsyncCallback(watch) {
                            public void onFailure(Throwable caught) {
                                // failure we show the exception
                                super.onFailure(caught);
                            }

                            public void onSuccess(Object result) {
                                super.onSuccess(result);
                                // sucesss - We need to refreshData the number of comments
                                // we reread the article to make sure we get it right
                                watch.getDataManager().getArticle(article.getPageName(), new XWikiAsyncCallback(watch) {
                                    public void onFailure(Throwable caught) {
                                        super.onFailure(caught);
                                    }

                                    public void onSuccess(Object result) {
                                        super.onSuccess(result);
                                        // Refresh the comment panel
                                        addCommentsPanel(commentPanel2, (FeedArticle) result);
                                        // We need to resize in case this brings up a scroll bar
                                        resizeWindow();
                                    }
                                });
                            }
                        });
                    }
                });
                // Show the dialog
                commentAddDialog.show();
            }
        });
    }

    protected Widget getCommentPanel(FeedArticleComment comment) {
        FlowPanel p = new FlowPanel();
        p.setStyleName(watch.getStyleName("article", "comment"));
        HTML author = new HTML(comment.getAuthor());
        author.setStyleName(watch.getStyleName("article", "comment-author"));
        p.add(author);
        HTML date = new HTML(getDateHTML(comment.getDate()));
        date.setStyleName(watch.getStyleName("article", "comment-date"));
        p.add(date);
        HTML commentHTML = new HTML(comment.getContent());
        commentHTML.setStyleName(watch.getStyleName("article", "comment-content"));
        p.add(commentHTML);
        return p;
    }

    private String getDateHTML(String date) {
        return watch.getTranslation("on") + " " + date + ": ";
    }

    protected Widget getContentZonePanel(FeedArticle article) {
        FlowPanel p = new FlowPanel();
        p.setStyleName(watch.getStyleName("article", "contentzone"));
        p.add(getTagsPanel(article));
        p.add(getContentPanel(article));
        p.add(getCommentsPanel(article));
        // This panel is hidden by default
        p.setVisible(false);
        return p;
    }

    public void showArticle(FeedArticle article) {
        FlowPanel articlepanel = new FlowPanel();
        articlepanel.setStyleName(watch.getStyleName("article"));
        if (article.getReadStatus()==1)
            articlepanel.addStyleName(watch.getStyleName("article", "read"));
        else
            articlepanel.addStyleName(watch.getStyleName("article", "unread"));
        Widget contentZonePanel = getContentZonePanel(article);
        articlepanel.add(getTitlePanel(article, articlepanel, contentZonePanel));
        articlepanel.add(contentZonePanel);
        panel.add(articlepanel);
    }

    public void resizeWindow() {
        int windowWidth = watch.getUserInterface().getOffsetWidth();
        int feedTreeWidth = watch.getUserInterface().getFeedTreeWidth();
        int filterBarWidth = watch.getUserInterface().getFilterBarWidth();
        int newWidth = windowWidth - feedTreeWidth - filterBarWidth;
        if (newWidth < 0)
         newWidth = 0;
        setWidth(newWidth + "px");
    }
}
