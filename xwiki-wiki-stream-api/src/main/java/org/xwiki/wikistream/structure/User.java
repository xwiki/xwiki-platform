/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.xwiki.wikistream.structure;

/**
 * Represents a Wiki user
 * 
 * @version $Id$
 */
public class User
{

    private String userName;

    private String firstName;

    private String lastName;

    private String company;

    private String about;

    private String email;

    private String phone;

    private String address;

    private String blog;

    private String blogFeed;

    /**
     * @param userName
     */
    public User(String userName)
    {
        this.userName = userName;
    }

    /**
     * @param firstName
     * @param lastName
     */
    public User(String firstName, String lastName)
    {
        this.firstName = firstName;
        this.lastName = lastName;
    }

    /**
     * @return the userName
     */
    public String getUserName()
    {
        return userName;
    }

    /**
     * @param userName the userName to set
     */
    public void setUserName(String userName)
    {
        this.userName = userName;
    }

    /**
     * @return the firstName
     */
    public String getFirstName()
    {
        return firstName;
    }

    /**
     * @param firstName the firstName to set
     */
    public void setFirstName(String firstName)
    {
        this.firstName = firstName;
    }

    /**
     * @return the lastName
     */
    public String getLastName()
    {
        return lastName;
    }

    /**
     * @param lastName the lastName to set
     */
    public void setLastName(String lastName)
    {
        this.lastName = lastName;
    }

    /**
     * @return the company
     */
    public String getCompany()
    {
        return company;
    }

    /**
     * @param company the company to set
     */
    public void setCompany(String company)
    {
        this.company = company;
    }

    /**
     * @return the about
     */
    public String getAbout()
    {
        return about;
    }

    /**
     * @param about the about to set
     */
    public void setAbout(String about)
    {
        this.about = about;
    }

    /**
     * @return the email
     */
    public String getEmail()
    {
        return email;
    }

    /**
     * @param email the email to set
     */
    public void setEmail(String email)
    {
        this.email = email;
    }

    /**
     * @return the phone
     */
    public String getPhone()
    {
        return phone;
    }

    /**
     * @param phone the phone to set
     */
    public void setPhone(String phone)
    {
        this.phone = phone;
    }

    /**
     * @return the address
     */
    public String getAddress()
    {
        return address;
    }

    /**
     * @param address the address to set
     */
    public void setAddress(String address)
    {
        this.address = address;
    }

    /**
     * @return the blog
     */
    public String getBlog()
    {
        return blog;
    }

    /**
     * @param blog the blog to set
     */
    public void setBlog(String blog)
    {
        this.blog = blog;
    }

    /**
     * @return the blogFeed
     */
    public String getBlogFeed()
    {
        return blogFeed;
    }

    /**
     * @param blogFeed the blogFeed to set
     */
    public void setBlogFeed(String blogFeed)
    {
        this.blogFeed = blogFeed;
    }

}
