package rssreader.client;

import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.user.client.ui.Hyperlink;
import api.client.XObject;

/**
 * Created by IntelliJ IDEA.
 * User: ldubost
 * Date: 2 déc. 2006
 * Time: 16:24:44
 * To change this template use File | Settings | File Templates.
 */
public class ActivateKeywordClickListener implements ClickListener {
    private XObject keywordobj;
    private RSSReader reader;
    private Hyperlink link;

    public ActivateKeywordClickListener(RSSReader reader, XObject keywordobj, Hyperlink link) {
        this.keywordobj = keywordobj;
        this.reader = reader;
        this.link = link;
    }

    public void onClick(Widget sender) {
        reader.clearTreeStyles();
        link.setStyleName("treeelementtextactive");
        reader.onActivateKeyword((String) keywordobj.get("name"), (String) keywordobj.get("group"));
    }
}
