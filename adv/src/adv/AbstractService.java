

/*******************************************************************************
 * Licensed Materials - Property of IBM
 * (c) Copyright IBM Corporation 2010. All Rights Reserved. 
 * 
 * Note to U.S. Government Users Restricted Rights:  Use, 
 * duplication or disclosure restricted by GSA ADP Schedule 
 * Contract with IBM Corp.
 *******************************************************************************/

package adv;

import org.eclipse.core.runtime.IProgressMonitor;

import com.ibm.team.workitem.common.internal.SaveParameter;
import com.ibm.team.workitem.common.ISaveParameter;
import com.ibm.team.workitem.common.IWorkItemCommon;
import com.ibm.team.workitem.common.model.IWorkItem;
import com.ibm.team.workitem.common.model.Identifier;
import com.ibm.team.workitem.common.model.IState;
import com.ibm.team.workitem.common.model.IAttribute;

import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.HttpClient;
import org.apache.http.HttpResponse;



//import org.json.JSONObject;
//import org.json.JSONArray;
//import org.json.JSONException;
//import org.json.JSONString;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.ibm.team.json.JSONArray;
import com.ibm.team.json.JSONObject;
import com.ibm.team.process.common.IProcessConfigurationElement;
import com.ibm.team.process.common.advice.AdvisableOperation;
import com.ibm.team.process.common.advice.IAdvisorInfo;
import com.ibm.team.process.common.advice.IAdvisorInfoCollector;
import com.ibm.team.process.common.advice.runtime.IOperationAdvisor;
import com.ibm.team.repository.common.IAuditable;
import com.ibm.team.repository.common.TeamRepositoryException;
import com.ibm.team.workitem.common.IWorkItemCommon;
import com.ibm.team.workitem.service.IWorkItemServer;

public class AbstractService extends com.ibm.team.repository.service.AbstractService implements IOperationAdvisor {
	
	public static String NEXUS_TAG = "";
	public static String NEXUS_GID = "NEXUS_GID";
	public static String NEXUS_VER = "NEXUS_VER";
	public static String NEXUS_ARTIFACT_NAME = "NEXUS_ARTIFACT_NAME";
	public static String NEXUS_CLASSIFIER = "NEXUS_CLASSIFIER";
	public static String NEXUS_EXTENSION = "NEXUS_EXTENSION";
	public static String NEXUS_KISAM_CR = "NEXUS_KISAM_CR";
	public static String WORKITEM_ID = "WORKITEM_ID";

	public static String NEW_TBI_STATE = "";

	private static final String WORKITEM_ATTRIBUTE_RELEASE_ID = "gov.irs.team.workitem.attribute.releaseID";
	private static final String WORKITEM_ATTRIBUTE_GROUP_ID = "gov.irs.team.workitem.attribute.groupID";
	private static final String WORKITEM_ATTRIBUTE_VERSION = "gov.irs.team.workitem.attribute.artifactVersion";
	private static final String WORKITEM_ATTRIBUTE_ARTIFACT_NAME = "gov.irs.team.workitem.attribute.artifact";
	private static final String WORKITEM_ATTRIBUTE_CLASSIFIER = "gov.irs.team.workitem.attribute.artifactClassifier";
	private static final String WORKITEM_ATTRIBUTE_EXTENSION = "gov.irs.team.workitem.attribute.artifactExtension";
	private static final String WORKITEM_ATTRIBUTE_KISAM_CR = "gov.irs.team.workitem.attribute.kisamcr";
	private static final String WORKITEM_ATTRIBUTE_ID = "id";
	
	private static final String NON_IRS_NEXUS_INSTANCE_URL = "nexus.lab.zivra.com:8081";
	//private static final String IRS_NEXUS_INSTANCE_URL = "vl2smemappcssr3.dstest.irsnet.gov:8081";
	private static final String IRS_NEXUS_INSTANCE_URL = "10.219.175.183:8081";
	private static final String NON_IRS_NEXUS_RELEASE_CANDIDATE_REPO = "IRS_ReleaseCandidate";
	private static final String NON_IRS_NEXUS_CERTIFIED_REPO = "IRS_Certified";
	private static final String NON_IRS_NEXUS_PRODUCTION_REPO = "IRS_Production";
	private static final String IRS_NEXUS_RELEASE_CANDIDATE_REPO = "release-candidates";
	private static final String IRS_NEXUS_CERTIFIED_REPO = "certified";
	private static final String IRS_NEXUS_PRODUCTION_REPO = "production";
	public static final String IRSnexusCredentialsEncoded = "Z2xqdGI6Z2xqdGI=";
	public static final String nexusCredentialsEncoded = "YWRtaW46Wml2cmExMjM0IQ==";
	
	private static final String CREATE_TRACED_WORKITEMS_PARTICIPANTS_ACTION_SAVENEW = null;
	
	public boolean debug = false;
	//public boolean IRS = false;
	public boolean IRS = true;
	
	//private static String OLD_TBI_STATE = "";
	org.apache.http.HttpResponse httpResponse = null;
	org.apache.http.client.HttpClient httpclient = org.apache.http.impl.client.HttpClients.createDefault();
	
	// Services we need
	private IWorkItemServer fWorkItemServer;
	private IWorkItemCommon fWorkItemCommon;

	public AbstractService() {
		// TODO Auto-generated constructor stub
	}

	// Added on 10-27-20, trying to work out the reading field from the wi that initiated the advisor
	//private static final String WORKITEM_ATTRIBUTE_RELEASE_ID = "gov.irs.team.workitem.attribute.releaseID";
															  // gov.irs.team.workitem.attribute.releaseID
	

	/**
	 * If a process uses the "build on state change" participant, this run method
	 * is called by the Jazz process engine whenever a work item is saved.
	 * 
	 * @param operation
	 *            the operation in which to participate
	 *            <p>
	 *            The operation provides the runtime context for the
	 *            participant. This participant will use it to get information
	 *            about the work item that is being saved.
	 *            </p>
	 * @param participantConfig
	 *            the configuration element which configures this participant;
	 *            this corresponds to the XML element which declares this
	 *            participant in the process specification/customization.
	 *            <p>
	 *            This participant obtains the trigger work item type and state
	 *            from this parameter. The build definition id is also found
	 *            here.
	 *            </p>
	 * @param collector
	 *            the collector into which any info should be added
	 *            <p>
	 *            Participants report back failures and success via this
	 *            collector.
	 *            </p>
	 * @param monitor
	 *            the monitor used to report progress and receive cancellation
	 *            requests
	 *            <p>
	 *            Generally speaking, server side participants, like this one,
	 *            do not need to report progress to the monitor. Client side
	 *            participants should report progress. Checking for cancellation
	 *            after long operations performed by the participant is a good
	 *            practice on either client or server.
	 *            </p>
	 * @throws TeamRepositoryException
	 *             if one of the invoked service api throws it
	 */	
	public void run(AdvisableOperation operation,
			IProcessConfigurationElement advisorConfiguration,
			IAdvisorInfoCollector collector, IProgressMonitor monitor)
			throws TeamRepositoryException {
		// TODO Auto-generated method stub
		System.out.println("\n##########\nIN THE RUN MODULE OF AbstractService.java");
		SimpleDateFormat formatter= new SimpleDateFormat("yyyy-MM-dd 'at' HH:mm:ss z");
		//SimpleDateFormat formatter= new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z");
		Date date = new Date(System.currentTimeMillis());
		System.out.println(formatter.format(date));
		
		fWorkItemServer = getService(IWorkItemServer.class);
		fWorkItemCommon = getService(IWorkItemCommon.class);
		
		String originalState = "";
		IWorkItem sourceworkItem = null;
		IWorkItem sourceworkItem2 = null;
		Identifier<IState> oldStateId = null;
		
		
		Object obj = operation.getOperationData();
		ISaveParameter saveParameter = null;
		SaveParameter save = (SaveParameter) obj;
		IAuditable audi = save.getNewState();
		IAuditable oldState = save.getOldState();
		if (obj instanceof ISaveParameter) {
			saveParameter = (ISaveParameter) obj;
			System.out.println("ADV:CHECKING THE STATUS of CREATE_TRACED_WORKITEMS_PARTICIPANTS_ACTION_SAVE TO DETEMINE IF THE PARTICIPANT IS SUPPOSED TO FIRE");
			/*
			if (saveParameter.getAdditionalSaveParameters().contains(BuildOnStateChangeParticipant.CREATE_TRACED_WORKITEMS_PARTICIPANTS_ACTION_SAVENEW)) 
			{ 
				System.out.println("\nADV:NOT FIRING AFTER CHECKING");
				return; 
			}
			*/
		}
		if (audi instanceof IWorkItem)
		{
			System.out.println("\nAUDITABLE IS INSTANCE OF WORKITEM");
			IWorkItem work = (IWorkItem)audi;
			String newType = work.getWorkItemType();
			System.out.println("\nWORKITEM TYPE " + newType);
			Identifier<IState> newStateId = work.getState2();
			String targetState = newStateId.getStringIdentifier();
			System.out.println("\nWORKITEM STATE " + newStateId);
			System.out.println("\nWORKITEM TARGET STATE " + targetState);
			if(oldState!=null)
			{
				if (oldState instanceof IWorkItem) 
				{
					sourceworkItem2 = (IWorkItem) oldState;
					oldStateId = sourceworkItem2.getState2();
					if(oldStateId!=null)
					{
						originalState = oldStateId.getStringIdentifier();
						if (originalState.equals("com.ibm.team.workitem.buildTrackingWorkflow.state.s8")) {System.out.println("\nADV:OLD STATE ID: Draft");}
						if (originalState.equals("com.ibm.team.workitem.buildTrackingWorkflow.state.s9")) {System.out.println("\nADV:OLD STATE ID: Building");}
						if (originalState.equals("com.ibm.team.workitem.buildTrackingWorkflow.state.s10")) {System.out.println("\nADV:OLD STATE ID: Certification Review");}
						if (originalState.equals("com.ibm.team.workitem.buildTrackingWorkflow.state.s11")) {System.out.println("\nADV:OLD STATE ID: Certified");}
						if (originalState.equals("com.ibm.team.workitem.buildTrackingWorkflow.state.s12")) {System.out.println("\nADV:OLD STATE ID: Scheduled");}
						if (originalState.equals("com.ibm.team.workitem.buildTrackingWorkflow.state.s13")) {System.out.println("\nADV:OLD STATE ID: Dropped");}
						if (originalState.equals("com.ibm.team.workitem.buildTrackingWorkflow.state.s14")) {System.out.println("\nADV:OLD STATE ID: Deployed");}
					}
				}
			}
			if (targetState.equals("com.ibm.team.workitem.buildTrackingWorkflow.state.s8")) {System.out.println("\nADV:NEW STATE ID: Draft");}
			if (targetState.equals("com.ibm.team.workitem.buildTrackingWorkflow.state.s9")) {System.out.println("\nADV:NEW STATE ID: Building");}
			if (targetState.equals("com.ibm.team.workitem.buildTrackingWorkflow.state.s10")) {System.out.println("\nADV:NEW STATE ID: Certification Review");}
			if (targetState.equals("com.ibm.team.workitem.buildTrackingWorkflow.state.s11")) {System.out.println("\nADV:NEW STATE ID: Certified");}
			if (targetState.equals("com.ibm.team.workitem.buildTrackingWorkflow.state.s12")) {System.out.println("\nADV:NEW STATE ID: Scheduled");}
			if (targetState.equals("com.ibm.team.workitem.buildTrackingWorkflow.state.s13")) {System.out.println("\nADV:NEW STATE ID: Dropped");}
			if (targetState.equals("com.ibm.team.workitem.buildTrackingWorkflow.state.s14")) {System.out.println("\nADV:NEW STATE ID: Deployed");}	

			System.out.println("\nADV:NEW STATE ID: " + newStateId);
			System.out.println("\nADV:OLD STATE ID: " + oldStateId);
			System.out.println("\nADV:TARGET STATE: " + targetState);
			System.out.println("\nADV:ORIGINAL STATE: " + originalState);
			
			System.out.println("\nADV:Performing all the USE CASE #3 checks, regardless of targetState or originalState or if the State is changing");
			String RYAN = UseCase3(originalState, targetState, work, collector, monitor);
			System.out.println("\nADV:RETURN CODE FROM USECASE3 METHOD = " + RYAN);
			
			if ((newStateId != null) && !(newStateId.equals(oldStateId))) { // State is changing
				System.out.println("\nADV:STATE IS CHANGING");
				System.out.println("\nADV:ORIGINAL STATE = |" + originalState + "| NEW STATE = |" + targetState + "|");
				if (newType.equals("com.ibm.team.workItemType.buildtrackingitem"))
				{
					if (targetState.equals("com.ibm.team.workitem.buildTrackingWorkflow.state.s8")) 
					{ 
						System.out.println("\nADV:TBI/Draft");
						//System.out.println("\nADV:Performing all the USE CASE #3 checks");
						//System.out.println("\nADV:OLD STATE ID: " + originalState);
						//System.out.println("\nADV:NEW STATE ID = " + targetState);
						if(oldStateId!=null)
						{
							System.out.println("\nADV:UPDATING AN EXISTING TBI IN DRAFT STATE: " + originalState);
						} else {
							System.out.println("\nADV:CREATING A NEW TBI IN DRAFT STATE:");
						}
						//String RYAN = UseCase3(oldStateId, targetState, work, collector, monitor);
						//System.out.println("\nADV:RETURN CODE FROM USECASE3 = " + RYAN);
					} //targetState.equals("com.ibm.team.workitem.buildTrackingWorkflow.state.s8" - DRAFT

					else if (targetState.equals("com.ibm.team.workitem.buildTrackingWorkflow.state.s9")) 
					{ 
						System.out.println("\nADV:TBI/Building");
						//String RYAN = UseCase3(oldStateId, targetState, work, collector, monitor);
						//System.out.println("\nADV:RETURN CODE FROM USECASE3 = " + RYAN);
					} else if (targetState.equals("com.ibm.team.workitem.buildTrackingWorkflow.state.s10")) 
					{ 
						System.out.println("\nADV:TBI/Certification Review");
						//String RYAN = UseCase3(oldStateId, targetState, work, collector, monitor);
						//System.out.println("\nADV:RETURN CODE FROM USECASE3 = " + RYAN);
					} else if (targetState.equals("com.ibm.team.workitem.buildTrackingWorkflow.state.s11")) // TBI/Certified
					{ 
						System.out.println("\nADV:TBI/Certified");
						//System.out.println("\nADV:Working with a TBI Work Item WITH NEW STATE of " + targetState);
						//System.out.println("\nADV:Working with a TBI Work Item WITH OLD STATE of " + originalState);
						System.out.println("\nADV:<<<<< ######## >>>>>> ");
						/*
						String nexusUrl7 = "http://" + NON_IRS_NEXUS_INSTANCE_URL + "/service/rest/v1/staging/move/" + NON_IRS_NEXUS_CERTIFIED_REPO + "?repository=" + NON_IRS_NEXUS_RELEASE_CANDIDATE_REPO + "&maven.groupId=" + NEXUS_GID + "&maven.artifactId=" + NEXUS_ARTIFACT_NAME + "&maven.baseVersion=" + NEXUS_VER + "&maven.extension=" + NEXUS_EXTENSION + "&maven.classifier=" + NEXUS_CLASSIFIER;
						*/
						
						if (originalState.equals("com.ibm.team.workitem.buildTrackingWorkflow.state.s10"))
						{
							System.out.println("\nADV:OLD STATE: Certification Review");
							//System.out.println("\nADV:Performing all the USE CASE #3 checks");
							//String RYAN = UseCase3(oldStateId, targetState, work, collector, monitor);
							
							//System.out.println("\nADV:PERFORMING THE API CALL TO MOVE THE ARTIFACT FROM THE ReleaseCandidate REPO TO THE Certified REPO");

						} else { // OLD STATE IS CERTIFICATION REVIEW
							System.out.println("\nADV:OLD STATE IS NOT CERTIFICATION REVIEW, NO PROMOTION ACTION NECESSARY"); 
						}
					} // NEW STATE IS CERTIFIED
					else if (targetState.equals("com.ibm.team.workitem.buildTrackingWorkflow.state.s12")) // TBI/Scheduled
					{ 
						System.out.println("\nADV:TBI/Scheduled");
						//System.out.println("\nADV:Performing all the USE CASE #3 checks");
						//String RYAN = UseCase3(oldStateId, targetState, work, collector, monitor);
					}
					else if (targetState.equals("com.ibm.team.workitem.buildTrackingWorkflow.state.s13")) { System.out.println("\nADV:TBI/Dropped"); }
					else if (targetState.equals("com.ibm.team.workitem.buildTrackingWorkflow.state.s14")) // TBI/Deployed
					{ 
						//System.out.println("\nADV:Working with a TBI Work Item WITH NEW STATE of Deployed");
						//System.out.println("\nADV:Working with a TBI Work Item WITH OLD STATE of " + originalState);
						System.out.println("\nADV:TBI/Deployed");
						System.out.println("\nADV:##### <<<<< >>>>> #####");
						/*
						String nexusUrl7 = "http://" + NON_IRS_NEXUS_INSTANCE_URL + "/service/rest/v1/staging/move/" + NON_IRS_NEXUS_PRODUCTION_REPO + "?repository=" + NON_IRS_NEXUS_CERTIFIED_REPO + "&maven.groupId=" + NEXUS_GID + "&maven.artifactId=" + NEXUS_ARTIFACT_NAME + "&maven.baseVersion=" + NEXUS_VER + "&maven.extension=" + NEXUS_EXTENSION + "&maven.classifier=" + NEXUS_CLASSIFIER;
						*/
						//org.apache.http.client.HttpClient httpclient = org.apache.http.impl.client.HttpClients.createDefault();
						//org.apache.http.HttpResponse httpResponse = null;
						
						if (originalState.equals("com.ibm.team.workitem.buildTrackingWorkflow.state.s12"))
						{
							System.out.println("\nADV:OLD STATE: Scheduled");
							//System.out.println("\nADV:Performing all the USE CASE #3 checks");
							//String RYAN = UseCase3(oldStateId, targetState, work, collector, monitor);
						} else { // OLD STATE IS SCHEDULED
							System.out.println("\nADV:OLD STATE IS NOT SCHEDULED, NO PROMOTION ACTION NECESSARY"); 
						}
					} // NEW STATE IS DEPLOYED
					else { System.out.println("\nADV:Bad State for TBI"); }
				} else if (newType.equals("com.ibm.team.workitem.workItemType.impediment")) 
				{
					if (targetState.equals("com.ibm.team.workitem.impedimentWorkflow.state.s1")) { System.out.println("\nADV:Impediment/New"); }
					else if (targetState.equals("com.ibm.team.workitem.impedimentWorkflow.state.s2")) { System.out.println("\nADV:Impediment/Resolved"); }
					else if (targetState.equals("com.ibm.team.workitem.impedimentWorkflow.state.s3")) { System.out.println("\nADV:Impediment/Invalid"); }
					else { System.out.println("\nADV:Bad State for Impediment"); }
				} else if (newType.equals("task"))
				{
					if (targetState.equals("com.ibm.team.workitem.taskWorkflow.state.s1")) { System.out.println("\nADV:Task/New"); }
					else if (targetState.equals("com.ibm.team.workitem.taskWorkflow.state.s2")) { System.out.println("\nADV:Task/In Progress");	}
					else if (targetState.equals("com.ibm.team.workitem.taskWorkflow.state.s3")) { System.out.println("\nADV:Task/Done"); }
					else if (targetState.equals("com.ibm.team.workitem.taskWorkflow.state.s4")) { System.out.println("\nADV:Task/Invalid");	}
					else { System.out.println("\nADV:Bad State for Task");	}
				} else if (newType.equals("com.ibm.team.apt.workItemType.story"))
				{
					if (targetState.equals("com.ibm.team.apt.story.defined")) { System.out.println("\nADV:Story/In Progress"); }
					else if (targetState.equals("com.ibm.team.apt.storyWorkflow.state.s1")) { System.out.println("\nADV:Story/Deferred"); }
					else if (targetState.equals("com.ibm.team.apt.story.tested")) { System.out.println("\nADV:Story/Implemented"); }
					else if (targetState.equals("com.ibm.team.apt.story.idea")) { System.out.println("\nADV:Story/New"); }
					else if (targetState.equals("com.ibm.team.apt.storyWorkflow.state.s2")) { System.out.println("\nADV:Story/Invalid"); }
					else { System.out.println("\nADV:Bad State for Story"); }
				} else 
				{
					/*
					* "Bad Work Item Type"
					*/
					System.out.println("\nADV:Bad WorkItem Type");			
				}
			} else { // ((newStateId != null) && !(newStateId.equals(oldStateId)))
				System.out.println("\nADV:STATE IS not CHANGING");
			}

		} // audi instanceof IWorkItem
		System.out.println("\n##########\nEND OF RUN MODULE OF AbstractService.java");
		date = new Date(System.currentTimeMillis());
		System.out.println(formatter.format(date));
	} //run
	
	/**
	 * Returns a text string from an attribute on the work item being saved
	 * 
	 * @param workItem
	 *            the current workitem being saved
	 * @param attribute
	 *            an attribute on the workitem being saved
	 * @param monitor
	 *            the monitor used to report progress and receive cancellation
	 *            requests
	 *            <p>
	 *            Generally speaking, server side participants, like this one,
	 *            do not need to report progress to the monitor. Client side
	 *            participants should report progress. Checking for cancellation
	 *            after long operations performed by the participant is a good
	 *            practice on either client or server.
	 *            </p>
	 * @throws TeamRepositoryException
	 *             if one of the invoked service api throws it
	 */
	private String getString(IWorkItem workItem, IAttribute attribute,
			IProgressMonitor monitor) throws TeamRepositoryException {
			//System.out.println("\nADV: IN getString METHOD");
			String duration = "";
			String tempDuration = "";
			
			//System.out.println("\nIN GETSTRING : " + " WORKITEM : " + workItem  + " ATTRIBUTE : " + attribute);
			if (attribute != null && workItem.hasAttribute(attribute)) {
				Object value = workItem.getValue(attribute);
				if (value instanceof String) 
				{
					tempDuration = (String) value;
				} else if (value instanceof Integer) {
					//tempDuration = (String) value;
					return ((Integer) value).toString();
				} else {
					tempDuration = "";
				}
				if (tempDuration != null)
				{
					return tempDuration;
				}
			}
			return duration;
	}
	/**
	 * Validates that the value in the appropriate fields on the TBI are associated with an artifact in a Nexus repository.
	 * Runs the following methods:
	 *             ValidateReleaseID	
	 *             ValidateCoordinates
	 *             ValidatePromotion
	 * @param originalState
	 *            a text string that is used to execute an API call against the Nexus instance
	 * @param targetState
	 *            a text string that is used to execute an API call against the Nexus instance
	 * @param sourceworkItem
	 *            the encrypted password for the Nexus server
	 *            <p>
	 *            This participant will report back any failures it discovers,
	 *            for example, if the build definition does not exist.
	 *            </p>
	 * @param collector
	 *            the collector into which any info should be added
	 *            <p>
	 *            Participants report back failures and success via this
	 *            collector.
	 *            </p>
	 */
	private String UseCase3(String originalState, String targetState, IWorkItem sourceworkItem, IAdvisorInfoCollector collector, 
		IProgressMonitor monitor) throws TeamRepositoryException {
		System.out.println("\nADV: IN UseCase3 METHOD");
		//String duration = "";
		//String tempDuration = "";
		String nexusUrl7 = "";
		String IRSnexusUrl7 = "";
		String nexusUrl5 = "";
		String IRSnexusUrl5 = "";
		String nexusUrl2 = "";
		String IRSnexusUrl2 = "";
		
		System.out.println("\nADV:IN USE CASE3 method");
		
		System.out.println("\nADV:Performing all the USE CASE #3 checks");
		System.out.println("\nADV:ORIGINAL STATE: |" + originalState + "|");
		System.out.println("\nADV:NEW STATE ID = |" + targetState + "|");
		if(originalState!=null) 
		{ 
			System.out.println("\nADV:UPDATING AN EXISTING TBI: |" + originalState + "|");
			} else {
			System.out.println("\nADV:CREATING A NEW TBI: |" + originalState + "|");
		}
		// System.out.println("ADV:nexusUrl7: " + nexusUrl7);
		// MOVING CODE
		IAttribute myReleaseId = fWorkItemCommon.findAttribute(
			sourceworkItem.getProjectArea(), WORKITEM_ATTRIBUTE_RELEASE_ID, monitor);
		//System.out.println("ADV:IAttribute myGroupId");
		IAttribute myGroupId = fWorkItemCommon.findAttribute(
			sourceworkItem.getProjectArea(), WORKITEM_ATTRIBUTE_GROUP_ID, monitor);
		//System.out.println("ADV:IAttribute myVersion");
		IAttribute myVersion = fWorkItemCommon.findAttribute(
			sourceworkItem.getProjectArea(), WORKITEM_ATTRIBUTE_VERSION, monitor);
		//System.out.println("ADV:IAttribute myArtifactName");
		IAttribute myArtifactName = fWorkItemCommon.findAttribute(
			sourceworkItem.getProjectArea(), WORKITEM_ATTRIBUTE_ARTIFACT_NAME, monitor);
		IAttribute myClassifier = fWorkItemCommon.findAttribute(
			sourceworkItem.getProjectArea(), WORKITEM_ATTRIBUTE_CLASSIFIER, monitor);
		IAttribute myExtension = fWorkItemCommon.findAttribute(
			sourceworkItem.getProjectArea(), WORKITEM_ATTRIBUTE_EXTENSION, monitor);
		IAttribute myKISAMCR = fWorkItemCommon.findAttribute(
			sourceworkItem.getProjectArea(), WORKITEM_ATTRIBUTE_KISAM_CR, monitor);
		IAttribute myWorkItemId = fWorkItemCommon.findAttribute(
				sourceworkItem.getProjectArea(), WORKITEM_ATTRIBUTE_ID, monitor);		

		NEXUS_TAG = getString(sourceworkItem, myReleaseId, monitor);
		System.out.println("ADV:NEXUS_TAG :" + NEXUS_TAG);
		NEXUS_CLASSIFIER = getString(sourceworkItem, myClassifier, monitor);
		System.out.println("ADV:NEXUS_CLASSIFIER : " + NEXUS_CLASSIFIER);
		NEXUS_GID = getString(sourceworkItem, myGroupId, monitor);
		System.out.println("ADV:NEXUS_GID : " + NEXUS_GID);
		NEXUS_VER = getString(sourceworkItem, myVersion, monitor);
		System.out.println("ADV:NEXUS_VER : " + NEXUS_VER);
		NEXUS_ARTIFACT_NAME = getString(sourceworkItem, myArtifactName, monitor);
		System.out.println("ADV:NEXUS_ARTIFACT_NAME : " + NEXUS_ARTIFACT_NAME);
		NEXUS_EXTENSION = getString(sourceworkItem, myExtension, monitor);
		System.out.println("ADV:NEXUS_EXTENSION : " + NEXUS_EXTENSION);
		NEXUS_KISAM_CR = getString(sourceworkItem, myKISAMCR, monitor);
		WORKITEM_ID = getString(sourceworkItem, myWorkItemId, monitor);
		//System.out.println("ADV:NEXUS_KISAM_CR : " + NEXUS_KISAM_CR);
		//System.out.println("ADV:WORKITEM_ID : " + WORKITEM_ID);
		// ADDED ON 1-6-2021 b/c it had been removed inadvertently
		/*
		if (NEXUS_TAG == null) 
		{
			System.out.println("NEXUS_TAG IS EQUAL TO NULL");	
		}
		*/
		if (NEXUS_TAG.length() == 0)
		{
			System.out.println("RELEASE ID FIELD not POPULATED,  NO NEED TO PROCEED FURTHER!!");	
		} else {		
			//System.out.println("\nADV:SETTING DEBUG BASED ON NEXUS_KISAM_CR: " + NEXUS_KISAM_CR);
			//if (NEXUS_KISAM_CR == "debug") { debug = true; } else { debug = false; }
			if (NEXUS_KISAM_CR.equals("debug")) { debug = true; } else { debug = false; }
			System.out.println("\nADV:CHECKING VALUE OF DEBUG");			
			if (debug) { System.out.println("\nADV:DEBUG IS ON"); } else { System.out.println("\nADV:DEBUG IS OFF"); }
			
			nexusUrl5 = "http://" + NON_IRS_NEXUS_INSTANCE_URL + "/service/rest/v1/search?tag=" + NEXUS_TAG;  // This one looks for a repo with the tag from the Release ID field
			IRSnexusUrl5 = "http://" + IRS_NEXUS_INSTANCE_URL + "/nexus/service/rest/v1/search?tag=" + NEXUS_TAG;  // This one looks for a repo with the tag from the Release ID field
			nexusUrl2 = "http://" + NON_IRS_NEXUS_INSTANCE_URL + "/service/rest/v1/search/assets?group=" + NEXUS_GID + "&name=" + NEXUS_ARTIFACT_NAME + "&version=" + NEXUS_VER + "&maven.extension=" + NEXUS_EXTENSION + "&maven.classifier=" + NEXUS_CLASSIFIER;
			IRSnexusUrl2 = "http://" + IRS_NEXUS_INSTANCE_URL + "/nexus/service/rest/v1/search?group=" + NEXUS_GID + "&name=" + NEXUS_ARTIFACT_NAME + "&version=" + NEXUS_VER + "&maven.extension=" + NEXUS_EXTENSION + "&maven.classifier=" + NEXUS_CLASSIFIER;
			nexusUrl7 = "http://" + NON_IRS_NEXUS_INSTANCE_URL + "/service/rest/v1/search?repository="+ NON_IRS_NEXUS_RELEASE_CANDIDATE_REPO + "&tag=" + NEXUS_TAG;
			//nexusUrl7 = "http://" + NON_IRS_NEXUS_INSTANCE_URL + "/service/rest/v1/search?repository=IRS_ReleaseCandidate&tag=" + NEXUS_TAG;
			IRSnexusUrl7 = "http://" + IRS_NEXUS_INSTANCE_URL + "/nexus/service/rest/v1/search?repository="+ IRS_NEXUS_RELEASE_CANDIDATE_REPO + "&tag=" + NEXUS_TAG;
			//IRSnexusUrl7 = "http://" + IRS_NEXUS_INSTANCE_URL + "/nexus/service/rest/v1/search?repository=release-candidates&tag=" + NEXUS_TAG;
						
			if(originalState!=null) 
			{ 
				System.out.println("\nADV:Setting Move URL based on target state");
				System.out.println("\nADV:targetState >" + targetState + "<");
	 
					if (targetState.equals("com.ibm.team.workitem.buildTrackingWorkflow.state.s11")) // Certified
				{ 
					System.out.println("ADV:TARGET = CERTIFIED, SETTING TARGET REPO TO IRS_CERTIFIED");
					System.out.println("ADV:CONFIRM THAT ASSET DOES not EXIST IN CERTIFIED, BUT does in RELEASE CANDIDATE");
					
					if (IRS) 
					{
						System.out.println("\nADV:Calling ValidateReleaseID with: " + IRSnexusUrl5);
						if (ValidateReleaseID(IRSnexusUrl5, IRSnexusCredentialsEncoded, collector))
						{
							System.out.println("ADV:ValidateReleaseID returned TRUE");
							if (ValidateCoordinates(IRSnexusUrl2, IRSnexusCredentialsEncoded, collector))
							{
								System.out.println("ADV:ValidateCoordinates returned TRUE");
								if (originalState.equals("com.ibm.team.workitem.buildTrackingWorkflow.state.s10"))
								{
									System.out.println("\nADV:Calling ValidatePromotion with CERTIFIED: IRS");
									if (ValidatePromotion(targetState, IRSnexusCredentialsEncoded, collector))
									{
										System.out.println("ADV:ValidatePromotion returned TRUE");
									} else {
										System.out.println("ADV:ValidatePromotion returned FALSE");
									}
								}
							} else {
								System.out.println("ADV:ValidateCoordinates returned FALSE");
							}
						} else {
							System.out.println("ADV:ValidateReleaseID returned FALSE");
						}
					} else {  // NOT IRS
						System.out.println("\nADV:Calling ValidateReleaseID with: " + nexusUrl5);
						if (ValidateReleaseID(nexusUrl5, nexusCredentialsEncoded, collector))
						{
							System.out.println("ADV:ValidateReleaseID returned TRUE");
							if (ValidateCoordinates(nexusUrl2, nexusCredentialsEncoded, collector))
							{
								System.out.println("ADV:ValidateCoordinates returned TRUE");
								if (originalState.equals("com.ibm.team.workitem.buildTrackingWorkflow.state.s10"))
								{
									System.out.println("\nADV:Calling ValidatePromotion with CERTIFIED: NON-IRS");
									if (ValidatePromotion(targetState, nexusCredentialsEncoded, collector))
									{
										System.out.println("ADV:ValidatePromotion returned TRUE");
									} else {
										System.out.println("ADV:ValidatePromotion returned FALSE");
									}
								}
							} else {
								System.out.println("ADV:ValidateCoordinates returned FALSE");
							}	
						} else {
							System.out.println("ADV:ValidateReleaseID returned FALSE");
						}
					}
	
				} else if (targetState.equals("com.ibm.team.workitem.buildTrackingWorkflow.state.s14")) {
					System.out.println("ADV:TARGET = DEPLOYED, SETTING TARGET REPO TO IRS_PRODUCTION");
					//System.out.println("ADV:CONFIRM THAT ASSET DOES not EXIST IN IRS_PRODUCTION, BUT does IN CERTIFIED");
					
					if (IRS) 
					{
						System.out.println("\nADV:Calling ValidateReleaseID with: " + IRSnexusUrl2);
						if (ValidateReleaseID(IRSnexusUrl5, IRSnexusCredentialsEncoded, collector))
						{
							System.out.println("ValidateReleaseID returned TRUE");
							if (ValidateCoordinates(IRSnexusUrl2, IRSnexusCredentialsEncoded, collector))
							{
								System.out.println("ValidateCoordinates returned TRUE");
								if (originalState.equals("com.ibm.team.workitem.buildTrackingWorkflow.state.s12"))
								{
									System.out.println("\nADV:Calling ValidatePromotion with PRODUCTION: IRS");
									if (ValidatePromotion(targetState, IRSnexusCredentialsEncoded, collector))
									{
										System.out.println("ValidatePromotion returned TRUE");
									} else {
										System.out.println("ValidatePromotion returned FALSE");
									}
								}
							} else {
								System.out.println("ValidateCoordinates returned FALSE");
							}
						} else {
							System.out.println("ValidateReleaseID returned FALSE");
						}
					} else { // IRS
						System.out.println("\nADV:Calling ValidateReleaseID with: " + nexusUrl2);
						if (ValidateReleaseID(nexusUrl5, nexusCredentialsEncoded, collector))
						{
							System.out.println("ADV:ValidateReleaseID returned TRUE");
							if (ValidateCoordinates(nexusUrl2, nexusCredentialsEncoded, collector))
							{
								System.out.println("ADV:ValidateCoordinates returned TRUE");
								if (originalState.equals("com.ibm.team.workitem.buildTrackingWorkflow.state.s12"))
								{
									System.out.println("\nADV:Calling ValidatePromotion with PRODUCTION: NON-IRS");
									if (ValidatePromotion(targetState, nexusCredentialsEncoded, collector))
									{
										System.out.println("ADV:WE HAVE CONFIRMED THAT ASSET does EXIST IN CERTIFIED , BUT does NOT in PRODUCTION");
									} else {
										System.out.println("ADV:THE ASSET does not EXIST IN CERTIFIED");
									}
								}
							} else {
								System.out.println("ValidateCoordinates returned FALSE");
							}
						} else {
							System.out.println("ValidateReleaseID returned FALSE");
						}
					}					
				
					////nexusUrl7 = "http://" + NON_IRS_NEXUS_INSTANCE_URL + "/nexus/service/rest/v1/staging/move/IRS_Production?repository=IRS_Certified&maven.groupId=" + NEXUS_GID + "&maven.artifactId=" + NEXUS_ARTIFACT_NAME + "&maven.baseVersion=" + NEXUS_VER + "&maven.extension=" + NEXUS_EXTENSION + "&maven.classifier=" + NEXUS_CLASSIFIER;
					//-//IRSnexusUrl7 = "http://" + IRS_NEXUS_INSTANCE_URL + "/nexus/service/rest/V1/search?repository=certified&tag=" + NEXUS_TAG;
					//-//nexusUrl7 = "http://" + NON_IRS_NEXUS_INSTANCE_URL + "/service/rest/v1/search?repository=IRS_Certified&tag=" + NEXUS_TAG;
				////nexusUrl7 = "http://nexus.lab.zivra.com:8081/service/rest/v1/staging/move/IRS_Production?repository=IRS_Certified&maven.groupId=" + NEXUS_GID + "&maven.artifactId=" + NEXUS_ARTIFACT_NAME + "&maven.baseVersion=" + NEXUS_VER + "&maven.extension=" + NEXUS_EXTENSION + "&maven.classifier=" + NEXUS_CLASSIFIER;
				} else {
					System.out.println("\nADV:NO PROMOTION TARGET SPECIFIED");
					if (IRS) 
					{
						System.out.println("\nADV:Calling ValidateReleaseID with: " + IRSnexusUrl2);
						if (ValidateReleaseID(IRSnexusUrl5, IRSnexusCredentialsEncoded, collector))
						{
							System.out.println("ADV:ValidateReleaseID returned TRUE");
							if (ValidateCoordinates(IRSnexusUrl2, IRSnexusCredentialsEncoded, collector))
							{
								System.out.println("ADV:ValidateCoordinates returned TRUE");
							} else {
								System.out.println("ADV:ValidateCoordinates returned FALSE");
							}
						} else {
							System.out.println("ADV:ValidateReleaseID returned FALSE");
						}
						
					} else {
						System.out.println("\nADV:Calling ValidateReleaseID with: " + nexusUrl2);
						if (ValidateReleaseID(nexusUrl5, nexusCredentialsEncoded, collector))
						{
							System.out.println("ADV:ValidateReleaseID returned TRUE");
							if (ValidateCoordinates(nexusUrl2, nexusCredentialsEncoded, collector))
							{
								System.out.println("ADV:ValidateCoordinates returned TRUE");
							} else {
								System.out.println("ADV:ValidateCoordinates returned FALSE");
							}
						} else {
							System.out.println("ADV:ValidateReleaseID returned FALSE");
						}						
					}
				}
			} else {
				System.out.println("\nADV:OldStateId IS NULL");	
			}
			System.out.println("\nADV:LEAVING USE CASE3 method");
			if (IRS) 
			{
				return IRSnexusUrl7;
			} else {
				return nexusUrl7;
			}
		}
		if (IRS) 
		{
			return IRSnexusUrl7;
		} else {
			return nexusUrl7;
		}
	}
	/**
	 * Validates that the value in the Release ID field on the TBI is associated to tag on an artifact in a Nexus repository
	 * 
	 * @param Url
	 *            a text string that is used to execute an API call against the Nexus instance
	 * @param Credentials
	 *            the encrypted password for the Nexus server
	 *            <p>
	 *            This participant will report back any failures it discovers,
	 *            for example, if the build definition does not exist.
	 *            </p>
	 * @param collector
	 *            the collector into which any info should be added
	 *            <p>
	 *            Participants report back failures and success via this
	 *            collector.
	 *            </p>
	 */
	private boolean ValidateReleaseID(String Url, String Credentials, IAdvisorInfoCollector collector) {
	////private void ReadFile(String filename) {
	//private void ReadFile(String[] args) {
		System.out.println("\nADV:IN ValidateReleaseID METHOD");
		System.out.println("ADV:USING URL: " + Url);
		String nxitempath = "";
		String nxitemid = "";
		String nxitemrepo = "";
		String nxrepoformat = "";
		String nxchecksum = "";
		String nxgroup = "";
		String nxname = "";
		String nxversion = "";
		String nxassets = "";
		String nxtags = "";
		String nxcontinuationtoken = "";
		JSONObject agg = null;
		java.net.URL obj;
		try {
			obj = new java.net.URL(Url);  // SEARCHING FOR THE REPO TAG PULLED FROM THE RELEASE ID FIELD
			java.net.HttpURLConnection con = (java.net.HttpURLConnection) obj.openConnection();
			System.out.println("\nADV:Sending 'GET' request to URL : " + Url); // SEARCHING FOR THE REPO TAG PULLED FROM THE RELEASE ID FIELD
			con.setRequestMethod("GET");
			con.setRequestProperty("Authorization", "Basic " + Credentials);
			System.out.println("Successfully Authorized-ValidateReleaseID");
			int responseCode = con.getResponseCode();
			System.out.println("\nADV:GET request response code : " + responseCode); // GET Response code
			if (responseCode != 200) 
			{
				System.out.println("\nADV:Response Code indicates FAILURE OF ValidateReleaseID API CALL: " + responseCode);
				IAdvisorInfo problem = collector.createProblemInfo("INVALID RELEASE ID", "ASSET CANNOT BE FOUND using the RELEASE ID in Nexus", "error");
				collector.addInfo(problem);
				con.disconnect();
				return false;
			}
			BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
			System.out.println("\nin : " + in);
			agg = JSONObject.parse(in);
			JSONArray responseArray  = (JSONArray) agg.get("items");	
			int responseArrayLength = responseArray.size();
			System.out.println("\nValidateReleaseID-ITEMS ARRAY LENGTH : " + responseArrayLength);
			if (responseArrayLength == 0) 
			{
				System.out.println("\nADV:Response Array has a 0 Length and therefore found NOTHING: " + responseArray.size());
				IAdvisorInfo problem = collector.createProblemInfo("Release ID NOT FOUND", "Release ID does not resolve to a tagged repository in Nexus", "error");
				collector.addInfo(problem);
				con.disconnect();
				////nxdownloadUrl = "DOWNLOAD URL NOT FOUND";
				return false;
			} else {
				System.out.println("\nADV:ASSET FOUND USING THE RELEASE ID in Nexus");						
				for (int i = 0; i < responseArray.size(); i++) 
				{			  
					String str = responseArray.get(i).toString();
					JSONObject resultObject = (JSONObject)responseArray.get(i);

					nxitempath = resultObject.get("id").toString();
					nxitemrepo = resultObject.get("repository").toString();
					nxrepoformat = resultObject.get("format").toString();
					System.out.println("ADV:REL ID:component id: " + nxitempath );
					System.out.println("ADV:REL ID:nxitemrepo: " + nxitemrepo );
					System.out.println("ADV:REL ID:nxrepoformat: " + nxrepoformat );
				}			
				//con.disconnect();
				return true;
			}
			
		} catch (MalformedURLException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		//con.disconnect();
		//org.apache.http.client.utils.HttpClientUtils.closeQuietly(con);
		System.out.println("\n LINE NUMBER 775");
		return true;
	}
	/**
	 * Validates that the Nexus coordinates shown on the TBI are associated to an artifact in a repository
	 * 
	 * @param Url
	 *            a text string that is used to execute an API call against the Nexus instance
	 * @param Credentials
	 *            the encrypted password for the Nexus server
	 *            <p>
	 *            This participant will report back any failures it discovers,
	 *            for example, if the build definition does not exist.
	 *            </p>
	 * @param collector
	 *            the collector into which any info should be added
	 *            <p>
	 *            Participants report back failures and success via this
	 *            collector.
	 *            </p>
	 */
	private boolean ValidateCoordinates(String Url, String Credentials, IAdvisorInfoCollector collector) {
	////private void ReadFile(String filename) {
	//private void ReadFile(String[] args) {
		System.out.println("\nADV:IN ValidateCoordinates METHOD");
		System.out.println("ADV:USING URL: " + Url);
		String nxitempath = "";
		String nxitemrepo = "";
		String nxrepoformat = "";
		String nxchecksum = "";
		String nxcontinuationtoken = "";
		JSONObject agg = null;
		
		java.net.URL obj;
		
		try {		
			obj = new java.net.URL(Url);
			java.net.HttpURLConnection con = (java.net.HttpURLConnection) obj.openConnection();
			System.out.println("\nADV:ValidateCoordinates:Sending 'GET' request to URL : " + Url); // SEARCHING FOR AN ASSET CAN BE FOUND USING THE GAV, CLASSIFIER, and EXTENSION
			con.setRequestMethod("GET");
			con.setRequestProperty("Authorization", "Basic " + Credentials);
			//System.out.println("Successfully Authorized-ValidateCoordinates");
			int responseCode = con.getResponseCode();
			
			System.out.println("\nADV:ValidateCoordinates:GET request response code : " + responseCode); // GET Response code
			if (responseCode != 200) 
			{
				System.out.println("\nADV:Response Code indicates FAILURE OF API CALL: " + responseCode);
				IAdvisorInfo problem = collector.createProblemInfo("INVALID ASSET", "ASSET CANNOT BE FOUND in Nexus", "error");
				collector.addInfo(problem);
				con.disconnect();
				return false;
			}
			BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
			System.out.println("\nin : " + in);
			agg = JSONObject.parse(in);
			JSONArray responseArray  = (JSONArray) agg.get("items");	
			int responseArrayLength = responseArray.size();
			System.out.println("\nValidateCoordinates-ITEMS ARRAY LENGTH : " + responseArrayLength);
			if (responseArrayLength == 0) 
			{
				System.out.println("\nADV:Response Array has a 0 Length and therefore found NOTHING: " + responseArray.size());
				IAdvisorInfo problem = collector.createProblemInfo("INVALID ASSET", "ASSET CANNOT BE FOUND USING THE GAV, CLASSIFIER, and EXTENSION in Nexus", "error");
				collector.addInfo(problem);
				con.disconnect();
				return false;
			} else {
				System.out.println("\nADV:ASSET FOUND USING THE GAV, CLASSIFIER, and EXTENSION in Nexus");						
				for (int i = 0; i < responseArray.size(); i++) 
				{			  
					String str = responseArray.get(i).toString();
					//System.out.println("\nADV:GetDownloadUrl:VALUE OF RESPONSE: " + str + "\n");
					//JSONObject resultObject = responseArray.getJSONObject(i);
					JSONObject resultObject = (JSONObject)responseArray.get(i);
					nxitempath = resultObject.get("id").toString();
					nxitemrepo = resultObject.get("repository").toString();
					nxrepoformat = resultObject.get("format").toString();
					System.out.println("ADV:COORD:component id: " + nxitempath );
					System.out.println("ADV:COORD:nxitemrepo: " + nxitemrepo );
					System.out.println("ADV:COORD:nxrepoformat: " + nxrepoformat );
				}			
				//con.disconnect();
				return true;
			}

	} catch (MalformedURLException e1) {
		// TODO Auto-generated catch block
		e1.printStackTrace();
	} catch (IOException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
	System.out.println("\n LINE NUMBER 867");
	return true;
	}
	/**
	 * Validates that the Nexus artifacts to be promoted exist in the correct repository
	 * 
	 * @param State
	 *            the target state that the TBI is being moved into
	 * @param Credentials
	 *            the encrypted password for the Nexus server
	 *            <p>
	 *            This participant will report back any failures it discovers,
	 *            for example, if the build definition does not exist.
	 *            </p>
	 * @param collector
	 *            the collector into which any info should be added
	 *            <p>
	 *            Participants report back failures and success via this
	 *            collector.
	 *            </p>
	 */
	private boolean ValidatePromotion(String State, String Credentials, IAdvisorInfoCollector collector) {
		System.out.println("\nADV:IN ValidatePromotion METHOD");
		System.out.println("ADV:USING State: " + State);
		String nxitempath = "";
		String nxitemrepo = "";
		String nxrepoformat = "";
		String IRSnexusUrl7 = "";
		String nexusUrl7 = "";
		JSONObject agg = null;
	
		if (State.equals("com.ibm.team.workitem.buildTrackingWorkflow.state.s11"))
		{ 
			System.out.println("ADV:TARGET = CERTIFIED, SETTING TARGET REPO TO CERTIFIED");
			System.out.println("ADV:CONFIRM THAT ASSET DOES not EXIST IN CERTIFIED, BUT does in RELEASE CANDIDATE");			
			if (IRS) 
			{
				System.out.println("\nADV:In ValidatePromotion with CERTIFIED: IRS");				
				//IRSnexusUrl7 = "http://" + IRS_NEXUS_INSTANCE_URL + "/nexus/service/rest/v1/search?repository=release-candidates&tag=" + NEXUS_TAG;
				IRSnexusUrl7 = "http://" + IRS_NEXUS_INSTANCE_URL + "/nexus/service/rest/v1/search?repository="+ IRS_NEXUS_RELEASE_CANDIDATE_REPO + "&tag=" + NEXUS_TAG;
			} else {
				System.out.println("\nADV:In ValidatePromotion with CERTIFIED: NON-IRS");
				//nexusUrl7 = "http://" + NON_IRS_NEXUS_INSTANCE_URL + "/service/rest/v1/search?repository=IRS_ReleaseCandidate&tag=" + NEXUS_TAG;
				nexusUrl7 = "http://" + NON_IRS_NEXUS_INSTANCE_URL + "/nexus/service/rest/v1/search?repository="+ NON_IRS_NEXUS_CERTIFIED_REPO + "&tag=" + NEXUS_TAG;
			}	
		} else if (State.equals("com.ibm.team.workitem.buildTrackingWorkflow.state.s14")) {
			System.out.println("ADV:TARGET = DEPLOYED, SETTING TARGET REPO TO IRS_PRODUCTION");
			System.out.println("ADV:CONFIRM THAT ASSET DOES not EXIST IN IRS_PRODUCTION, BUT does IN CERTIFIED");			
			if (IRS) 
			{
				System.out.println("\nADV:In ValidatePromotion with PRODUCTION: IRS");				
				//IRSnexusUrl7 = "http://" + IRS_NEXUS_INSTANCE_URL + "/nexus/service/rest/v1/search?repository=certified&tag=" + NEXUS_TAG;
				IRSnexusUrl7 = "http://" + IRS_NEXUS_INSTANCE_URL + "/nexus/service/rest/v1/search?repository="+ IRS_NEXUS_CERTIFIED_REPO + "&tag=" + NEXUS_TAG;
			} else {
				System.out.println("\nADV:In ValidatePromotion with PRODUCTION: NON-IRS");
				//nexusUrl7 = "http://" + NON_IRS_NEXUS_INSTANCE_URL + "/service/rest/v1/search?repository=IRS_Certified&tag=" + NEXUS_TAG;
				nexusUrl7 = "http://" + NON_IRS_NEXUS_INSTANCE_URL + "/nexus/service/rest/v1/search?repository="+ NON_IRS_NEXUS_CERTIFIED_REPO + "&tag=" + NEXUS_TAG;
			}
		} else {
			System.out.println("\nADV:NO TARGET SPECIFIED");	
		}	
		
		java.net.URL obj;
		try {
			if (IRS) 
			{
				obj = new java.net.URL(IRSnexusUrl7);
				System.out.println("\nADV:ValidatePromotion:Sending 'GET' request to URL : " + IRSnexusUrl7);
			} else {
				obj = new java.net.URL(nexusUrl7);
				System.out.println("\nADV:ValidatePromotion:Sending 'GET' request to URL : " + nexusUrl7);
			}
			
			java.net.HttpURLConnection con = (java.net.HttpURLConnection) obj.openConnection();		
			con.setRequestMethod("GET");
			con.setRequestProperty("Authorization", "Basic " + Credentials);
			//System.out.println("Successfully Authorized-ValidatePromotion");
			int responseCode = con.getResponseCode();
			
			System.out.println("\nADV:ValidatePromotion:GET request response code : " + responseCode); // GET Response code
			if (responseCode != 200) 
			{
				System.out.println("\nADV:Response Code indicates FAILURE OF ValidatePromotion API CALL: " + responseCode);
				IAdvisorInfo problem = collector.createProblemInfo("INVALID PROMOTION", "ASSET CANNOT BE FOUND using the REPOSITORY and RELEASE ID in Nexus", "error");
				collector.addInfo(problem);
				con.disconnect();
				return false;
			}
			BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
			System.out.println("\nin : " + in);
			agg = JSONObject.parse(in);
			JSONArray responseArray  = (JSONArray) agg.get("items");	
			int responseArrayLength = responseArray.size();
			System.out.println("\nValidatePromotion-ITEMS ARRAY LENGTH : " + responseArrayLength);
			if (responseArrayLength == 0) 
			{
				System.out.println("\nADV:Response Array has a 0 Length and therefore found NOTHING: " + responseArray.size());
				IAdvisorInfo problem = collector.createProblemInfo("INVALID COMPONENT", "COMPONENT CANNOT BE FOUND USING THE THE REPOSITORY AND TAG in Nexus", "error");
				collector.addInfo(problem);
				con.disconnect();
				return false;
			} else {
				System.out.println("\nADV:COMPONENT FOUND USING THE REPOSITORY AND TAG in Nexus");						
				for (int i = 0; i < responseArray.size(); i++) 
				{			  
					String str = responseArray.get(i).toString();
					//System.out.println("\nADV:GetDownloadUrl:VALUE OF RESPONSE: " + str + "\n");
					//JSONObject resultObject = responseArray.getJSONObject(i);
					JSONObject resultObject = (JSONObject)responseArray.get(i);
					nxitempath = resultObject.get("id").toString();
					nxitemrepo = resultObject.get("repository").toString();
					nxrepoformat = resultObject.get("format").toString();
					System.out.println("ADV:PROM:component id: " + nxitempath );
					System.out.println("ADV:PROM:nxitemrepo: " + nxitemrepo );
					System.out.println("ADV:PROM:nxrepoformat: " + nxrepoformat );
				}			
				//con.disconnect();
				if (State.equals("com.ibm.team.workitem.buildTrackingWorkflow.state.s11"))
				{
					if (nxitemrepo.equals("IRS_Certified"))
					{
						return true;
					} else {
						return false;
					}
				} else if (State.equals("com.ibm.team.workitem.buildTrackingWorkflow.state.s14")) 
				{
					if (nxitemrepo.equals("IRS_Production"))
					{
						return true;
					} else {
						return false;
					}
				} else {
					System.out.println("\nADV:NO TARGET SPECIFIED");
					return false;
				}			
				//return true;
			}
			
		} catch (MalformedURLException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println("\n LINE NUMBER 1014");
		return true;
	}	
	
} // AbstractService extends com.ibm.team.repository.service.AbstractService
