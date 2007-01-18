package rssreader.client;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.*;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.Window;
import api.client.XWikiService;
import api.client.Document;
import api.client.XObject;

import java.util.*;

import org.gwtwidgets.client.ui.LightBox;

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
    private VerticalPanel viewpanel = new VerticalPanel();
    private VerticalPanel filterpanel = new VerticalPanel();
    private FormPanel searchform = new FormPanel();
    private Panel menupanel = new VerticalPanel();
    private TextBox keywordTextBox = new TextBox();
    private TextBox dateTextBox = new TextBox();
    private DialogBox dialog = new DialogBox();
    //private LightBox dialog2 = new LightBox(dialog);
    private PopupPanel popupMessage = new PopupPanel();
    private Tree feedTree = new Tree();
    private Map feeds;
    private Map groups;
    private List keywords;
    private static RSSReaderConstants constants = (RSSReaderConstants) GWT.create(RSSReaderConstants.class);
    private int clickCounter = 0;
    private static final int NB_ARTICLES = 50;

    // Filters
    private FilterStatus filterStatus = new FilterStatus();
    private Hyperlink flagFilterLink;
    private Hyperlink trashFilterLink;

    public static RSSReaderConstants getConstants() {
        return constants;
    }

    public void onModuleLoad() {
        rpanel = RootPanel.get(constants.divName());
        rpanel.add(hpanel);
        hpanel.add(menupanel);
        hpanel.add(viewpanel);
        viewpanel.add(filterpanel);
        int width = rpanel.getOffsetWidth() - menupanel.getOffsetWidth();
        viewpanel.setWidth(width + "px");
        // Root Panel
        rpanel.setStyleName("rssaggr");
        hpanel.setStyleName("rssaggr");

        // View Panel
        viewpanel.setStyleName("articles");

        // Filter Panel
        prepareFilterPanel();

        // Menu Panel
        HTML title = new HTML();
        title.setHTML(constants.RSSReader());
        menupanel.setStyleName("rssmenu");
        menupanel.add(title);
        menupanel.add(feedTree);

        searchform.addFormHandler(this);
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
        menupanel.add(searchform);

        // Load the feed list
        XWikiService.App.getInstance().getDocument(constants.feedPage(), true, true, false, new AsyncLoadFeedCallback(this));
        // Load the latest article list
        filterStatus.trashed = -1;
        onActivateAllFilter();
    }

    private void prepareFilterPanel() {
        filterpanel.clear();
        filterpanel.setStyleName("filter");
        DockPanel filterline = new DockPanel();
        filterline.setStyleName("filterline");
        HTML filtertitle = new HTML(constants.Filters());
        filtertitle.setStyleName("filtertitle");
        filterline.add(filtertitle, DockPanel.WEST);
        HTML filterstatus = new HTML(getFilterStatus());
        filterstatus.setStyleName("filterstatus");
        filterline.add(filterstatus, DockPanel.WEST);

        String pressreviewurl = "/xwiki/bin/view/Feeds/PressReview?xpage=plain";
        pressreviewurl += "&flagged=" + filterStatus.flagged;
        pressreviewurl += "&trashed=" + filterStatus.trashed;
        pressreviewurl += "&read=" + filterStatus.read;
        if (filterStatus.feed!=null)
         pressreviewurl += "&feed=" + filterStatus.feed;
        if (filterStatus.group!=null)
         pressreviewurl += "&group=" + filterStatus.group;
        if (filterStatus.tags.size()>0)
         pressreviewurl += "&tags=" + filterStatus.tags.toString();
        if (filterStatus.keyword!=null)
         pressreviewurl += "&keyword=" + filterStatus.keyword;
        if (filterStatus.date!=null)
         pressreviewurl += "&date=" + filterStatus.date;

        // Outside link
        String outsidelinkhtml = "<a href=\"" + pressreviewurl + "\" target=\"_blank\"><img src=\""
                                + getImagePath("agrss_mail.png") + "\" /></a>";
        HTML pressreview = new HTML(outsidelinkhtml);
        pressreview.setStyleName("pressreviewlink");
        pressreview.addClickListener(new ClickListener() {
            public void onClick(Widget sender) {
                   onPressReview();
            }
        });
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
        filterline2.add(dateform, DockPanel.WEST);


        if (filterStatus.date!=null) {
           HTML filterdate = new HTML(filterStatus.date.toString());
           filterline2.add(filterdate, DockPanel.WEST);
        }

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

        // Filtre suivi/non suivi
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

    private String getFilterStatus() {
        return filterStatus.toString();
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
            keywords = new ArrayList();


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
                    keywords.add(xobj.getProperty("name"));
                }
                Collections.sort(keywords);
            }

            refreshFeedTree();
        }
    }

    private void refreshFeedTree() {
        feedTree.setWidth("250px");
        feedTree.setHeight("380px");
        feedTree.setStyleName("menutree");
        TreeItem allTree = new TreeItem();
        TreeItem groupsTree = new TreeItem(constants.Groups());
        TreeItem keywordsTree = new TreeItem(constants.Keywords());

        Hyperlink link = new Hyperlink(constants.AllFeeds(), "");
        link.setStyleName("treeelementtext");
        link.addClickListener(new ActivateAllClickListener(this));
        allTree.setWidget(link);

        Iterator feedit = feeds.values().iterator();
        while (feedit.hasNext()) {
            Feed feed = (Feed) feedit.next();
            addFeedToTree(allTree, feed);
        }
        feedTree.addItem(allTree);

        List keys = new ArrayList(groups.keySet());
        Collections.sort(keys);
        Iterator groupit = keys.iterator();
        while (groupit.hasNext()) {
            String groupname = (String) groupit.next();
            if ((groupname!=null)&&(!groupname.trim().equals(""))) {
                Map group = (Map) groups.get(groupname);
                TreeItem groupItemTree = new TreeItem();
                link = new Hyperlink(groupname, "");
                link.setStyleName("treeelementtext");
                link.addClickListener(new ActivateGroupClickListener(this, groupname));
                groupItemTree.setWidget(link);
                groupsTree.addItem(groupItemTree);
                List feeds = new ArrayList(group.keySet());
                Collections.sort(feeds);
                Iterator feedgroupit = feeds.iterator();
                while (feedgroupit.hasNext()) {
                    String feedname = (String) feedgroupit.next();
                    Feed feed = (Feed) group.get(feedname);
                    addFeedToTree(groupItemTree, feed);
                }
                groupsTree.addItem(groupItemTree);
            }
        }
        feedTree.addItem(groupsTree);

        Iterator keywordit = keywords.iterator();
        while (keywordit.hasNext()) {
            String keywordname = (String) keywordit.next();
            addKeywordToTree(keywordsTree, keywordname);
        }
        feedTree.addItem(keywordsTree);
        setViewPanelWidth();
    }

    private void addFeedToTree(TreeItem treeItem, Feed feed) {
        Hyperlink link = new Hyperlink(feed.getName() + "(" + feed.getNb() + ")", "");
        link.setStyleName("treeelementtext");
        link.addClickListener(new ActivateFeedClickListener(this, feed.getName(), feed.getUrl()));
        treeItem.addItem(link);
    }

    private void addKeywordToTree(TreeItem treeItem, String keyword) {
        Hyperlink link = new Hyperlink(keyword, "");
        link.setStyleName("treeelementtext");
        link.addClickListener(new ActivateKeywordClickListener(this, keyword));
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

    public void onActivateKeyword(String keyword) {
        onActivateKeyword(keyword, 0);
    }

    public void onActivateKeyword(String keyword, int start) {
        String message = constants.LoadingMatching() + " " + keyword;
        filterStatus.feed = null;
        filterStatus.keyword = keyword;
        showArticles(message, start);
    }

    
    public void showArticles(String message, int start) {
        HTML html = new HTML();
        html.setText(message);
        viewpanel.clear();
        prepareFilterPanel();
        viewpanel.add(filterpanel);
        String skeyword = (filterStatus.keyword==null) ? null : filterStatus.keyword.replaceAll("'", "''");
        String sql = ", BaseObject as obj, XWiki.FeedEntryClass as feedentry ";
        String wheresql = "where doc.fullName=obj.name and obj.className='XWiki.FeedEntryClass' and obj.id=feedentry.id ";

        if ((filterStatus.tags!=null)&&(filterStatus.tags.size()>0)) {
            for(int i=0;i<filterStatus.tags.size();i++) {
                String tag = (String) filterStatus.tags.get(i);
                wheresql += " and elements(feedentry.tags) contains '" + tag.replaceAll("'","''") + "'";
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
            wheresql += " and elements(feedentry.group) contains '" + filterStatus.group.replaceAll("'","''") + "'";
        }

        if (filterStatus.read!=0) {
            // not implemented
        }

        sql += wheresql + " order by feedentry.date desc";
        
        setViewPanelWidth();
        // Window.alert(sql);
        XWikiService.App.getInstance().getDocuments(sql, NB_ARTICLES, start, true, true, false, new AsyncLoadFeedContentCallback(this, filterStatus.keyword));
    }

    private void setViewPanelWidth() {
        viewpanel.setWidth((rpanel.getOffsetWidth() - menupanel.getOffsetWidth()) + "px");
        viewpanel.setHeight((Window.getClientHeight() - (rpanel.getAbsoluteTop() + filterpanel.getOffsetHeight())) + "px");
    }

    public void showArticle(List feedentries, String keyword) {
        boolean done = false;
        if (feedentries!=null) {
            for (int i=0;i<feedentries.size();i++) {
                done = true;
                Document feedpage = (Document) feedentries.get(i);
                if (feedpage!=null) {
                    XObject feedentry = feedpage.getObject("XWiki.FeedEntryClass", 0);
                    FlexTable table = new FlexTable();
                    String title = feedentry.getViewProperty("title");
                    String feedname = feedentry.getViewProperty("feedname");
                    String feedurl = feedentry.getViewProperty("feedurl");
                    String url = feedentry.getViewProperty("url");
                    String author = feedentry.getViewProperty("author");
                    String date = feedentry.getViewProperty("date");
                    String content = feedentry.getViewProperty("content");
                    Integer iFlag = (Integer) feedentry.getProperty("flag");
                    int flagstatus = (iFlag==null) ? 0 : iFlag.intValue();
                    List comments = feedpage.getObjects("XWiki.XWikiComments");

                    VerticalPanel articlepanel = new VerticalPanel();
                    articlepanel.setStyleName("article");

                    // Article Title box
                    HorizontalPanel articletoppanel = new HorizontalPanel();
                    articlepanel.add(articletoppanel);
                    FlowPanel articletitlepanel = new FlowPanel();
                    articletoppanel.add(articletitlepanel);
                    articletitlepanel.setStyleName("articletitle");

                    // Flag
                    Image flaglink = new Image(getImagePath((flagstatus==1) ? "agrss_star_on.png" : "agrss_star_off.png"));
                    flaglink.addClickListener(new FlagClickListener(this, feedentry, feedpage.getSpace() + "." + feedpage.getName(), flaglink));
                    flaglink.setStyleName("articleflag");
                    articletitlepanel.add(flaglink);

                    // Title text
                    Hyperlink titlelink = new Hyperlink(title, "feedcontentview "+ clickCounter);
                    titlelink.setStyleName("articletitletext");
                    articletitlepanel.add(titlelink);

                    // Outside link
                    String outsidelinkhtml = "<a href=\"" + url + "\" target=\"_blank\"><img src=\""
                                            + getImagePath("agrss_extlink.png") + "\" /></a>";
                    HTML outsidelink = new HTML(outsidelinkhtml);
                    outsidelink.setStyleName("articleextlink");
                    articletitlepanel.add(outsidelink);

                    // Blog source
                    HTML authorhtml = new HTML();
                    authorhtml.setStyleName("articleauthor");
                    articletitlepanel.add(authorhtml);

                    Hyperlink feedlink = new Hyperlink(feedname, "feedview");
                    feedlink.addClickListener(new ActivateFeedClickListener(this, feedname, feedurl));
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

                    VerticalPanel commentspanel = new VerticalPanel();
                    commentspanel.setStyleName("articlecomments");
                    contentzonepanel.add(commentspanel);
                    commentspanel.add(new HTMLPanel(constants.Comments()));
                    // Need to add comments here..

                    contentzonepanel.setVisible(false);
                    titlelink.addClickListener(new TitleClickListener(contentzonepanel));
                    articlepanel.add(contentzonepanel);
                    viewpanel.add(articlepanel);
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
            onActivateKeyword(keywordTextBox.getText());
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

    public void flagFeedCallback(XObject feedentry, int newflagstatus, Image link, boolean success) {
        if (success==false)
         showError(constants.FlagFeedFailed());
        else {
            feedentry.setProperty("flagged", new Integer(newflagstatus));
            link.setUrl(getImagePath((newflagstatus==1) ? "agrss_star_on.png" : "agrss_star_off.png"));
        }
    }


    public  class TitleClickListener implements ClickListener {
        private Panel panel;

        public TitleClickListener(Panel panel) {
            this.panel = panel;
        }
        public void onClick(Widget sender) {
            clickCounter++;
            panel.setVisible(!panel.isVisible());
            History.newItem("feedcontentview" + clickCounter);
        }
    }
}
