package rssreader.client;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.*;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import api.client.XWikiService;
import api.client.Document;
import api.client.XObject;

import java.util.*;

import org.gwtwidgets.client.util.SimpleDateFormat;

/**
 * Created by IntelliJ IDEA.
 * User: ldubost
 * Date: 1 déc. 2006
 * Time: 22:43:02
 * To change this template use File | Settings | File Templates.
 */
public class RSSReader implements EntryPoint, FormHandler {
    private RootPanel rpanel;
    private HorizontalPanel hpanel = new HorizontalPanel();
    private DockPanel viewpanel = new DockPanel();
    private VerticalPanel filterpanel = new VerticalPanel();
    private FormPanel searchform = new FormPanel();
    private FlowPanel tagspanel = new FlowPanel();
    private Panel menupanel = new VerticalPanel();
    private TextBox keywordTextBox = new TextBox();
    private TextBox dateTextBox = new TextBox();
    private DialogBox dialog = new DialogBox();
    private PopupPanel popupMessage = new PopupPanel();
    private PopupPanel commentPopup = new PopupPanel(false);
    private PopupPanel tagsPopup = new PopupPanel(false);
    private Tree feedTree = new Tree();
    private Tree groupTree = new Tree();
    private Tree keywordTree = new Tree();
    private Map feeds;
    private Map groups;
    private Map keywords;
    private static RSSReaderConstants constants = (RSSReaderConstants) GWT.create(RSSReaderConstants.class);
    private int clickCounter = 0;
    private static final int NB_ARTICLES = 10;

    // Filters
    private FilterStatus filterStatus = new FilterStatus();
    private Hyperlink flagFilterLink;
    private Hyperlink trashFilterLink;
    private String webappName = "xwiki";
    private String page = "Feeds.WebHome";

    public static RSSReaderConstants getConstants() {
        return constants;
    }

    public void onModuleLoad() {
        rpanel = RootPanel.get(constants.divName());
        rpanel.add(hpanel);
        hpanel.add(menupanel);
        hpanel.add(viewpanel);
        viewpanel.add(filterpanel, DockPanel.NORTH);
        int width = rpanel.getOffsetWidth() - menupanel.getOffsetWidth();
        viewpanel.setWidth(width + "px");
        // Root Panel
        rpanel.setStyleName("rssaggr");
        hpanel.setStyleName("rssaggr");

        // View Panel
        // View Panel
        viewpanel.setStyleName("articles");

        // Filter Panel
        prepareFilterPanel();


        // Menu Panel
        String titlehtml = constants.Feeds() + " | ";
        titlehtml += "<a href=\"" + getURL(page, "inline")  + "\" target=\"_blank\">" + constants.Modify() + "</a>";
        titlehtml += " | " + "<a href=\"" + getURL(page, "view", "confirm=1")  + "\" target=\"_blank\">" + constants.Reload() + "</a>";
        titlehtml += " | " + "<a href=\"" + getURL(page, "view", "status=1")  + "\" target=\"_blank\">" + constants.Status() + "</a>";
        HTML title = new HTML(titlehtml);

        menupanel.setStyleName("rssmenu");
        menupanel.add(title);
        TabPanel tabpanel = new TabPanel();
        menupanel.add(tabpanel);

        tabpanel.add(feedTree, constants.AllFeeds());
        tabpanel.getTabBar().selectTab(0);
        feedTree.setStyleName("feedtree");
        tabpanel.add(groupTree, constants.Groups());
        groupTree.setStyleName("grouptree");
        tabpanel.add(keywordTree, constants.Keywords());
        keywordTree.setStyleName("keywordtree");

        VerticalPanel searchpanel = new VerticalPanel();
        searchform.addFormHandler(this);
        searchform.setStyleName("menutab");
        HorizontalPanel formpanel = new HorizontalPanel();
        searchform.add(formpanel);
        formpanel.add(new HTML(constants.Search()));
        keywordTextBox.setName("keyword");
        keywordTextBox.setStyleName("formfield");
        formpanel.add(keywordTextBox);
        Button button = new Button();
        button.setText(constants.Go());
        button.setStyleName("formfield");
        button.addClickListener( new ClickListener() {
            public void onClick(Widget wd) {
                onSearch();
            }
        });
        formpanel.add(button);
        searchpanel.add(searchform);
        tagspanel = new FlowPanel();
        searchpanel.add(tagspanel);
        tabpanel.add(searchpanel, constants.Search());

        // Load the feed list
        XWikiService.App.getInstance().getDocument(constants.feedPage(), true, true, false, new AsyncLoadFeedCallback(this));

        // Load the tags list
        XWikiService.App.getInstance().customQuery("Feeds.TagsList", 0, 0, new AsyncLoadTagsListCallback(this));        

        // Load the latest article list
        filterStatus.trashed = -1;
        onActivateAllFilter();
    }

    private String getURL(String page, String action) {
        return getURL(page, action, null);
    }

    private String getURL(String page, String action, String qs) {
        String url = "/" + webappName  + "/bin/" + action + "/" + page.replace('.','/');
        if (qs!=null)
         url += "?" + qs;
        return url;
    }

    private void prepareFilterPanel() {
        filterpanel.clear();
        filterpanel.setStyleName("filter");
        DockPanel filterline = new DockPanel();
        filterline.setStyleName("filterline");
        HTML filtertitle = new HTML(constants.Filters());
        filtertitle.setStyleName("filtertitle");
        filterline.add(filtertitle, DockPanel.WEST);
        Panel filterstatus = getFilterStatus();
        filterstatus.setStyleName("filterstatus");
        filterline.add(filterstatus, DockPanel.WEST);

        String params = "";
        params += "&flagged=" + filterStatus.flagged;
        params += "&trashed=" + filterStatus.trashed;
        params += "&read=" + filterStatus.read;
        if (filterStatus.feed!=null)
         params += "&feed=" + filterStatus.feed;
        if (filterStatus.group!=null)
         params += "&group=" + filterStatus.group;
        if (filterStatus.tags.size()>0)
         params += "&tags=" + filterStatus.tags.toString();
        if (filterStatus.keyword!=null)
         params += "&keyword=" + filterStatus.keyword;
        if (filterStatus.date!=null)
         params += "&date=" + filterStatus.date;

        String pressreviewurl = "/xwiki/bin/view/Feeds/PressReview?" + params;
        String tagcloudurl = "/xwiki/bin/view/Feeds/TagCloud?" + params;

        // Outside link
        String outsidelinkhtml = "<a href=\"" + tagcloudurl + "\" target=\"_blank\"><img src=\""
                                + getImagePath("agrss_comment.png") + "\" /></a>";
        HTML tagcloud = new HTML(outsidelinkhtml);
        tagcloud.setStyleName("tabcloudlink");
        filterline.add(tagcloud, DockPanel.EAST);

        outsidelinkhtml = "<a href=\"" + pressreviewurl + "\" target=\"_blank\"><img src=\""
                                + getImagePath("agrss_mail.png") + "\" /></a>";
        HTML pressreview = new HTML(outsidelinkhtml);
        pressreview.setStyleName("pressreviewlink");
        filterline.add(pressreview, DockPanel.EAST);
        filterpanel.add(filterline);
        DockPanel filterline2 = new DockPanel();
        filterline2.setStyleName("filterlimit");
        FormPanel dateform = new FormPanel();
        dateform.addFormHandler(this);
        HorizontalPanel formpanel = new HorizontalPanel();
        formpanel.setStyleName("dateform");
        dateform.add(formpanel);
        HTML filtertext = new HTML(constants.Limitfrom());
        formpanel.add(filtertext);
        dateTextBox.setName("date");
        if (filterStatus.date==null) {
            Date date = new Date();
            date = new Date(date.getTime() - 24 * 60 * 60 * 1000);
            date.setHours(12);
            date.setMinutes(0);
            date.setSeconds(0);
            SimpleDateFormat format = new SimpleDateFormat("yyyy/MM/dd HH:mm");
            String sdate = format.format(date);
            dateTextBox.setText(sdate);
        } else
         dateTextBox.setText(filterStatus.date);
        dateTextBox.setStyleName("formfield");
        formpanel.add(dateTextBox);
        Button button = new Button();
        button.setText(constants.Go());
        button.setStyleName("formfield");
        button.addClickListener( new ClickListener() {
            public void onClick(Widget wd) {
                onUpdateDate();
            }
        });
        formpanel.add(button);
        HTML filterformattext = new HTML(constants.Limitformat());
        formpanel.add(filterformattext);
        filterline2.add(dateform, DockPanel.WEST);

        Hyperlink filterclear = new Hyperlink(constants.ClearFilter(), "");
        filterclear.setStyleName("filterclear");
        filterclear.addClickListener(new ClickListener() {
            public void onClick(Widget sender) {
                clearFilter();
            }
        });
        filterline2.add(filterclear, DockPanel.EAST);
        filterpanel.add(filterline2);

        /*
        CalendarPanel cpanel = new CalendarPanel();
        cpanel.addCalendarListener(new CalendarListener() {

            public void onDateClick(CalendarDate calendarDate) {
                Window.alert(calendarDate.toString());
            }
        });
        */

        Panel hpanel = new FlowPanel();
        hpanel.setStyleName("filterline");

        // filtre mot clé
        Image keyword;
        if (filterStatus.keyword!=null) {
            keyword = new Image(getImagePath("agrss_mots_on.png"));
            keyword.addClickListener(new ClickListener() {
                public void onClick(Widget sender) {
                    clearKeywordFilter();
                }
            });
            keyword.setStyleName("keywordfilteractive filterimage");
        } else {
            keyword = new Image(getImagePath("agrss_mots_off.png"));
            keyword.setStyleName("keywordfilterinactive filterimage");
        }
        hpanel.add(keyword);

        // Filtre tags
        Image tags;
        if (filterStatus.tags.size()>0) {
            tags = new Image(getImagePath("agrss_tag_on.png"));
            tags.addClickListener(new ClickListener() {
                public void onClick(Widget sender) {
                    clearTagsFilter();
                }
            });
            tags.setStyleName("tagsfilteractive filterimage");
        } else {
            tags = new Image(getImagePath("agrss_tag_off.png"));
            tags.setStyleName("tagsfilterinactive filterimage");
        }
        hpanel.add(tags);

        // Filtre suivi/non suivi
        Image flagged, unflagged;
        if (filterStatus.flagged==1) {
            flagged = new Image(getImagePath("agrss_suiv_on.png"));
            flagged.setStyleName("flaggedfilteractive filterimage");
        } else {
            flagged = new Image(getImagePath("agrss_suiv_off.png"));
            flagged.setStyleName("flaggedfilterinactive filterimage");
        }
        flagged.addClickListener(new ClickListener() {
            public void onClick(Widget sender) {
                switchFlaggedFilter();
            }
        });
        hpanel.add(flagged);
        if (filterStatus.flagged==-1) {
            unflagged = new Image(getImagePath("agrss_nonsuiv_on.png"));
            unflagged.setStyleName("unflaggedfilteractive nfilterimage");
        } else {
            unflagged = new Image(getImagePath("agrss_nonsuiv_off.png"));
            unflagged.setStyleName("unflaggedfilterinactive nfilterimage");
        }
        unflagged.addClickListener(new ClickListener() {
            public void onClick(Widget sender) {
                switchUnFlaggedFilter();
            }
        });
        hpanel.add(unflagged);

        // Filtre lu/non lu
        Image read, unread;
        if (filterStatus.read==1) {
            read = new Image(getImagePath("agrss_lu_on.png"));
            read.setStyleName("readfilteractive filterimage");
        } else {
            read = new Image(getImagePath("agrss_lu_off.png"));
            read.setStyleName("readfilterinactive filterimage");
        }
        read.addClickListener(new ClickListener() {
            public void onClick(Widget sender) {
                switchReadFilter();
            }
        });
        hpanel.add(read);
        if (filterStatus.read==-1) {
            unread = new Image(getImagePath("agrss_nonlu_on.png"));
            unread.setStyleName("unreadfilteractive nfilterimage");
        } else {
            unread = new Image(getImagePath("agrss_nonlu_off.png"));
            unread.setStyleName("unreadfilterinactive nfilterimage");
        }
        unread.addClickListener(new ClickListener() {
            public void onClick(Widget sender) {
                switchUnReadFilter();
            }
        });
        hpanel.add(unread);

        // Filtre poubelle
        Image trashed, untrashed;
        if ((filterStatus.trashed==1)&&(filterStatus.flagged!=1)) {
            trashed = new Image(getImagePath("agrss_poub_on.png"));
            trashed.setStyleName("trashedfilteractive filterimage");
        } else {
            trashed = new Image(getImagePath("agrss_poub_off.png"));
            trashed.setStyleName("trashedfilterinactive filterimage");
        }
        trashed.addClickListener(new ClickListener() {
            public void onClick(Widget sender) {
                switchTrashedFilter();
            }
        });
        hpanel.add(trashed);

        if ((filterStatus.trashed==-1)&&(filterStatus.flagged!=-1)) {
            untrashed = new Image(getImagePath("agrss_nonpoub_on.png"));
            untrashed.setStyleName("untrashedfilteractive nfilterimage");
        } else {
            untrashed = new Image(getImagePath("agrss_nonpoub_off.png"));
            untrashed.setStyleName("untrashedfilterinactive nfilterimage");
        }
        untrashed.addClickListener(new ClickListener() {
            public void onClick(Widget sender) {
                switchUnTrashedFilter();
            }
        });
        hpanel.add(untrashed);

        Hyperlink nextlink = new Hyperlink(constants.Next(), "");
        nextlink.addStyleName("articlesnext");
        nextlink.addClickListener(new ClickListener() {
            public void onClick(Widget widget) {
                filterStatus.start  += NB_ARTICLES;
                onRefresh();
            }
        });
        hpanel.add(nextlink);

        filterpanel.add(hpanel);
    }


    private void onUpdateDate() {
        if (dateTextBox.getText().length() == 0) {
            Window.alert(constants.NoSearchTerm());
        } else {
            onActivateDate(dateTextBox.getText());
        }
    }

    private String getImagePath(String filename) {
        return GWT.getModuleBaseURL()  + "/" + filename;
    }

    private void clearTagsFilter() {
        //To change body of created methods use File | Settings | File Templates.
    }

    private void clearKeywordFilter() {
        filterStatus.keyword = null;
        onActivateAllFilter();
    }

    private void clearFilter() {
        filterStatus.reset();
        onActivateAllFilter();
    }

    private Panel getFilterStatus() {
        Panel status = new HorizontalPanel();
        if (filterStatus.feed!=null) {
            String html = RSSReader.getConstants().Feed();
            html += " " + filterStatus.feed;
            status.add(new HTML(html));
            Hyperlink datelink = new Hyperlink("(x)", "");
            datelink.addClickListener(new ClickListener() {
                public void onClick(Widget widget) {
                    filterStatus.feed = null;
                    onRefresh();
                }
            });
            status.add(datelink);
        }

        addSeparator(status);

        if (filterStatus.group!=null) {
            String html = RSSReader.getConstants().Group();
            html += " " + filterStatus.group;
            status.add(new HTML(html));
            Hyperlink datelink = new Hyperlink("(x)", "");
            datelink.addClickListener(new ClickListener() {
                public void onClick(Widget widget) {
                    filterStatus.group = null;
                    onRefresh();
                }
            });
            status.add(datelink);
        }

        addSeparator(status);

        if (filterStatus.keyword!=null) {
            String html = RSSReader.getConstants().Keyword();
            html += " " + filterStatus.keyword;
            status.add(new HTML(html));
        }

        addSeparator(status);

        if (filterStatus.tags.size()>0) {
            String html = RSSReader.getConstants().Tags();
            status.add(new HTML(html));
            Hyperlink tagslink = new Hyperlink("(x)", "");
            tagslink.addClickListener(new ClickListener() {
                public void onClick(Widget widget) {
                    filterStatus.tags.clear();
                    onRefresh();
                }
            });
            status.add(tagslink);
            status.add(new HTML(": "));
            for (int i=0;i<filterStatus.tags.size();i++) {
                html = " " + filterStatus.tags.get(i);
                status.add(new HTML(html));
            }
        }

        addSeparator(status);

        if (filterStatus.flagged ==1) {
            String html = RSSReader.getConstants().FlagOn();
            status.add(new HTML(html));
        }
        if (filterStatus.flagged ==-1) {
            String html = RSSReader.getConstants().FlagOff();
            status.add(new HTML(html));
        }
        if (filterStatus.trashed == 1) {
            String html = RSSReader.getConstants().TrashedOn();
            status.add(new HTML(html));
        }
        if (filterStatus.trashed ==-1) {
            String html = RSSReader.getConstants().TrashedOff();
            status.add(new HTML(html));
        }

        addSeparator(status);

        if (filterStatus.read==1) {
            String html = RSSReader.getConstants().ReadOn();
            status.add(new HTML(html));
        }
        if (filterStatus.read==-11) {
            String html = RSSReader.getConstants().ReadOff();
            status.add(new HTML(html));
        }

        addSeparator(status);

        if (filterStatus.date!=null) {
            String html = RSSReader.getConstants().Limitfrom();
            html += filterStatus.date;
            status.add(new HTML(html));
            Hyperlink datelink = new Hyperlink("(x)", "");
            datelink.addClickListener(new ClickListener() {
                public void onClick(Widget widget) {
                    filterStatus.date = null;
                    onRefresh();
                }
            });
            status.add(datelink);
        }
        return status;
    }

    private void addSeparator(Panel status) {
        /*
        HTML html = new HTML(" - ");
        html.setStyleName("filtersep");
        status.add(html);
        */
    }

    private void onPressReview() {
        // Send a pressreview
    }

    private void switchFlaggedFilter() {
        if (filterStatus.flagged!=1) {
           filterStatus.flagged = 1;
           if (filterStatus.trashed==1)
            filterStatus.trashed = 0;
        } else {
            filterStatus.flagged = 0;
            filterStatus.trashed = -1;
        }
        onActivateAllFilter();
    }

    private void switchUnFlaggedFilter() {
        if (filterStatus.flagged!=-1) {
           filterStatus.flagged = -1;
           filterStatus.trashed = -1;
        } else {
            filterStatus.flagged = 0;
            filterStatus.trashed = 0;
        }
        onActivateAllFilter();
    }

    private void switchReadFilter() {
        if (filterStatus.read!=1) {
           filterStatus.read = 1;
        } else {
            filterStatus.read = 0;
        }
        onActivateAllFilter();
    }

    private void switchUnReadFilter() {
        if (filterStatus.read!=-1) {
           filterStatus.read = -1;
        } else {
            filterStatus.read = 0;
        }
        onActivateAllFilter();
    }

    private void switchTrashedFilter() {
        if (filterStatus.trashed!=1) {
           filterStatus.trashed = 1;
           filterStatus.flagged = 0;
        } else {
           filterStatus.trashed = 0;
        }
        onActivateAllFilter();
    }

    private void switchUnTrashedFilter() {
        if (filterStatus.trashed!=-1) {
           filterStatus.trashed = -1;
           filterStatus.flagged = 0;
        } else {
           filterStatus.trashed = 0;
        }
        onActivateAllFilter();
    }

    public void addToGroup(String group, Feed feed) {
        Map feeds = (Map) groups.get(group);
        if (feeds == null) {
            feeds = new HashMap();
            groups.put(group, feeds);
        }
        feeds.put(feed.getName(), feed);
    }

    public void loadFeeds(Document feedpage) {
        if (feedpage!=null) {
            feeds = new HashMap();
            groups = new HashMap();
            keywords = new HashMap();


            List fobjects = feedpage.getObjects("XWiki.AggregatorURLClass");
            if (fobjects!=null) {
                for (int i=0;i<fobjects.size();i++) {
                    XObject xobj = (XObject) fobjects.get(i);
                    Feed feed = new Feed(xobj);
                    List feedgroups = feed.getGroups();
                    if (feedgroups!=null) {
                        for (int j=0;j<feedgroups.size();j++) {
                            addToGroup((String) feedgroups.get(j), feed);
                        }
                    }
                    feeds.put(feed.getName(), feed);
                }
            }
            List objects = feedpage.getObjects("XWiki.KeywordClass");
            if (objects!=null) {
                for (int j=0;j<objects.size();j++) {
                    XObject xobj = (XObject) objects.get(j);
                    String name = (String) xobj.getProperty("name");
                    String group = (String) xobj.getProperty("group");
                    String fullname = name;
                    if ((group!=null)&&(!group.equals("")))
                     fullname += " - " + group;
                    keywords.put(fullname, xobj);
                }
             }

            refreshFeedTree();
        }
    }

    private void refreshFeedTree() {
        Iterator feedit = feeds.values().iterator();
        while (feedit.hasNext()) {
            Feed feed = (Feed) feedit.next();
            addFeedToTree(feedTree, feed);
        }
        List keys = new ArrayList(groups.keySet());
        Collections.sort(keys);
        Iterator groupit = keys.iterator();
        while (groupit.hasNext()) {
            String groupname = (String) groupit.next();
            if ((groupname!=null)&&(!groupname.trim().equals(""))) {
                Map group = (Map) groups.get(groupname);
                TreeItem groupItemTree = new TreeItem();
                Hyperlink link = new Hyperlink(groupname, "");
                link.setStyleName("treeelementtext");
                link.addClickListener(new ActivateGroupClickListener(this, link, groupname));
                groupItemTree.setWidget(link);
                groupTree.addItem(groupItemTree);
                List feeds = new ArrayList(group.keySet());
                Collections.sort(feeds);
                Iterator feedgroupit = feeds.iterator();
                while (feedgroupit.hasNext()) {
                    String feedname = (String) feedgroupit.next();
                    Feed feed = (Feed) group.get(feedname);
                    addFeedToTree(groupItemTree, feed);
                }
                groupTree.addItem(groupItemTree);
            }
        }

        List kkeys = new ArrayList(keywords.keySet());
        Collections.sort(kkeys);
        Iterator keywordit = kkeys.iterator();
        while (keywordit.hasNext()) {
            String keywordname = (String) keywordit.next();
            addKeywordToTree(keywordTree, keywordname);
        }
        setViewPanelWidth();
    }

    private void addFeedToTree(Tree treeItem, Feed feed) {
        String feedtitle =  feed.getName() + "(" + feed.getNb() + ")";
        String imgurl = feed.getImgurl();
        if ((imgurl!=null)&&(!imgurl.trim().equals(""))) {
            feedtitle = "<img src=\"" + imgurl + "\" class=\"rssthumbnail\" />" + feedtitle;
            String url = feed.getUrl();
        }
        Hyperlink link = new Hyperlink(feedtitle, true, "");
        link.setStyleName("treeelementtext");
        link.addClickListener(new ActivateFeedClickListener(this, feed.getName(), feed.getUrl(), link));
        treeItem.add(link);
    }

    private void addFeedToTree(TreeItem treeItem, Feed feed) {
        String feedtitle =  feed.getName() + "(" + feed.getNb() + ")";
        String imgurl = feed.getImgurl();
        if ((imgurl!=null)&&(!imgurl.trim().equals(""))) {
            feedtitle = "<img src=\"" + imgurl + "\" class=\"rssthumbnail\" />" + feedtitle;
        }
        Hyperlink link = new Hyperlink(feedtitle, true, "");
        link.setStyleName("treeelementtext");
        link.addClickListener(new ActivateFeedClickListener(this, feed.getName(), feed.getUrl(), link));
        treeItem.addItem(link);
    }

    private void addKeywordToTree(Tree treeItem, String keyword) {
        Hyperlink link = new Hyperlink(keyword, "");
        link.setStyleName("treeelementtext");
        XObject xobj = (XObject) keywords.get(keyword);
        link.addClickListener(new ActivateKeywordClickListener(this, xobj, link));
        treeItem.addItem(link);
    }

    public void showError(Throwable caught) {
        showError(caught.getMessage());
    }

    public void showError(String text) {
        dialog.setText(text);
        Button ok = new Button(constants.OK());
        ok.addClickListener(new ClickListener() {
            public void onClick(Widget sender) {
                dialog.hide();
            }
        });
        dialog.setWidget(ok);
        int posX = rpanel.getAbsoluteLeft() + rpanel.getOffsetWidth()/2 - dialog.getOffsetWidth()/2;
        int posY = rpanel.getAbsoluteTop() + rpanel.getOffsetHeight()/2 - dialog.getOffsetHeight()/2;
        if (posX<0)
            posX = 0;
        if (posY<0)
            posY = 0;
        dialog.setPopupPosition(posX, posY);
        dialog.show();
    }

    public void onRefresh() {
        onActivateAllFilter(filterStatus.start);
    }

    public void onActivateAll() {
        onActivateAll(0);
    }

    public void onActivateAll(int start) {
        String message = constants.LoadingAll();
        filterStatus.reset();
        showArticles(message, start);
    }

    public void onActivateAllFilter() {
        onActivateAllFilter(0);
    }

    public void onActivateAllFeedsFilter() {
        filterStatus.feed = null;
        filterStatus.group = null;
        onActivateAllFilter();
    }

    public void onActivateAllFilter(int start) {
        String message = constants.LoadingAll();
        showArticles(message, start);
    }

    public void onActivateDate(String text) {
        onActivateDate(text, 0);
    }

    public void onActivateDate(String text, int start) {
            filterStatus.date = text;
        showArticles("",  start);
    }


    public void onActivateGroup(String groupname) {
        onActivateGroup(groupname, 0);
    }

    public void onActivateGroup(String groupname, int start) {
        String message = constants.Loading() + "  " + groupname;
        filterStatus.group = groupname;
        filterStatus.feed = null;
        showArticles(message, start);
    }

    public void onActivateFeed(String name, String url) {
        onActivateFeed(name, url, 0);
    }

    public void onActivateFeed(String name, String url, int start) {
        String message = constants.Loading() + "  " + name + " " + url;
        filterStatus.group = null;
        filterStatus.feed = url;
        showArticles(message, start);
    }

    public void onActivateKeyword(String keyword, String group) {
        onActivateKeyword(keyword, group, 0);
    }

    public void onActivateKeyword(String keyword, String group, int start) {
        String message = constants.LoadingMatching() + " " + keyword;
        if (group!=null)
             message += " - " + group;
        filterStatus.feed = null;
        filterStatus.keyword = keyword;
        filterStatus.group = group;
        showArticles(message, start);
    }

    
    public void showArticles(String message, int start) {
        HTML html = new HTML();
        html.setText(message);
        viewpanel.clear();
        prepareFilterPanel();
        viewpanel.add(filterpanel, DockPanel.NORTH);
        String skeyword = (filterStatus.keyword==null) ? null : filterStatus.keyword.replaceAll("'", "''");
        String sql = ", BaseObject as obj, XWiki.FeedEntryClass as feedentry ";
        String wheresql = "where doc.fullName=obj.name and obj.className='XWiki.FeedEntryClass' and obj.id=feedentry.id ";

        if ((filterStatus.tags!=null)&&(filterStatus.tags.size()>0)) {
            for(int i=0;i<filterStatus.tags.size();i++) {
                String tag = (String) filterStatus.tags.get(i);
                wheresql += " and '" + tag.replaceAll("'","''") + "' in elements(feedentry.tags) ";
            }
        }

        if ((filterStatus.keyword!=null)&&(!filterStatus.keyword.trim().equals(""))) {
              wheresql  += " and (feedentry.title like '%" + skeyword + "%' "
                + " or feedentry.content like '%" + skeyword + "%' "
                + " or feedentry.fullContent like '%" + skeyword + "%') ";
        }

        if (filterStatus.flagged==1) {
             wheresql += " and feedentry.flag=1";
        } else if ((filterStatus.flagged==-1)&&(filterStatus.trashed==-1)) {
            wheresql += " and (feedentry.flag=0 or feedentry.flag is null)";
        } else if (filterStatus.trashed==1) {
            wheresql += " and feedentry.flag=-1";
        } else if (filterStatus.trashed==-1) {
            wheresql += " and (feedentry.flag>-1 or feedentry.flag is null)";
        } else if (filterStatus.flagged==-1) {
            wheresql += " and (feedentry.flag<1 or feedentry.flag is null)";
        }

        if ((filterStatus.feed!=null)&&(!filterStatus.feed.trim().equals(""))) {
            wheresql += " and feedentry.feedurl='" + filterStatus.feed.replaceAll("'","''") + "'";
        } else if ((filterStatus.group!=null)&&(!filterStatus.group.trim().equals(""))) {
            wheresql += " and feedentry.feedurl in ("
                     + "select feed.url from XWiki.AggregatorURLClass as feed where '" + filterStatus.group.replaceAll("'","''") + "' in elements(feed.group))"; 
        }

        if (filterStatus.date!=null) {
            wheresql += " and feedentry.date >= '" + filterStatus.date + "' ";
        }

        if (filterStatus.read==1) {
            wheresql += " and feedentry.read=1";
        } else if (filterStatus.read==-1) {
            wheresql += " and (feedentry.read is null or feedentry.read=0)";
        }

        sql += wheresql + " order by feedentry.date desc";
        
        setViewPanelWidth();
        // Window.alert(sql);
        XWikiService.App.getInstance().getDocuments(sql, NB_ARTICLES, start, true, true, false, new AsyncLoadFeedContentCallback(this, filterStatus.keyword));
    }

    private void setViewPanelWidth() {
        viewpanel.setWidth((rpanel.getOffsetWidth() - menupanel.getOffsetWidth()) + "px");
        // viewpanel.setHeight((Window.getClientHeight() - (rpanel.getAbsoluteTop() + filterpanel.getOffsetHeight())) + "px");
    }

    public void showArticle(List feedentries, String keyword) {
        boolean done = false;
        if (feedentries!=null) {
            for (int i=0;i<feedentries.size();i++) {
                done = true;
                Document feedpage = (Document) feedentries.get(i);
                if (feedpage!=null) {
                    XObject feedentry = feedpage.getObject("XWiki.FeedEntryClass", 0);
                    String title = feedentry.getViewProperty("title");
                    String feedname = feedentry.getViewProperty("feedname");
                    String feedurl = feedentry.getViewProperty("feedurl");
                    String url = feedentry.getViewProperty("url");
                    String author = feedentry.getViewProperty("author");
                    String date = feedentry.getViewProperty("date");
                    String content = feedentry.getViewProperty("content");
                    String tags = feedentry.getViewProperty("tags");
                    Integer iFlag = (Integer) feedentry.getProperty("flag");
                    int flagstatus = (iFlag==null) ? 0 : iFlag.intValue();
                    Integer iRead = (Integer) feedentry.getProperty("read");
                    int readstatus = (iRead==null) ? 0 : iRead.intValue();
                    List comments = feedpage.getObjects("XWiki.XWikiComments");

                    VerticalPanel articlepanel = new VerticalPanel();
                    articlepanel.setStyleName("article");

                    // Article Title box
                    HorizontalPanel articletoppanel = new HorizontalPanel();
                    articlepanel.add(articletoppanel);
                    HorizontalPanel articletitlepanel2 = new HorizontalPanel();
                    articletoppanel.add(articletitlepanel2);

                    // Image
                    if (feedname!=null) {
                        Feed feed = (Feed) feeds.get(feedname);
                        if (feed!=null) {
                            String imgurl = feed.getImgurl();
                            if ((imgurl!=null)&&(!imgurl.trim().equals(""))) {
                                Image image = new Image(imgurl);
                                image.setStyleName("rssthumbnail2");
                                articletitlepanel2.add(image);
                            }
                        }
                    }

                    FlowPanel articletitlepanel = new FlowPanel();
                    articletitlepanel2.add(articletitlepanel);
                    articletitlepanel.setStyleName("articletitle");

                    // Flag
                    Image flaglink = new Image(getImagePath((flagstatus==1) ? "agrss_star_on.png" : "agrss_star_off.png"));
                    flaglink.addClickListener(new FlagClickListener(this, feedentry, feedpage.getSpace() + "." + feedpage.getName(), flaglink));
                    flaglink.setStyleName("articleflag");
                    articletitlepanel.add(flaglink);

                    // Title text
                    Hyperlink titlelink = new Hyperlink(title, "feedcontentview "+ clickCounter);
                    if (readstatus==1)
                     titlelink.setStyleName("articletitletext");
                    else
                     titlelink.setStyleName("articletitletextunread");

                    articletitlepanel.add(titlelink);

                    // Outside link
                    String outsidelinkhtml = "<a href=\"" + url + "\" target=\"_blank\"><img src=\""
                                            + getImagePath("agrss_extlink.png") + "\" /></a>";
                    HTML outsidelink = new HTML(outsidelinkhtml);
                    outsidelink.setStyleName("articleextlink");
                    articletitlepanel.add(outsidelink);

                    Image trashlink = new Image(getImagePath("agrss_trash.png"));
                    trashlink.addClickListener(new TrashClickListener(this, feedentry, feedpage.getSpace() + "." + feedpage.getName(), trashlink));
                    trashlink.setStyleName("articletrash");
                    articletitlepanel.add(trashlink);

                    // Outside link
                    outsidelinkhtml = "<a href=\"" + url + "\" target=\"_blank\">" + url + "</a>";
                    outsidelink = new HTML(outsidelinkhtml);
                    outsidelink.setStyleName("articleextlink2");
                    articlepanel.add(outsidelink);

                    // Blog source
                    HTML authorhtml = new HTML();
                    authorhtml.setStyleName("articleauthor");
                    articletitlepanel.add(authorhtml);

                    Hyperlink feedlink = new Hyperlink(feedname, "feedview");
                    feedlink.addClickListener(new ActivateFeedClickListener(this, feedname, feedurl, feedlink));
                    feedlink.setStyleName("articlesource");
                    articletitlepanel.add(feedlink);

                    // Date
                    HTML datehtml = new HTML(date);
                    datehtml.setStyleName("articledate");
                    articletitlepanel.add(datehtml);

                    // Commentaires
                    String com = ((comments==null) ? 0 : comments.size()) + " " + constants.Comments();
                    HTML commenthtml = new HTML(com);
                    commenthtml.setStyleName("articlecommentsnb");
                    articletitlepanel.add(commenthtml);

                    // Need to add actions at the right
                    
                    if ("".equals(keyword)) {
                        String regex = "(";
                        for (int chnb=0;chnb<keyword.length();chnb++) {
                            String ch = keyword.substring(chnb,chnb + 1).toLowerCase();
                            regex += "[" + ch + ch.toUpperCase() + "]";
                        }
                        regex += ")";

                        content = content.replaceAll(regex, "<b>$1</b>");
                    }

                    if (content.trim().equals(""))
                     content = constants.NoContent();
                    else {
                        content = cleanHTML(content);
                    }


                    HorizontalPanel contentzonepanel = new HorizontalPanel();
                    contentzonepanel.setStyleName("articlecontentzone");


                    HTMLPanel contentpanel = new HTMLPanel(content);
                    contentpanel.setStyleName("articlecontent");
                    contentzonepanel.add(contentpanel);

                    VerticalPanel datapanel = new VerticalPanel();
                    contentzonepanel.add(datapanel);


                    FlexTable tagspanel = new FlexTable();
                    tagspanel.setStyleName("tagsbox");
                    datapanel.add(tagspanel);
                    HorizontalPanel tagstitle = new HorizontalPanel();
                    tagstitle.add(new HTML(constants.Tags()));
                    tagspanel.setWidget(0, 0, tagstitle);
                    tagspanel.getCellFormatter().setStyleName(0, 0, "tagsboxtitle");

                    HTML tagshtml = new HTML(tags);
                    Image tagslink = new Image(getImagePath("agrss_comment.png"));
                    tagslink.addClickListener(new TagsClickListener(this, feedpage.getSpace() + "." + feedpage.getName(), tags, tagshtml));
                    tagslink.setStyleName("tagslink");
                    tagstitle.add(tagslink);
                    tagspanel.setWidget(0, 1, tagshtml);
                    tagspanel.getCellFormatter().setStyleName(0, 1, "tagsboxitems");
                    
                    FlexTable commentspanel = new FlexTable();
                    commentspanel.setStyleName("commentsbox");
                    datapanel.add(commentspanel);
                    HorizontalPanel commenttitle = new HorizontalPanel();
                    commenttitle.add(new HTML(constants.Comments()));
                    commentspanel.setWidget(0, 0, commenttitle);
                    commentspanel.getFlexCellFormatter().setColSpan(0, 0, 3);
                    commentspanel.getCellFormatter().setStyleName(0, 0, "commentsboxtitle");

                    Image commentlink = new Image(getImagePath("agrss_comment.png"));
                    commentlink.addClickListener(new CommentClickListener(this, feedpage.getSpace() + "." + feedpage.getName()));
                    commentlink.setStyleName("commentslink");
                    commenttitle.add(commentlink);

                    // Need to add comments here..
                    int line = 1;
                    if (comments!=null) {
                        for (int cnb=comments.size()-1;cnb>=0;cnb--) {
                            XObject comment = (XObject) comments.get(cnb);
                            commentspanel.setHTML(line, 0, comment.getViewProperty("date"));
                            commentspanel.setHTML(line, 1, comment.getViewProperty("author").replaceAll("XWiki.", ""));
                            commentspanel.getCellFormatter().setStyleName(line, 0, "commentsboxheader");
                            line++;
                            commentspanel.getFlexCellFormatter().setColSpan(line, 0, 3);
                            commentspanel.getCellFormatter().setStyleName(line, 0, "commentsboxcontent");
                            commentspanel.setHTML(line, 0, comment.getViewProperty("comment"));
                            line++;
                        }
                    }

                    contentzonepanel.setVisible(false);
                    titlelink.addClickListener(new TitleClickListener(contentzonepanel, feedpage.getSpace() + "." + feedpage.getName(), feedentry, titlelink));
                    articlepanel.add(contentzonepanel);
                    viewpanel.add(articlepanel, DockPanel.NORTH);
                }
            }
            History.newItem("feedview");
        }

        if (done==false) {
            showError(constants.NoArticles());
        }

        setViewPanelWidth();        
    }

    private String cleanHTML(String content) {
        return content.replaceAll("<script.*?>.*?</script>", "");
    }

    public void onSearch() {
        if (keywordTextBox.getText().length() == 0) {
            Window.alert(constants.NoSearchTerm());
        } else {
            onActivateKeyword(keywordTextBox.getText(), null);
        }
    }

    public void onSubmit(FormSubmitEvent event) {
        event.setCancelled(true);
        onSearch();
    }

    public void onSubmitComplete(FormSubmitCompleteEvent event) {
        Window.alert(event.getResults());
    }

    public void flagFeed(String feedname, XObject feedentry, Image link) {
        Integer iFlag = (Integer) feedentry.getProperty("flag");
        int flagstatus = (iFlag==null) ? 0 : iFlag.intValue();
        int newflagstatus = (flagstatus==1) ? 0 : 1;
        XWikiService.App.getInstance().updateProperty(feedname, "XWiki.FeedEntryClass", "flag", newflagstatus, new AsyncFlagFeedCallback(this, feedentry, newflagstatus, link));
    }

    public void trashFeed(String feedname, XObject feedentry, Image link) {
        Integer iFlag = (Integer) feedentry.getProperty("flag");
        int flagstatus = (iFlag==null) ? 0 : iFlag.intValue();
        if (flagstatus!=-1) {
         int newflagstatus = -1;
         XWikiService.App.getInstance().updateProperty(feedname, "XWiki.FeedEntryClass", "flag", newflagstatus, new AsyncFlagFeedCallback(this, feedentry, newflagstatus, link));
        }
    }


    public void flagFeedCallback(XObject feedentry, int newflagstatus, Image link, boolean success) {
        if (success==false)
         showError(constants.FlagFeedFailed());
        else {
            feedentry.setProperty("flag", new Integer(newflagstatus));
            link.setUrl(getImagePath((newflagstatus==1) ? "agrss_star_on.png" : "agrss_star_off.png"));
        }
    }

    public void showCommentForm(String page, int x, int y) {
        VerticalPanel vpanel = new VerticalPanel();
        commentPopup.clear();
        commentPopup.add(vpanel);
        TextArea tarea = new TextArea();
        tarea.setPixelSize(400, 200);
        vpanel.add(tarea);
        FlowPanel fpanel = new FlowPanel();
        vpanel.add(fpanel);
        Button cancel = new Button(constants.Cancel(), new ClickListener() {
            public void onClick(Widget widget) {
                commentPopup.hide();
            }
        });
        Button submit = new Button(constants.PostComment(), new PostCommentClickListener(this, page, tarea));
        fpanel.add(cancel);
        fpanel.add(submit);
        commentPopup.setPopupPosition(x - 400, y - 100);
        commentPopup.show();
    }

    public void postComment(String page, String text) {
        commentPopup.hide();
        XWikiService.App.getInstance().addComment(page, text, new AsyncCallback() {
            public void onFailure(Throwable throwable) {
                showError((Throwable) throwable);
            }
            public void onSuccess(Object object) {
                onRefresh();
            }
        });
    }

    public void showTagsForm(String page, String tags, HTML tagshtml, int x, int y) {
        VerticalPanel vpanel = new VerticalPanel();
        tagsPopup.clear();
        tagsPopup.add(vpanel);
        TextBox tarea = new TextBox();
        tarea.setText(tags);
        tarea.setSize("200px", "50px");
        vpanel.add(tarea);
        FlowPanel fpanel = new FlowPanel();
        vpanel.add(fpanel);
        Button cancel = new Button(constants.Cancel(), new ClickListener() {
            public void onClick(Widget widget) {
                tagsPopup.hide();
            }
        });
        Button submit = new Button(constants.PostTags(), new PostTagsClickListener(this, page, tarea, tagshtml));
        fpanel.add(cancel);
        fpanel.add(submit);
        tagsPopup.setPopupPosition(x - 200, y - 40);
        tagsPopup.show();
    }

    public void postTags(String page, final String tags, HTML tagshtml) {
        tagsPopup.hide();
        List taglist = new ArrayList();
        String[] tagarray = tags.split(" ");
        for (int i=0;i<tagarray.length;i++)
         taglist.add(tagarray[i]);
        XWikiService.App.getInstance().updateProperty(page, "XWiki.FeedEntryClass", "tags", taglist, new PostTagsAsyncCallback(this, tagshtml, tags));
    }

    public void showTagsList(List list) {
        if (list!=null) {
            for (int i=0;i<list.size();i++) {
                List result = (List) list.get(i);
                String name = (String) result.get(0);
                int count = ((Integer)result.get(1)).intValue();
                Hyperlink link = new Hyperlink(name, "");
                int pixels = 9 + count;
                if (pixels>15)
                 pixels = 15;
                link.setStyleName("tagscloudlink tagscloud" + pixels);
                link.addClickListener(new TagsItemClickListener(this, name));
                tagspanel.add(link);
            }
        }
    }

    public void onActivateTag(String tag) {
        if (!filterStatus.tags.contains(tag)) {
            filterStatus.tags.add(tag);
            onActivateAllFilter();
        }
    }

    public void clearTreeStyles() {
        Iterator it = feedTree.treeItemIterator();
        while(it.hasNext()) {
            TreeItem item = (TreeItem) it.next();
            item.getWidget().setStyleName("treeelementtext");
        }
        it = groupTree.treeItemIterator();
        while(it.hasNext()) {
            TreeItem item = (TreeItem) it.next();
            item.getWidget().setStyleName("treeelementtext");
        }
        it = keywordTree.treeItemIterator();
        while(it.hasNext()) {
            TreeItem item = (TreeItem) it.next();
            item.getWidget().setStyleName("treeelementtext");
        }
    }


    public  class TitleClickListener implements ClickListener {
        private Panel panel;
        private XObject feedentry;
        private String feedpage;
        private Hyperlink link;

        public TitleClickListener(Panel panel, String feedpage, XObject feedentry,  Hyperlink link) {
            this.panel = panel;
            this.feedentry = feedentry;
            this.feedpage = feedpage;
            this.link = link;
        }
        public void onClick(Widget sender) {
            clickCounter++;
            panel.setVisible(!panel.isVisible());
            Integer iRead = (Integer) feedentry.getProperty("read");
            int readstatus = (iRead==null) ? 0 : iRead.intValue();
            if (readstatus!=1) {
             XWikiService.App.getInstance().updateProperty(feedpage, "XWiki.FeedEntryClass", "read", 1, new AsyncCallback() {
                 public void onFailure(Throwable throwable) {
                    showError(throwable);
                 }
                 public void onSuccess(Object object) {
                     link.setStyleName("articletitletext");
                 }
             });
            }
            History.newItem("feedcontentview" + clickCounter);
        }
    }
}
