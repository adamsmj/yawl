/*
 * Copyright (c) 2004-2010 The YAWL Foundation. All rights reserved.
 * The YAWL Foundation is a collaboration of individuals and
 * organisations who are committed to improving workflow technology.
 *
 * This file is part of YAWL. YAWL is free software: you can
 * redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation.
 *
 * YAWL is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General
 * Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with YAWL. If not, see <http://www.gnu.org/licenses/>.
 */

package org.yawlfoundation.yawl.monitor.jsf;

import com.sun.rave.web.ui.appbase.AbstractApplicationBean;
import com.sun.rave.web.ui.component.Link;
import org.yawlfoundation.yawl.monitor.MonitorClient;

import javax.faces.FacesException;
import javax.faces.application.Application;
import javax.faces.application.ViewHandler;
import javax.faces.component.UIViewRoot;
import javax.faces.context.FacesContext;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Application scope data bean for the worklist and admin pages.
 *
 *  @author Michael Adams
 *  BPM Group, QUT Australia
 *  v0.1, 21/10/2007
 *
 *  Boilerplate code generated by Sun Java Studio Creator 2.1
 *
 *  Last Date: 05/01/2008
 */

public class ApplicationBean extends AbstractApplicationBean {

    // REQUIRED AND/OR IMPLEMENTED ABSTRACT PAGE BEAN METHODS //

    private int __placeholder;

    private void _init() throws Exception { }

    /** Constructor */
    public ApplicationBean() { }

    public void init() {
        super.init();

        // Initialize automatically managed components - do not modify
        try {
            _init();
        } catch (Exception e) {
            log("ApplicationBean Initialization Failure", e);
            throw e instanceof FacesException ? (FacesException) e: new FacesException(e);
        }

        // Add init code here that must complete *after* managed components are initialized
    }

    public void destroy() { }

    public String getLocaleCharacterEncoding() {
        return super.getLocaleCharacterEncoding();
    }


    /*******************************************************************************/

    // GLOBAL COMPONENTS //

    public enum PageRef { casesPage, itemsPage, paramsPage }

    // favIcon appears in the browser's address bar for all pages
    private Link favIcon = new Link() ;

    public Link getFavIcon() { return favIcon; }

    public void setFavIcon(Link link) { favIcon = link; }


    private MonitorClient mc;

    public MonitorClient getMonitorClient() {
        if (mc == null) mc = MonitorClient.getInstance();
        return mc;
    }



    /*******************************************************************************/

    // MEMBERS AND METHODS USED BY ALL SESSIONS //

    private static final int PAGE_AUTO_REFRESH_RATE = 30 ;


    public int getDefaultJSFRefreshRate() {
        return PAGE_AUTO_REFRESH_RATE ;
    }


    // mapping of participant id to each session
    private Map<String, SessionBean> sessionReference = new HashMap<String, SessionBean>();

    public void addSessionReference(String participantID, SessionBean sBean) {
        sessionReference.put(participantID, sBean) ;
    }

    public SessionBean getSessionReference(String participantID) {
        return sessionReference.get(participantID) ;
    }

    public void removeSessionReference(String participantID) {
        sessionReference.remove(participantID) ;
    }


    // set of participants currently logged on
    private Set<String> liveUsers = new HashSet<String>();

    public Set<String> getLiveUsers() {
        return liveUsers;
    }

    public void setLiveUsers(Set<String> userSet) {
        liveUsers = userSet;
    }

    public void addLiveUser(String userid) {
        liveUsers.add(userid);
    }

    public void removeLiveUser(String userid) {
        if (isLoggedOn(userid)) liveUsers.remove(userid);
    }

    public boolean isLoggedOn(String userid) {
        return liveUsers.contains(userid);
    }


    /**
     * formats a long time value into a string of the form 'ddd:hh:mm:ss'
     * @param age the time value (in milliseconds)
     * @return the formatted time string
     */
    public String formatAge(long age) {
        long secsPerHour = 60 * 60 ;
        long secsPerDay = 24 * secsPerHour ;
        age = age / 1000 ;                             // ignore the milliseconds

        long days = age / secsPerDay ;
        age %= secsPerDay ;
        long hours = age / secsPerHour ;
        age %= secsPerHour ;
        long mins = age / 60 ;
        age %= 60 ;                                    // seconds leftover
        return String.format("%d:%02d:%02d:%02d", days, hours, mins, age) ;
    }


    public String rPadSp(String str, int padlen) {
        int len = padlen - str.length();
        if (len < 1) return str ;

        StringBuilder result = new StringBuilder(str) ;
        for (int i = 0; i < len; i++) {
            result.append("&nbsp;");
        }
        return result.toString();
    }


    public String rPad (String str, int padlen) {
        int len = padlen - str.length();
        if (len < 1) return str ;

        StringBuilder padded = new StringBuilder(str);
        char[] spaces  = new char[len] ;
        for (int i = 0; i < len; i++) spaces[i] = ' ';
        padded.append(spaces) ;
        return padded.toString();
    }

    public void refresh() {
        FacesContext context = FacesContext.getCurrentInstance();
        Application application = context.getApplication();
        ViewHandler viewHandler = application.getViewHandler();
        UIViewRoot viewRoot = viewHandler.createView(context, context
             .getViewRoot().getViewId());
        context.setViewRoot(viewRoot);
      //  context.renderResponse(); //Optional
  }

}