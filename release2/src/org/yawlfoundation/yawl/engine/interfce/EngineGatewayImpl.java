/*
 * This file is made available under the terms of the LGPL licence.
 * This licence can be retrieved from http://www.gnu.org/copyleft/lesser.html.
 * The source remains the property of the YAWL Foundation.  The YAWL Foundation is a collaboration of
 * individuals and organisations who are committed to improving workflow technology.
 *
 */


package org.yawlfoundation.yawl.engine.interfce;

import org.apache.log4j.Logger;
import org.jdom.Document;
import org.yawlfoundation.yawl.authentication.*;
import org.yawlfoundation.yawl.elements.YAWLServiceReference;
import org.yawlfoundation.yawl.elements.YDecomposition;
import org.yawlfoundation.yawl.elements.YSpecification;
import org.yawlfoundation.yawl.elements.YTask;
import org.yawlfoundation.yawl.elements.data.YParameter;
import org.yawlfoundation.yawl.elements.state.YIdentifier;
import org.yawlfoundation.yawl.engine.*;
import org.yawlfoundation.yawl.exceptions.YAWLException;
import org.yawlfoundation.yawl.exceptions.YEngineStateException;
import org.yawlfoundation.yawl.exceptions.YPersistenceException;
import org.yawlfoundation.yawl.exceptions.YStateException;
import org.yawlfoundation.yawl.logging.YLogDataItemList;
import org.yawlfoundation.yawl.unmarshal.YMarshal;
import org.yawlfoundation.yawl.util.HttpURLValidator;
import org.yawlfoundation.yawl.util.JDOMUtil;
import org.yawlfoundation.yawl.util.StringUtil;
import org.yawlfoundation.yawl.util.YVerificationMessage;

import java.net.URI;
import java.rmi.RemoteException;
import java.util.*;

/**
 * This class allows access to all of the engines capabilities using Strings and
 * XML interface to pass information.
 *
 * 
 * @author Lachlan Aldred
 * Date: 21/01/2004
 * Time: 11:58:18
 * 
 */
public class EngineGatewayImpl implements EngineGateway {

    Logger logger = Logger.getLogger(EngineGatewayImpl.class);

    private YEngine _engine;
    private YSessionCache _sessionCache;
    private boolean enginePersistenceFailure = false;
    private static final String OPEN_FAILURE = "<failure><reason>";
    private static final String CLOSE_FAILURE = "</reason></failure>";
    private static final String SUCCESS = "<success/>";
    private static final String OPEN_SUCCESS = "<success>";
    private static final String CLOSE_SUCCESS = "</success>";

    /**
     *  Constructor
     */
    public EngineGatewayImpl(boolean persist) throws YPersistenceException {
        _engine = YEngine.getInstance(persist);
        _sessionCache = _engine.getSessionCache();
    }

    // PRIVATE METHODS

    /** encases a message in "<failure><reason>...</reason></failure>"
     *
     * @param msg the text to encase
     * @return the encase message
     */
    private String failureMessage(String msg) {
        return  StringUtil.wrap(StringUtil.wrap(msg, "reason"), "failure");
    }

    /** encases a message in "<success>...</success>
     *
     * @param msg the text to encase
     * @return the encase message
     */
    private String successMessage(String msg) {
        return StringUtil.wrap(msg, "success");
    }


    private String checkSession(String sessionHandle) {
        return _sessionCache.checkConnection(sessionHandle) ? SUCCESS
                               : failureMessage("Invalid or expired session");
    }

    private boolean isFailureMessage(String msg) {
        return msg.startsWith("<fail");
    }



    //**************************************************//
    

    /**
     * Indicates if the engine has encountered some form of persistence failure in its lifetime.<P>
     *
     * @return whether or not the engine failed to achieve persistence
     */
    public boolean enginePersistenceFailure()
    {
        return enginePersistenceFailure;
    }


    /**
     * Registers an external observer gateway with the engine
     * @param gateway the gateway to register
     */
    public void registerObserverGateway(ObserverGateway gateway) {
        _engine.registerInterfaceBObserverGateway(gateway);
    }

    
    public void setDefaultWorklist(String url) {
        _engine.setDefaultWorklist(url);
    }


    public void setAllowAdminID(boolean allow) {
        _engine.setAllowAdminID(allow);
    }


    public void shutdown() {
        _engine.shutdown();
    }
    
    /**
     * Triggers the announcement that engine startup is complete
     * Should only be called from InterfaceB_EngineBasedServer.init()
     */
    public void notifyServletInitialisationComplete() {
        _engine.announceEngineInitialisationCompletion();
    }

    
    public void setActualFilePath(String path) {
        path = path.replace('\\', '/' );             // switch slashes
        if (! path.endsWith("/")) path += "/";       // make sure it has ending slash
        _engine.setEngineClassesRootFilePath(path) ;       
    }



    /**
     *
     * @param sessionHandle
     * @return
     * @throws RemoteException
     */
    public String getAvailableWorkItemIDs(String sessionHandle) throws RemoteException {
        String sessionMessage = checkSession(sessionHandle);
        if (isFailureMessage(sessionMessage)) return sessionMessage;

        Set<YWorkItem> allItems = _engine.getAvailableWorkItems();
        StringBuilder workItemsStr = new StringBuilder();
        workItemsStr.append("<ids>");
        for (YWorkItem workItem : allItems) {
            workItemsStr.append(StringUtil.wrap(workItem.getWorkItemID().toString(), "workItemID"));
        }
        workItemsStr.append("</ids>");
        return workItemsStr.toString();
    }


    /**
     *
     * @param workItemID
     * @param sessionHandle
     * @return
     * @throws RemoteException
     */
    public String getWorkItemDetails(String workItemID, String sessionHandle) throws RemoteException {
        String sessionMessage = checkSession(sessionHandle);
        if (isFailureMessage(sessionMessage)) return sessionMessage;

        YWorkItem workItem = _engine.getWorkItem(workItemID);
        if (workItem != null) {
            return workItem.toXML();
        }
        else {
            return failureMessage("WorkItem with ID (" + workItemID + ") not found.");
        }
    }


    /**
     *
     * @param specID the specification (process definition id)
     * @param sessionHandle the sessionhandle
     * @return a string version of the process definition
     * @throws RemoteException
     */
    public String getProcessDefinition(YSpecificationID specID, String sessionHandle) throws RemoteException {
        String sessionMessage = checkSession(sessionHandle);
        if (isFailureMessage(sessionMessage)) return sessionMessage;

        YSpecification spec = _engine.getProcessDefinition(specID);
        if (spec == null) {
            return failureMessage("Specification with ID (" + specID + ") not found.");
        }
        List<YSpecification> specList = new Vector<YSpecification>();
        specList.add(spec);
        String version = spec.getSchemaVersion();
        try {
            return YMarshal.marshal(specList, version);
        }
        catch (Exception e) {
            logger.error("Failed to marshal a specification into XML.", e);
            return failureMessage("Failed to marshal the specification into XML.");
        }
    }


    public String getSpecificationDataSchema(YSpecificationID specID, String sessionHandle)
                                                   throws RemoteException {
        String sessionMessage = checkSession(sessionHandle);
        if (isFailureMessage(sessionMessage)) return sessionMessage;

        return _engine.getSpecificationDataSchema(specID);
    }


    /**
     *
     * @param workItemID
     * @param sessionHandle
     * @return
     * @throws RemoteException
     */
    public String suspendWorkItem(String workItemID, String sessionHandle) throws RemoteException {
        String sessionMessage = checkSession(sessionHandle);
        if (isFailureMessage(sessionMessage)) return sessionMessage;

        try {
            YWorkItem item = _engine.suspendWorkItem(workItemID);
            if (item != null)
                return successMessage(item.toXML());
            else
                return failureMessage("WorkItem with ID [" + workItemID + "] not found.");
        }
        catch (YAWLException e) {
            if (e instanceof YPersistenceException) {
                enginePersistenceFailure = true;
            }
            return failureMessage(e.getMessage());
        }
    }

    /**
     *
     * @param workItemID
     * @param sessionHandle
     * @return
     * @throws RemoteException
     */
    public String unsuspendWorkItem(String workItemID, String sessionHandle) throws RemoteException {
        String sessionMessage = checkSession(sessionHandle);
        if (isFailureMessage(sessionMessage)) return sessionMessage;

        try {
            YWorkItem item = _engine.unsuspendWorkItem(workItemID);
            if (item != null)
                return successMessage(item.toXML());
            else
                return failureMessage("WorkItem with ID [" + workItemID + "] not found.");
        }
        catch (YAWLException e) {
            if (e instanceof YPersistenceException) {
                enginePersistenceFailure = true;
            }
            return failureMessage(e.getMessage());
        }
    }


    /**
     *
     * @param workItemID
     * @param sessionHandle
     * @return
     * @throws RemoteException
     */
    public String rollbackWorkItem(String workItemID, String sessionHandle) throws RemoteException {
        String sessionMessage = checkSession(sessionHandle);
        if (isFailureMessage(sessionMessage)) return sessionMessage;

        try {
            _engine.rollbackWorkItem(workItemID);
            return SUCCESS;
        } catch (YAWLException e) {
            if (e instanceof YPersistenceException) {
                enginePersistenceFailure = true;
            }
            return failureMessage(e.getMessage());
        }
    }


    /**
     *
     * @param workItemID work item id
     * @param data data
     * @param logPredicate a pre-parsed configurable logging string
     * @param sessionHandle  sessionhandle
     * @return result XML message.
     * @throws RemoteException if used in RMI mode
     */

    public String completeWorkItem(String workItemID, String data, String logPredicate,
                                   boolean force, String sessionHandle) throws RemoteException {
        String sessionMessage = checkSession(sessionHandle);
        if (isFailureMessage(sessionMessage)) return sessionMessage;

        try {
            YWorkItem workItem = _engine.getWorkItem(workItemID);
            if (workItem != null) {
                _engine.completeWorkItem(workItem, data, logPredicate, force);
                return SUCCESS;
            } else {
                return failureMessage("WorkItem with ID [" + workItemID + "] not found.");
            }
        } catch (YAWLException e) {
            if (e instanceof YPersistenceException) {
                enginePersistenceFailure = true;
            }
            return failureMessage(e.getMessage());
        }
    }


    /**
     *
     * @param workItemID
     * @param sessionHandle
     * @return
     * @throws RemoteException
     */
    public String startWorkItem(String workItemID, String sessionHandle) throws RemoteException {
        String sessionMessage = checkSession(sessionHandle);
        if (isFailureMessage(sessionMessage)) return sessionMessage;

        try {
            YWorkItem item = _engine.getWorkItem(workItemID);
            if (item != null) {
                YAWLServiceReference service = null;
                YSession session = _sessionCache.getSession(sessionHandle);
                if (session != null)  {
                    service = ((YServiceSession) session).getService();
                }
                YWorkItem child = _engine.startWorkItem(item, service);
                if( child == null ) {
                	throw new YAWLException(
                			"Engine failed to start work item " + item.toString() +
                			". The engine returned no work items." );
                }
                return successMessage(child.toXML());
            }
            return failureMessage("No work item with id = " + workItemID);
        } catch (YAWLException e) {
            if (e instanceof YPersistenceException) {
                enginePersistenceFailure = true;
            }
            return failureMessage(e.getMessage());
        }
    }


        /**
     *
     * @param workItemID
     * @param sessionHandle
     * @return
     * @throws RemoteException
     */
    public String skipWorkItem(String workItemID, String sessionHandle) throws RemoteException {
            String sessionMessage = checkSession(sessionHandle);
            if (isFailureMessage(sessionMessage)) return sessionMessage;

        try {
            YWorkItem item = _engine.getWorkItem(workItemID);
            if (item != null) {
                YAWLServiceReference service = null;
                YSession session = _sessionCache.getSession(sessionHandle);
                if (session != null)  {
                    service = ((YServiceSession) session).getService();
                }
                YWorkItem child = _engine.skipWorkItem(item, service);
                if( child == null ) {
                	throw new YAWLException(
                			"Engine failed to skip work item " + item.toString() );
                }
                return successMessage(child.toXML());
            }
            return failureMessage("No work item with id = " + workItemID);
        } catch (YAWLException e) {
            if (e instanceof YPersistenceException) {
                enginePersistenceFailure = true;
            }
            return failureMessage(e.getMessage());
        }
    }

    /**
     * Creates a workitem sibling of of the workitemid param.  It will only do this
     * if:
     *    1) the task for workItemID is multi-instance,
     *    2) and if the task for workItemID is busy
     *    2) and if the task for workItemID allows dynamic instance creation,
     *    3) and if the number of instances has no yet reached max instances.
     *
     * @param workItemID the id of an existing instance to which to add a
     * new instance
     * @param paramValueForMICreation the data needed to create the new instance
     * @param sessionHandle session handle
     * @return an xml message indicating result.
     * @throws RemoteException
     */
    public String createNewInstance(String workItemID, String paramValueForMICreation,
                                    String sessionHandle) throws RemoteException {
        String sessionMessage = checkSession(sessionHandle);
        if (isFailureMessage(sessionMessage)) return sessionMessage;

        try {
            YWorkItem existingItem = _engine.getWorkItem(workItemID);
            YWorkItem newItem = _engine.createNewInstance(existingItem, paramValueForMICreation);
            return successMessage(newItem.toXML());
        } catch (YAWLException e) {
            if (e instanceof YPersistenceException) {
                enginePersistenceFailure = true;
            }
            return failureMessage(e.getMessage());
        }
    }


    /**
     *
     * @param sessionHandle
     * @return
     * @throws RemoteException
     */
    public String describeAllWorkItems(String sessionHandle) throws RemoteException {
        String sessionMessage = checkSession(sessionHandle);
        if (isFailureMessage(sessionMessage)) return sessionMessage;

        return describeWorkItems(_engine.getAllWorkItems());
    }


    public String getWorkItemsWithIdentifier(String idType, String itemID,
                                             String sessionHandle) throws RemoteException {
        String sessionMessage = checkSession(sessionHandle);
        if (isFailureMessage(sessionMessage)) return sessionMessage;

        return describeWorkItems(_engine.getWorkItemsWithIdentifier(idType, itemID));
    }


    /**
     *
     * @param userID
     * @param password
     * @return
     * @throws RemoteException
     */
    public String connect(String userID, String password) throws RemoteException {
        return _sessionCache.connect(userID, password);
    }


    /**
     *
     * @param sessionHandle
     * @return
     * @throws RemoteException
     */
    public String checkConnection(String sessionHandle) throws RemoteException {
        return checkSession(sessionHandle);
    }


    /**
     * @deprecated no longer valid - performs same function as 'checkConnection'
     * @param sessionHandle
     * @return either "<success/>" or "<failure><reason>...</...>"
     */
    public String checkConnectionForAdmin(String sessionHandle) {
        return checkSession(sessionHandle);
   }


    /**
     * @param specificationID
     * @param taskID
     * @param sessionHandle
     * @return
     * @throws RemoteException
     */
    public String getTaskInformation(YSpecificationID specificationID, String taskID,
                                     String sessionHandle) throws RemoteException {
        String sessionMessage = checkSession(sessionHandle);
        if (isFailureMessage(sessionMessage)) return sessionMessage;

        YTask task = _engine.getTaskDefinition(specificationID, taskID);
        if (task != null) {
            return task.getInformation();
        } else {
            return failureMessage("The was no task found with ID " + taskID);
        }
    }


    /**
     *
     * @param workItemID
     * @param sessionHandle
     * @return either "&lt;success/&gt;" or &lt;failure&gt;&lt;reason&gt;...&lt;/...&gt;
     */
    public String checkElegibilityToAddInstances(String workItemID, String sessionHandle) {
        String sessionMessage = checkSession(sessionHandle);
        if (isFailureMessage(sessionMessage)) return sessionMessage;

        try {
            _engine.checkElegibilityToAddInstances(workItemID);
            return SUCCESS;
        } catch (YStateException e) {
            return failureMessage(e.getMessage());
        }
    }


    /**
     * Gets a listing of
     * @param sessionHandle
     * @return
     * @throws RemoteException
     */
    public String getSpecificationList(String sessionHandle) throws RemoteException {
        String sessionMessage = checkSession(sessionHandle);
        if (isFailureMessage(sessionMessage)) return sessionMessage;

        Set<YSpecificationID> specIDs = _engine.getSpecIDs();
        Set<YSpecification> specs = new HashSet<YSpecification>();
        for (YSpecificationID ySpecID : specIDs) {
            specs.add(_engine.getSpecification(ySpecID));
        }
        return getDataForSpecifications(specs);
    }


   /**
    *
    * @param specID specID
    * @param caseParams format &lt;data&gt;[InputParam]*&lt;/data&gt; where
    * InputParam == &lt;varName&gt;var value&lt;/varName&gt;
    * @param caseID caseID
    * @param sessionHandle
    * @return the case id of the launched case, or a diagnostic <failure/> msg.
    */
	public String launchCase(YSpecificationID specID, String caseParams, URI caseCompletionURI, 
                           String caseID, String logDataStr, String sessionHandle){
       String sessionMessage = checkSession(sessionHandle);
       if (isFailureMessage(sessionMessage)) return sessionMessage;

        try {
            YLogDataItemList logData = new YLogDataItemList(logDataStr);
            return _engine.launchCase(specID, caseParams, caseCompletionURI, caseID,
                                      logData, sessionHandle);
        } catch (YAWLException e) {
            if (e instanceof YPersistenceException) {
                enginePersistenceFailure = true;
            }
            return failureMessage(e.getMessage());
        }
	}    
    

    /**
     *
     * @param specID specID
     * @param caseParams format &lt;data&gt;[InputParam]*&lt;/data&gt; where
     * InputParam == &lt;varName&gt;var value&lt;/varName&gt;
     * @param sessionHandle
     * @return the case id of the launched case, or a diagnostic <failure/> msg.
     */
    public String launchCase(YSpecificationID specID, String caseParams,
                             URI caseCompletionURI, String logDataStr, String sessionHandle) {
        String sessionMessage = checkSession(sessionHandle);
        if (isFailureMessage(sessionMessage)) return sessionMessage;

        try {
            YLogDataItemList logData = new YLogDataItemList(logDataStr);
            return _engine.launchCase(specID, caseParams, caseCompletionURI, logData, sessionHandle);
        } catch (YAWLException e) {
            if (e instanceof YPersistenceException) {
                enginePersistenceFailure = true;
            }
            return failureMessage(e.getMessage());
        }
    }
    

    /**
     * Given a process specification id return the cases that are its running
     * instances.
     * @param specID the process specification id string.
     * @param sessionHandle the sessionhandle
     * @return a XML message containing a set of caseID elements
     * that are run time instances of the process specification
     * with id = specID
     */
    public String getCasesForSpecification(YSpecificationID specID, String sessionHandle) {
        String sessionMessage = checkSession(sessionHandle);
        if (isFailureMessage(sessionMessage)) return sessionMessage;

        Set<YIdentifier> caseIDs = _engine.getCasesForSpecification(specID);
        StringBuilder result = new StringBuilder();
        for (YIdentifier caseID : caseIDs) {
            result.append("<caseID>");
            result.append(caseID.toString());
            result.append("</caseID>");
        }
        return result.toString();
    }


    /**
     * This method returns a complex XML message containing the state of a particular
     * case.  i.e. every token (identifier) and its position in every
     * condition/ task/ and internal condition.
     * @param caseID case id string
     * @param sessionHandle sessionHandle
     * @return XML state Message
     * @throws RemoteException
     */
    public String getCaseState(String caseID, String sessionHandle) throws RemoteException {
        String sessionMessage = checkSession(sessionHandle);
        if (isFailureMessage(sessionMessage)) return sessionMessage;

        YIdentifier id = _engine.getCaseID(caseID);
        if (id != null) {
            return _engine.getStateForCase(id);
        }
        return failureMessage("Case [" + caseID + "] not found.");
    }


    /**
     * Cancels a running case.
     * @param caseID the caseID string
     * @param sessionHandle sessionHandle
     * @return a diagnostic XML message indicating the result of method call.
     */
    public String cancelCase(String caseID, String sessionHandle) {
        String sessionMessage = checkSession(sessionHandle);
        if (isFailureMessage(sessionMessage)) return sessionMessage;

        YIdentifier id = _engine.getCaseID(caseID);
        if (id != null) {
            try {
                _engine.cancelCase(id, sessionHandle);
                return SUCCESS;
            }
            catch (YPersistenceException e) {
                enginePersistenceFailure = true;
                return failureMessage(e.getMessage());
            }
            catch (YEngineStateException e) {
                enginePersistenceFailure = false;
                return failureMessage(e.getMessage());
            }
        }
        return failureMessage("Case [" + caseID + "] not found.");
    }


    /**
     * Gets the child work items of a given work item id string.
     * @param workItemID
     * @param sessionHandle
     * @return an XML list of elements that describe each child work item.
     * @throws RemoteException
     */
    public String getChildrenOfWorkItem(String workItemID, String sessionHandle) throws RemoteException {
        String sessionMessage = checkSession(sessionHandle);
        if (isFailureMessage(sessionMessage)) return sessionMessage;

        YWorkItem item = _engine.getWorkItem(workItemID);
        return describeWorkItems(_engine.getChildrenOfWorkItem(item));
    }


    /**
     * Provides an XML list of options for manipulating a work item.  Uses the REST
     * philosophy of showing what you can do with a work item
     * and provides some links to further manipulate the work item.
     * @param workItemID work item id string
     * @param thisURL the url of the engine interface B server (i think).
     * @param sessionHandle the sesssion handle
     * @return a REST style list of options to change the work item under question.
     */
    public String getWorkItemOptions(String workItemID, String thisURL, String sessionHandle) {
        String sessionMessage = checkSession(sessionHandle);
        if (isFailureMessage(sessionMessage)) return sessionMessage;

        StringBuilder options = new StringBuilder();
        YWorkItem workItem = _engine.getWorkItem(workItemID);
        if (workItem != null) {
            if (workItem.getStatus().equals(YWorkItemStatus.statusExecuting)) {
                options.append("<option operation=\"suspend\">" +
                        "<documentation>Suspend the currently active workItem</documentation>" +
                        "<style>post</style>" +
                        "<url>").append(thisURL).append("?action=suspend</url></option>");
                options.append("<option operation=\"complete\">" +
                        "<documentation>Notify the engine that the work item is complete</documentation>" +
                        "<style>post</style>" + "<url>").append(thisURL).append("?action=complete</url>" +
                        "<bodyContent>Any return data needed for this work item.</bodyContent>" +
                        "</option>");
            }
            try {
                _engine.checkElegibilityToAddInstances(workItemID);
                options.append("<option operation=\"addNewInstance\">" +
                        "<documentation>Add a new Instance similar to this work item</documentation>" +
                        "<style>post</style>" + "<url>").
                        append(thisURL).
                        append("?action=createInstance</url>" +
                                "<bodyContent>The data for the new instance.</bodyContent>" +
                                "</option>");
            } catch (YAWLException e) {
                //just don't provide that option.
                if (e instanceof YPersistenceException) {
                    enginePersistenceFailure = true;
                }
            }
            if (workItem.getStatus().equals(YWorkItemStatus.statusEnabled)
                    || workItem.getStatus().equals(YWorkItemStatus.statusFired)) {
                options.append("<option operation=\"start\">" +
                        "<documentation>Starts a work item</documentation>" +
                        "<style>post</style>" + "<url>").
                        append(thisURL).
                        append("?action=startOne&amp;user=[userID]</url>" +
                                "<returns>The data provided by the engine for " +
                                "processing the work item.</returns>" +
                                "</option>");
            }
        }
        return options.toString();
    }


    /**
     * Allows the user to load a specificationStr.
     * @param specificationStr a YAWL schema compliant process specificationStr
     * in its entirety, in string format.
     * @param sessionHandle a session handle
     * @return a diagnostic XML message indicating the result of loading the
     * specificationStr.
     */
    public String loadSpecification(String specificationStr, String sessionHandle) {
        String sessionMessage = checkSession(sessionHandle);
        if (isFailureMessage(sessionMessage)) return sessionMessage;

        List<YVerificationMessage> errorMessages = new ArrayList<YVerificationMessage>();
        try {
            _engine.addSpecifications(specificationStr, false, errorMessages);
        } catch (Exception e) {
            if (e instanceof YPersistenceException) {
                enginePersistenceFailure = true;
            }
            e.printStackTrace();
            return failureMessage(e.getMessage());
        }

        if (errorMessages.size() > 0) {
            String status;
            StringBuilder errorMsg = new StringBuilder();
            errorMsg.append(OPEN_FAILURE);
            for (YVerificationMessage message : errorMessages) {
                status =  message.getStatus().equals(YVerificationMessage.ERROR_STATUS) ?
                    "error" : "warning";

                errorMsg.append("<" + status + ">");
                Object src = message.getSource();
                if (src instanceof YTask) {
                    YDecomposition decomp = ((YTask) src).getDecompositionPrototype();
                    if (decomp != null) {
                        errorMsg.append("<src>").
                                append(decomp.getName() != null ?
                                        decomp.getName() : decomp.getID()).
                                append("</src>");
                    }
                }
                errorMsg.append("<message>").
                        append(message.getMessage()).
                        append("</message>").
                        append("</" + status + ">");

            }
            errorMsg.append(CLOSE_FAILURE);
            return errorMsg.toString();
        } else {
            return SUCCESS;
        }
    }


    /**
     * Unloads the specification.
     * @param specID the specification id string
     * @param sessionHandle session handle
     * @return an XML message indicating the result of the operation.
     */
    public String unloadSpecification(YSpecificationID specID, String sessionHandle) {
        String sessionMessage = checkSession(sessionHandle);
        if (isFailureMessage(sessionMessage)) return sessionMessage;

        try {
            _engine.unloadSpecification(specID);
            return SUCCESS;
        } catch (YAWLException e) {
            if (e instanceof YPersistenceException) {
                enginePersistenceFailure = true;
            }
            return failureMessage(e.getMessage());
        }
    }


    /**
     * Creates a new external client account in the system.
     * @param userName the name of the user
     * @param password the users elected password.
     * @param doco some descriptive text about the account
     * @param sessionHandle
     * @return diagnostic XML message.
     */
    public String createAccount(String userName, String password, String doco, String sessionHandle) {
        String sessionMessage = checkSession(sessionHandle);
        if (isFailureMessage(sessionMessage)) return sessionMessage;

        try {
            _engine.addExternalClient(new YExternalClient(userName, password, doco));
            return SUCCESS;
        }
        catch (YPersistenceException ype) {
            return failureMessage("Persistence exception attempting to create account");
        }
    }


    /**
     * Gets the list of external accounts in the system.
     * @param sessionHandle session handle
     * @return an XML message showing each user in the system.
     */
    public String getAccounts(String sessionHandle) {
        String sessionMessage = checkSession(sessionHandle);
        if (isFailureMessage(sessionMessage)) return sessionMessage;

        Set<YExternalClient> clients = _engine.getExternalClients();
        StringBuilder result = new StringBuilder();
        for (YExternalClient client : clients) {
            result.append(client.toXML());
        }
        return result.toString();
    }


    /**
     * Returns an XML list (unrooted) of yawlService elements.
     * @param sessionHandle
     * @return either "<success/>" or "<failure><reason>...</...>"
     */
    public String getYAWLServices(String sessionHandle) {
        String sessionMessage = checkSession(sessionHandle);
        if (isFailureMessage(sessionMessage)) return sessionMessage;

        Set<YAWLServiceReference> yawlServices = _engine.getYAWLServices();
        StringBuilder result = new StringBuilder();
        for (YAWLServiceReference service : yawlServices) {
            result.append(service.toXMLComplete());
        }
        return result.toString();
    }


    /**
     * Gets the documentation associated with the yawl service
     * @param yawlServiceURI
     * @param sessionHandle
     * @return doco as xml (if any) else failure message
     */
    public String getYAWLServiceDocumentation(String yawlServiceURI, String sessionHandle) {
        String sessionMessage = checkSession(sessionHandle);
        if (isFailureMessage(sessionMessage)) return sessionMessage;

        YAWLServiceReference service = _engine.getRegisteredYawlService(yawlServiceURI);
        if (null != service) {
            if (service.getDocumentation() != null) {
                return service.getDocumentation();
            } else {
                return failureMessage("Yawl service [" + yawlServiceURI + "]" +
                        " has no documentation.");
            }
        } else {
            return  failureMessage("Yawl service [" + yawlServiceURI + "] " +
                    "not found.");
        }
    }


    /**
     * Adds a new YAWL service to the engine
     * @param serviceStr an XML message containing the YAWL service details.
     * @param sessionHandle the session handle
     * @return diagnostic XML message.
     */
    public String addYAWLService(String serviceStr, String sessionHandle) {
        String sessionMessage = checkSession(sessionHandle);
        if (isFailureMessage(sessionMessage)) return sessionMessage;

        YAWLServiceReference service = YAWLServiceReference.unmarshal(serviceStr);
        if (null != service) {
            if (null == _engine.getRegisteredYawlService(service.getURI())) {
                if (HttpURLValidator.validate(service.getURI()).startsWith("<success")) {
                    try {
                        _engine.addYawlService(service);
                        return SUCCESS;
                    } catch (YPersistenceException e) {
                        enginePersistenceFailure = true;
                        return failureMessage(e.getMessage());
                    }
                }
                else {
                    return failureMessage("Service unresponsive: " + service.getURI());
                }
            } else {
                return failureMessage("Engine has already registered a service with " +
                        "the same URI [" + service.getURI() + "]");
            }
        } else {
            return failureMessage("Failed to parse yawl service from [" +
                    serviceStr + "]");
        }
    }

    public String removeYAWLService(String serviceURI, String sessionHandle) {
        String sessionMessage = checkSession(sessionHandle);
        if (isFailureMessage(sessionMessage)) return sessionMessage;

        YAWLServiceReference service = _engine.getRegisteredYawlService(serviceURI);
        if (null != service) {
            try {
                YAWLServiceReference ys = _engine.removeYawlService(serviceURI);
                if (null != ys) {
                    return SUCCESS;
                }
            } catch (YPersistenceException e) {
                enginePersistenceFailure = true;
                return failureMessage(e.getMessage());
            }
        }
        return failureMessage("Engine does not contain this YAWL" +
                " service [ " + serviceURI + " ]");

    }

    /**
     * @deprecated The YAWL Engine no longer maintains users directly
     * @param client
     * @param sessionHandle
     * @return
     * @throws RemoteException
     */
    public String deleteAccount(String client, String sessionHandle) throws RemoteException {
        YSession session = _sessionCache.getSession(sessionHandle);
        if (session != null) {
            if ((session instanceof YExternalSession) && session.getName().equals(client)) {
                return failureMessage("Deletion of own account not allowed");
            }
            if (_engine.getExternalClient(client) != null) {
                try {
                    _engine.removeExternalClient(client);
                    return SUCCESS;
                }
                catch (YPersistenceException ype) {
                    return failureMessage("Persistence exception removing client account");
                }
            }
            else return failureMessage("Unknown client account name: " + client);
        }
        else return failureMessage("Invalid or expired session handle");
    }


    public String changePassword(String password, String sessionHandle) throws RemoteException {
        YSession session = _sessionCache.getSession(sessionHandle);
        if (session != null) {
            try {
                session.setPassword(password);
                return SUCCESS;
            }
            catch (YPersistenceException ype) {
                return failureMessage("Password could not be set due to persistence exception");
            }
        }
        else return failureMessage("Invalid or expired session handle");
    }


    private String describeWorkItems(Set<YWorkItem> workItems) {
        StringBuilder result = new StringBuilder();
        if (workItems != null) {
            for (YWorkItem workitem : workItems) {
                result.append(workitem.toXML());
            }
        }    
        return result.toString();
    }


    private String getDataForSpecifications(Set<YSpecification> specSet) {
        StringBuilder specs = new StringBuilder();
        for (YSpecification spec : specSet) {
            specs.append("<specificationData>");
            specs.append(StringUtil.wrap(spec.getURI(), "uri"));

            if (spec.getID() != null) {
                specs.append(StringUtil.wrap(spec.getID(), "id"));
            }
            if (spec.getName() != null) {
                specs.append(StringUtil.wrap(spec.getName(), "name"));
            }
            if (spec.getDocumentation() != null) {
                specs.append(StringUtil.wrap(spec.getDocumentation(), "documentation"));
            }
            
            Iterator inputParams = spec.getRootNet().getInputParameters().values().iterator();
            if (inputParams.hasNext()) {
                specs.append("<params>");
                while (inputParams.hasNext()) {
                    YParameter inputParam = (YParameter) inputParams.next();
                    specs.append(inputParam.toSummaryXML());
                }
                specs.append("</params>");
            }
            specs.append(StringUtil.wrap(spec.getRootNet().getID(), "rootNetID"));
            specs.append(StringUtil.wrap(spec.getSchemaVersion(),"version"));
            specs.append(StringUtil.wrap(spec.getSpecVersion(), "specversion"));
            specs.append(StringUtil.wrap(_engine.getLoadStatus(spec.getSpecificationID()),
                         "status"));

            specs.append("</specificationData>");
        }
        return specs.toString();
    }

    /***************************************************************************/

    /** The following methods are called by an Exception Service via Interface_X */

    public String setExceptionObserver(String observerURI) {
       if (_engine.setExceptionObserver(observerURI))
           return SUCCESS;
       else
           return failureMessage("setExceptionObserver failed") ;
    }


    public String removeExceptionObserver() {
       _engine.removeExceptionObserver();
        return SUCCESS;
    }


    public String updateWorkItemData(String workItemID, String data, String sessionHandle) {
        String sessionMessage = checkSession(sessionHandle);
        if (isFailureMessage(sessionMessage)) return sessionMessage;

        return String.valueOf(_engine.updateWorkItemData(workItemID, data));
    }


    public String updateCaseData(String caseID, String data, String sessionHandle) {
        String sessionMessage = checkSession(sessionHandle);
        if (isFailureMessage(sessionMessage)) return sessionMessage;

        return String.valueOf(_engine.updateCaseData(caseID, data));
    }


    public String restartWorkItem(String workItemID, String sessionHandle) throws RemoteException {
        String sessionMessage = checkSession(sessionHandle);
        if (isFailureMessage(sessionMessage)) return sessionMessage;

        String result = "";
        YWorkItem item = _engine.getWorkItem(workItemID);
        if (item != null) {
            item.setStatus(YWorkItemStatus.statusEnabled);
            result = startWorkItem(workItemID, sessionHandle);
        }
        return result ;
    }


    public String cancelWorkItem(String workItemID, String fail, String sessionHandle)
                                                                 throws RemoteException {
        String sessionMessage = checkSession(sessionHandle);
        if (isFailureMessage(sessionMessage)) return sessionMessage;

        YWorkItem item = _engine.getWorkItem(workItemID);
        _engine.cancelWorkItem(item, fail.equalsIgnoreCase("true")) ;
        return SUCCESS ;
    }


    public String getLatestSpecVersion(String id, String sessionHandle) throws RemoteException {
        String sessionMessage = checkSession(sessionHandle);
        if (isFailureMessage(sessionMessage)) return sessionMessage;

        YSpecification spec = _engine.getLatestSpecification(id) ;
        return spec.getSpecificationID().getVersionAsString();
    }


    /***************************************************************************/

        /**
     * @param specificationID
     * @param taskID
     * @param sessionHandle
     * @return
     * @throws RemoteException
     */
    public String getMITaskAttributes(YSpecificationID specificationID, String taskID,
                                      String sessionHandle) throws RemoteException {
        String sessionMessage = checkSession(sessionHandle);
        if (isFailureMessage(sessionMessage)) return sessionMessage;

        YTask task = _engine.getTaskDefinition(specificationID, taskID);
        if (task != null) {
            if (task.isMultiInstance())
                return task.getMultiInstanceAttributes().toXML() ;
            else
                return failureMessage(taskID + " is not a multi-instance task");
        }
        else
            return failureMessage("The was no task found with ID " + taskID);
    }

    /***************************************************************************/


    public String getResourcingSpecs(YSpecificationID specificationID, String taskID,
                                     String sessionHandle) throws RemoteException {
        String sessionMessage = checkSession(sessionHandle);
        if (isFailureMessage(sessionMessage)) return sessionMessage;

        YTask task = _engine.getTaskDefinition(specificationID, taskID);
        if (task != null)
            return JDOMUtil.elementToStringDump(task.getResourcingSpecs());
        else
            return failureMessage("Unable to get resourcing information for task: " +
                                    taskID) ;
    }

    /***************************************************************************/

    public String getCaseData(String caseID, String sessionHandle) throws RemoteException {
        String sessionMessage = checkSession(sessionHandle);
        if (isFailureMessage(sessionMessage)) return sessionMessage;

        // if the case id is of a sub-net, get the sub-net's data instead
        if (caseID.indexOf(".") > -1) {
            try {
                return _engine.getNetData(caseID);
            }
            catch (YStateException yse) {
                return failureMessage(yse.getMessage());
            }
        }

        if (_engine.getCaseID(caseID) != null) {
            Document caseData = _engine.getCaseDataDocument(caseID);
            if (caseData != null)
                return JDOMUtil.elementToString(caseData.getRootElement());
            else
                return failureMessage("Could not retrieve case data from engine or " +
                                      "case data has a null value");
        }
        else return failureMessage("There is no active case with id: " + caseID);
    }


    public String getCaseInstanceSummary(String sessionHandle) throws RemoteException {
        String sessionMessage = checkSession(sessionHandle);
        if (isFailureMessage(sessionMessage)) return sessionMessage;

        return _engine.getInstanceCache().marshalCases();
    }


    public String getWorkItemInstanceSummary(String caseID, String sessionHandle)
            throws RemoteException {
        String sessionMessage = checkSession(sessionHandle);
        if (isFailureMessage(sessionMessage)) return sessionMessage;

        return _engine.getInstanceCache().marshalWorkItems(caseID);
    }


    public String getParameterInstanceSummary(String caseID, String itemID, String sessionHandle)
            throws RemoteException {
        String sessionMessage = checkSession(sessionHandle);
        if (isFailureMessage(sessionMessage)) return sessionMessage;

        return _engine.getInstanceCache().marshalParameters(caseID, itemID);
    }

}
