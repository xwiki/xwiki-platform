package com.xpn.xwiki.plugin.mailsender;
import java.util.List;

import com.xpn.xwiki.api.Attachment;

/**
 * The variables to, cc, bcc can contain several email addresses, separated by commas.
 */
public class Mail
{
    private String to;
    private String from;
    private String cc;
    private String bcc;
    private String subject;
    private String textPart;
    private String htmlPart;
    private List attachments;
    

    public List getAttachments()
    {
        return attachments;
    }


    public void setAttachments(List attachments)
    {
        this.attachments = attachments;
    }


    public Mail()
    {
        super();
    }
    
    
    public Mail(String from, String to, String cc, String bcc, String subject, String message, String htmlPart)
    {
        super();
        this.from = from;
        this.to = to;
        this.cc = cc;
        this.bcc = bcc;
        this.subject = subject;
        this.textPart = message;
        this.htmlPart = htmlPart;
    }


    public String getFrom()
    {
        return from;
    }

    public void setFrom(String from)
    {
        this.from = from;
    }

    public String getTo()
    {
        return to;
    }

    public void setTo(String to)
    {
        this.to = to;
    }

    public String getCc()
    {
        return cc;
    }

    public void setCc(String cc)
    {
        this.cc = cc;
    }

    public String getBcc()
    {
        return bcc;
    }

    public void setBcc(String bcc)
    {
        this.bcc = bcc;
    }

    public String getSubject()
    {
        return subject;
    }

    public void setSubject(String subject)
    {
        this.subject = subject;
    }

    public String getTextPart()
    {
        return textPart;
    }

    public void setTextPart(String message)
    {
        this.textPart = message;
    }

    public String toFullString()
    {
        return "From: " + from + "\nTo: " + to + "\nCc: " + cc + "\nBcc: " + bcc + "\nSubject:"
            + subject + "\nText: " + textPart+" \nHTML:"+htmlPart;
    }


    public String getHtmlPart()
    {
        return htmlPart;
    }


    public void setHtmlPart(String htmlPart)
    {
        this.htmlPart = htmlPart;
    }

}
