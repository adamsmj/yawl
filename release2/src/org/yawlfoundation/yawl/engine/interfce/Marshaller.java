/*
 * This file is made available under the terms of the LGPL licence.
 * This licence can be retrieved from http://www.gnu.org/copyleft/lesser.html.
 * The source remains the property of the YAWL Foundation.  The YAWL Foundation is a collaboration of
 * individuals and organisations who are committed to improving workflow technology.
 *
 */


package org.yawlfoundation.yawl.engine.interfce;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.output.XMLOutputter;
import org.yawlfoundation.yawl.elements.YAttributeMap;
import org.yawlfoundation.yawl.elements.data.YParameter;
import org.yawlfoundation.yawl.engine.YSpecificationID;
import org.yawlfoundation.yawl.unmarshal.YDecompositionParser;
import org.yawlfoundation.yawl.util.JDOMUtil;

import java.io.IOException;
import java.io.StringReader;
import java.util.*;


/**
 *
 * @author Lachlan Aldred
 * Date: 16/02/2004
 * Time: 18:41:17
 *
 */
public class Marshaller {

    public static String getOutputParamsInXML(YParametersSchema paramSchema,
                                              String dataSpaceRootElementNm) {
        StringBuilder result = new StringBuilder();

        result.append("<").append(dataSpaceRootElementNm).append(">");
        if (paramSchema != null) {
            List<YParameter> params = paramSchema.getOutputParams();
            for (YParameter param : params) {
                result.append(presentParam(param));
            }
        }
        result.append("</").append(dataSpaceRootElementNm).append(">");
        return result.toString();
    }


    public static String presentParam(YParameter param) {

        boolean typedParam = param.getDataTypeName() != null;
        StringBuilder result = new StringBuilder();
        result.append("\n  <!--");
        if (typedParam) {
            result.append("Data Type:     " + param.getDataTypeName());
        }
        result.append("\n      Is Mandatory:  " + param.isMandatory());
        if (param.getDocumentation() != null) {
            result.append("\n      Documentation: " + param.getDocumentation());
        }
        result.append("-->\n  ");
        if (typedParam || param.isUntyped()) {
            result.append("<" + param.getName() + "></" + param.getName() + ">");
        } else {
            result.append("<" + param.getElementName() + "></" + param.getElementName() + ">");
        }
        return result.toString();
    }


    public static TaskInformation unmarshalTaskInformation(String taskInfoAsXML) {
        YParametersSchema paramsForTaskNCase = new YParametersSchema();
        YAttributeMap attributemap = new YAttributeMap();

        Element taskInfo = JDOMUtil.stringToElement(taskInfoAsXML);

        // if the string was encased in response tags, go down one level
        if (taskInfoAsXML.startsWith("<response>")) {
            taskInfo = taskInfo.getChild("taskInfo");
        }
        String taskID = taskInfo.getChildText("taskID");
        String taskName = taskInfo.getChildText("taskName");
        String taskDocumentation = taskInfo.getChildText("taskDocumentation");
        String decompositionID = taskInfo.getChildText("decompositionID");
        Element yawlService = taskInfo.getChild("yawlService");
    
        Element specElem = taskInfo.getChild("specification");
        String specIdentifier = specElem.getChildText("identifier");
        String specVersion = specElem.getChildText("version");
        String specURI = specElem.getChildText("uri");
        YSpecificationID specificationID = new YSpecificationID(specIdentifier, specVersion, specURI);
    
        Element attributes = taskInfo.getChild("attributes");
        if (attributes != null) {
            List attributelist = attributes.getChildren();
            for (Object o : attributelist) {
                Element attribute = (Element) o;
                attributemap.put(attribute.getName(),
                        attributes.getChildText(attribute.getName()));
            }
        }

        Element params = taskInfo.getChild("params");
        List paramElementsList = params.getChildren();
        for (Object o : paramElementsList) {
            Element paramElem = (Element) o;
            if ("formalInputParam".equals(paramElem.getName())) {
                paramsForTaskNCase.setFormalInputParam(paramElem.getText());
                continue;
            }
            YParameter param = new YParameter(null, paramElem.getName());
            YDecompositionParser.parseParameter(
                    paramElem,
                    param,
                    null,
                    false);
            if (param.isInput()) {
                paramsForTaskNCase.setInputParam(param);
            } else {
                paramsForTaskNCase.setOutputParam(param);
            }
            String paramOrdering = paramElem.getChildText("ordering");
            if (paramOrdering != null) {
                int order = Integer.parseInt(paramOrdering);
                param.setOrdering(order);
            }
        }

        TaskInformation taskInformation = new TaskInformation(
                paramsForTaskNCase,
                taskID,
                specificationID,
                taskName,
                taskDocumentation,
                decompositionID);

        taskInformation.setAttributes(attributemap);

        return taskInformation;
    }


    /**
     * Creates a list of SpecificationDatas from formatted XML.
     * These are brief meta data summary
     * information objects that describe a worklfow specification.
     * @param specificationSummaryListXML
     * @return  the list
     */
    public static List<SpecificationData> unmarshalSpecificationSummary(
            String specificationSummaryListXML) {
        List<SpecificationData> specSummaryList = new ArrayList<SpecificationData>();

        Element specElem = JDOMUtil.stringToElement(specificationSummaryListXML);
        List specSummaryElements = specElem.getChildren();
        for (Object o : specSummaryElements) {
            Element specElement = (Element) o;
            String specID = specElement.getChildText("id");
            String specURI = specElement.getChildText("uri");
            String specName = specElement.getChildText("name");
            String specDoco = specElement.getChildText("documentation");
            String specStatus = specElement.getChildText("status");
            String version = specElement.getChildText("version");
            String rootNetID = specElement.getChildText("rootNetID");
            String specVersion = specElement.getChildText("specversion");
            
            if (specURI != null && specStatus != null) {
                YSpecificationID ySpecID = new YSpecificationID(specID, specVersion, specURI);
                SpecificationData specData = new SpecificationData(ySpecID,
                            specName, specDoco, specStatus, version);
                
                specData.setRootNetID(rootNetID);
                specSummaryList.add(specData);
                Element inputParams = specElement.getChild("params");
                if (inputParams != null) {
                    List paramElements = inputParams.getChildren();
                    for (Object p :paramElements) {
                        Element paramElem = (Element) p;
                        YParameter param = new YParameter(null, YParameter._INPUT_PARAM_TYPE);
                        YDecompositionParser.parseParameter(
                                paramElem,
                                param,
                                null,
                                false);//todo check correctness
                        specData.addInputParam(param);
                    }
                }
            }
        }
        return specSummaryList;
    }


    public static WorkItemRecord unmarshalWorkItem(String workItemXML) {
        return unmarshalWorkItem(JDOMUtil.stringToElement(workItemXML));
    }


    public static WorkItemRecord unmarshalWorkItem(Element workItemElement) {
        if (workItemElement == null) return null;
        
        WorkItemRecord wir;
        String status = workItemElement.getChildText("status");
        String caseID = workItemElement.getChildText("caseid");
        String taskID = workItemElement.getChildText("taskid");
        String specURI = workItemElement.getChildText("specuri");
        String enablementTime = workItemElement.getChildText("enablementTime");
        if (caseID != null && taskID != null && specURI != null &&
            enablementTime != null && status != null) {

            wir = new WorkItemRecord(caseID, taskID, specURI, enablementTime, status);

            wir.setExtendedAttributes(unmarshalWorkItemAttributes(workItemElement));
            wir.setUniqueID(workItemElement.getChildText("uniqueid"));
            wir.setAllowsDynamicCreation(workItemElement.getChildText(
                                                              "allowsdynamiccreation"));
            wir.setRequiresManualResourcing(workItemElement.getChildText(
                                                           "requiresmanualresourcing"));
            wir.setCodelet(workItemElement.getChildText("codelet"));
            wir.setDeferredChoiceGroupID(workItemElement.getChildText(
                                                              "deferredChoiceGroupid"));
            wir.setSpecVersion(workItemElement.getChildText("specversion"));
            wir.setFiringTime(workItemElement.getChildText("firingTime"));
            wir.setStartTime(workItemElement.getChildText("startTime"));
            wir.setCompletionTimeMs(workItemElement.getChildText("completionTime"));
            wir.setEnablementTimeMs(workItemElement.getChildText("enablementTimeMs"));
            wir.setFiringTimeMs(workItemElement.getChildText("firingTimeMs"));
            wir.setStartTimeMs(workItemElement.getChildText("startTimeMs"));
            wir.setCompletionTimeMs(workItemElement.getChildText("completionTimeMs"));
            wir.setTimerTrigger(workItemElement.getChildText("timertrigger"));
            wir.setTimerExpiry(workItemElement.getChildText("timerexpiry"));
            wir.setStartedBy(workItemElement.getChildText("startedBy"));
            wir.setTag(workItemElement.getChildText("tag"));
            wir.setCustomFormURL(workItemElement.getChildText("customform"));

            String specid = workItemElement.getChildText("specidentifier") ;
            if (specid != null) wir.setSpecIdentifier(specid);

            String resStatus = workItemElement.getChildText("resourceStatus");
            if (resStatus != null) wir.setResourceStatus(resStatus);
            
            String edited = workItemElement.getChildText("edited") ;
            if (edited != null)
                wir.setEdited(edited.equalsIgnoreCase("true"));

            Element data = workItemElement.getChild("data");
            if ((null != data) && (data.getContentSize() > 0))
                   wir.setDataList((Element) data.getContent(0));

            Element updateddata = workItemElement.getChild("updateddata");
            if ((null != updateddata) && (updateddata.getContentSize() > 0))
                   wir.setUpdatedData((Element) updateddata.getContent(0));

            Element logPredicate = workItemElement.getChild("logPredicate");
            if (logPredicate != null) {
                wir.setLogPredicateStarted(logPredicate.getChildText("start"));
                wir.setLogPredicateCompletion(logPredicate.getChildText("completion"));
            }
                        
            return wir;
        }
        throw new IllegalArgumentException("Input element could not be parsed.");
    }


    public static Hashtable<String, String> unmarshalWorkItemAttributes(Element item) {
        YAttributeMap result = new YAttributeMap();
        result.fromJDOM(item.getAttributes());
        return result ;
    }

    public static List<String> unmarshalCaseIDs(String casesAsXML) {
        List<String> cases = new ArrayList<String>();

        Element casesElem = JDOMUtil.stringToElement(casesAsXML);
        List caseList = casesElem.getChildren();
        for (Object o : caseList) {
            Element caseElem = (Element) o;
            String caseID = caseElem.getText();
            if (caseID != null) {
                cases.add(caseID);
            }
        }

        return cases;
    }


    public static String getMergedOutputData(Element inputData, Element outputData) {
        try {
            Element merged = (Element) inputData.clone();
            JDOMUtil.stripAttributes(merged);
            JDOMUtil.stripAttributes(outputData);

            List children = outputData.getChildren();

            //iterate through the output vars and add them to the merged doc.
            for (int i = children.size() - 1; i >= 0; i--) {
                Object o = children.get(i);
                if (o instanceof Element) {
                    Element child = (Element) o;
                    child.detach();
           //         child.setAttributes(null);
                    //the input data will be removed from the merged doc and
                    //the output data will be added.
                    merged.removeChild(child.getName());
                    merged.addContent(child);
                }
            }
            return JDOMUtil.elementToString(merged);
        }
        catch (Exception e) {
            return "";
        }
    }

    
    public static String filterDataAgainstOutputParams(String mergedOutputData,
                                                       List outputParams) throws JDOMException, IOException {
        //build the merged output data document
        SAXBuilder builder = new SAXBuilder();
        Document internalDecompDoc = builder.build(new StringReader(mergedOutputData));

        //set up output document
        String rootElemntName = internalDecompDoc.getRootElement().getName();
        Document outputDoc = new Document();
        Element rootElement = new Element(rootElemntName);
        outputDoc.setRootElement(rootElement);

        Collections.sort(outputParams); //make sure its sorted
        for (Iterator iterator = outputParams.iterator(); iterator.hasNext();) {
            YParameter parameter = (YParameter) iterator.next();
            String varElementName =
                    parameter.getName() != null ?
                    parameter.getName() : parameter.getElementName();

            Element child = internalDecompDoc.getRootElement().getChild(varElementName);
            if (null != child) {
                Element clone = (Element) child.clone();
                outputDoc.getRootElement().addContent(clone);
            }
        }
        return new XMLOutputter().outputString(outputDoc);
    }


}
