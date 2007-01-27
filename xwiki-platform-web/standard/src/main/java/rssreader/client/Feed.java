package rssreader.client;

import api.client.Document;
import api.client.XObject;

import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: ldubost
 * Date: 7 janv. 2007
 * Time: 12:30:56
 * To change this template use File | Settings | File Templates.
 */
public class Feed {
    private String name;
    private String url;
    private String imgurl;
    private List groups;
    private String date;
    private Integer nb;

    public Feed(Document doc )  {
        this(doc.getObject("XWiki.AggregatorURLClass"));
    }

    public Feed(XObject xobj)  {
        setName((String) xobj.getProperty("name"));
        setUrl((String) xobj.getProperty("url"));
        setGroups((List) xobj.getProperty("group"));
        setDate((String) xobj.getProperty("date"));
        setNb((Integer) xobj.getProperty("nb"));
        setImgurl((String)xobj.getProperty("imgurl"));
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public List getGroups() {
        return groups;
    }

    public void setGroups(List groups) {
        this.groups = groups;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public Integer getNb() {
        return nb;
    }

    public void setNb(Integer nb) {
        this.nb = nb;
    }

    public String getImgurl() {
        return imgurl;
    }

    public void setImgurl(String imgurl) {
        this.imgurl = imgurl;
    }
}
