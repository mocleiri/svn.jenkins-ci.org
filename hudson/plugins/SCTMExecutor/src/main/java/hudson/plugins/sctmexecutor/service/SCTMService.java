package hudson.plugins.sctmexecutor.service;

import hudson.FilePath;
import hudson.plugins.sctmexecutor.Messages;
import hudson.plugins.sctmexecutor.exceptions.SCTMException;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.rmi.RemoteException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.rpc.ServiceException;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.io.IOUtils;

import com.borland.sctm.ws.administration.MainEntities;
import com.borland.sctm.ws.administration.MainEntitiesServiceLocator;
import com.borland.sctm.ws.administration.exceptions.InternalException;
import com.borland.sctm.ws.administration.exceptions.InvalidIdException;
import com.borland.sctm.ws.execution.ExecutionWebService;
import com.borland.sctm.ws.execution.ExecutionWebServiceServiceLocator;
import com.borland.sctm.ws.execution.entities.ExecutionHandle;
import com.borland.sctm.ws.execution.entities.ExecutionNode;
import com.borland.sctm.ws.execution.entities.ExecutionResult;
import com.borland.sctm.ws.execution.entities.PropertyValue;
import com.borland.sctm.ws.logon.SystemService;
import com.borland.sctm.ws.logon.SystemServiceServiceLocator;
import com.borland.sctm.ws.performer.PerformerService;
import com.borland.sctm.ws.performer.PerformerServiceServiceLocator;
import com.borland.sctm.ws.performer.SPNamedEntity;
import com.borland.sctm.ws.planning.PlanningService;
import com.borland.sctm.ws.planning.PlanningServiceServiceLocator;

public class SCTMService implements ISCTMService {
  private static final int MAX_LOGONRETRYCOUNT = 3;
  private static final Logger LOGGER = Logger.getLogger("hudson.plugins.sctmservice");  //$NON-NLS-1$
  
  private SystemService systemService;
  private ExecutionWebService execService;
  private MainEntities adminService;
  private PlanningService planningService;
  private PerformerService performerService;
  private long sessionId;
  private volatile int logonRetryCount;
  private String user;
  private String pwd;
  private Map<Integer, String> execDefIdToName;
  private int projectId;
  private String serviceExchangeURL;

  public SCTMService(String serviceURL, String user, String pwd, int projectId) throws SCTMException {
    try {
      this.user = user;
      this.pwd = pwd;
      this.projectId = projectId;
      
      systemService = new SystemServiceServiceLocator().getsccsystem(new URL(serviceURL + "/sccsystem?wsdl")); //$NON-NLS-1$
      execService = new ExecutionWebServiceServiceLocator().gettmexecution(new URL(serviceURL + "/tmexecution?wsdl")); //$NON-NLS-1$
      adminService = new MainEntitiesServiceLocator().getsccentities(new URL(serviceURL+"/sccentities?wsdl"));
      planningService = new PlanningServiceServiceLocator().gettmplanning(new URL(serviceURL+"/tmplanning?wsdl"));
      performerService = new PerformerServiceServiceLocator().gettmperformer(new URL(serviceURL+"/tmperformer?wsdl"));
      serviceExchangeURL = String.format("%sExchange?hid=%s&sid=%s&rfid=%s", serviceURL, "SilkPerformer");
      
      logon();
      this.execDefIdToName = new HashMap<Integer, String>();
    } catch (MalformedURLException e) {
      LOGGER.log(Level.SEVERE, e.getMessage(), e);
      throw new SCTMException(Messages.getString("SCTMService.err.serviceUrlWrong")); //$NON-NLS-1$
    } catch (ServiceException e) {
      LOGGER.log(Level.SEVERE, e.getMessage(), e);
      throw new SCTMException(MessageFormat.format(Messages.getString("SCTMService.err.urlOrServiceBroken"), serviceURL)); //$NON-NLS-1$
    } catch (RemoteException e) {
      LOGGER.log(Level.SEVERE, e.getMessage(), e);
      throw new SCTMException(MessageFormat.format(Messages.getString("SCTMService.err.commonFatalError"), e.getMessage())); //$NON-NLS-1$
    }
  }

  private void logon() throws RemoteException {
    this.sessionId = this.systemService.logonUser(this.user, this.pwd);;
    execService.setCurrentProject(sessionId, projectId);
    planningService.setCurrentProject(sessionId, String.valueOf(projectId));
  }
  
  private boolean handleLostSessionException(RemoteException e) throws SCTMException {
    if (lostSessionExceptionThrown(e) && logonRetryCount < MAX_LOGONRETRYCOUNT) { //$NON-NLS-1$
      logonRetryCount++;
      LOGGER.warning(Messages.getString("SCTMService.warn.SessionLostReconnect")); //$NON-NLS-1$
      try {
        logon();
        return true;
      } catch (RemoteException e1) {
        LOGGER.log(Level.SEVERE, e.getMessage(), e);
        if (e.getMessage().contains("Not logged in")) //$NON-NLS-1$
          throw new SCTMException(Messages.getString("SCTMService.err.accessDenied")); //$NON-NLS-1$
        else
          throw new SCTMException(MessageFormat.format(Messages.getString("SCTMService.err.commonFatalError"), e.getMessage())); //$NON-NLS-1$
      }
    }
    return false;
  }

  private boolean lostSessionExceptionThrown(RemoteException e) {
    String message = e.getMessage();
    return message.contains("Not logged in.") ||
           (message.contains("InvalidSidException: sid") && message.contains("is invalid or expired")); // InvalidSidException is not exposed as class in the wsdl
  }

  private Collection<ExecutionHandle> convertToList(ExecutionHandle[] handles) {
    Collection<ExecutionHandle> runs = new ArrayList<ExecutionHandle>(handles.length);
    for (ExecutionHandle handle : handles) {
      runs.add(handle);
    }
    return runs;
  }

  private String getProductName(ExecutionNode node) throws RemoteException {
    String testContainerId = getExecutionNodePropertyValue(node, "PROP_TESTCONTAINER");
    String productId = planningService.getProperty(sessionId, testContainerId, "_node_properties_ProductID_pk_fk").getValue();
    String productName = adminService.getProductNameById(sessionId, Integer.valueOf(productId));
    return productName;
  }

  private String getExecutionNodePropertyValue(ExecutionNode node, String propertyName) {
    PropertyValue[] propertyValues = node.getPropertyValues();
    for (PropertyValue propertyValue : propertyValues) {
      if (propertyName.equals(propertyValue.getName()))
        return propertyValue.getValue();
    }
    return null;
  }

  private String[] getAllBuildNumbersForProductAndVersion(int nodeId) throws RemoteException, SCTMException,
      InvalidIdException, InternalException {
    ExecutionNode node = getExecDefNode(nodeId);
    String version = getExecutionNodePropertyValue(node, "PROP_VERSIONNAME");
    String productName = getProductName(node);
    if (version == null || productName == null) {
      String name = getExecutionNodePropertyValue(node, "PROP_NAME");
      throw new IllegalArgumentException(MessageFormat.format("Cannot add a build number because the Execution definition ''{0}'' ({1}) is configured for using a ''Build Information File''. Disable ''Read from Build Information File'' and configure at least a valid version.",name, nodeId));
    }
    String[] builds = adminService.getBuilds(sessionId, productName, version);
    return builds;
  }

  private ExecutionNode getExecDefNode(int nodeId) throws RemoteException, SCTMException {
    ExecutionNode node = execService.getNode(sessionId, nodeId);
    if (node == null)
      throw new SCTMException(MessageFormat.format("Execution definition ''{0}'' does not exist. Check the build configuration.", nodeId));
    return node;
  }

  /* (non-Javadoc)
   * @see hudson.plugins.sctmexecutor.service.ISCTMService#start(int)
   */
  public Collection<ExecutionHandle> start(int executionId) throws SCTMException {
    try {
      ExecutionHandle[] handles = execService.startExecution(this.sessionId, executionId);
      logonRetryCount = 0;
      return convertToList(handles);
    } catch (RemoteException e) {
      if (handleLostSessionException(e))
        return start(executionId);
      LOGGER.log(Level.WARNING, e.getMessage(), e);
      throw new SCTMException(MessageFormat.format(Messages.getString("SCTMService.err.commonFatalError"), e.getMessage()));
    }
  }

  /* (non-Javadoc)
   * @see hudson.plugins.sctmexecutor.service.ISCTMService#start(int, java.lang.String)
   */
  public Collection<ExecutionHandle> start(int executionId, String buildNumber) throws SCTMException {
    try {
      ExecutionHandle[] handles = execService.startExecution(this.sessionId, executionId, buildNumber, 1, null);
      return convertToList(handles);
    } catch (RemoteException e) {
      if (handleLostSessionException(e)) {
        return start(executionId, buildNumber);
      }
      LOGGER.log(Level.WARNING, e.getMessage(), e);
      throw new SCTMException(MessageFormat.format(Messages.getString("SCTMService.err.commonFatalError"), e.getMessage())); //$NON-NLS-1$
    }
  }
  
  /* (non-Javadoc)
   * @see hudson.plugins.sctmexecutor.service.ISCTMService#isFinished(com.borland.tm.webservices.tmexecution.ExecutionHandle)
   */
  public boolean isFinished(ExecutionHandle handle) throws SCTMException {
    try {
      return execService.getStateOfExecution(sessionId, handle) < 0;
    } catch (RemoteException e) {
      if (handleLostSessionException(e))
        return isFinished(handle);
      LOGGER.log(Level.SEVERE, e.getMessage(), e);
      throw new SCTMException(MessageFormat.format(Messages.getString("SCTMService.err.commonFatalError"), e.getMessage())); //$NON-NLS-1$
    }
  }
  
  /* (non-Javadoc)
   * @see hudson.plugins.sctmexecutor.service.ISCTMService#getExecutionResult(com.borland.tm.webservices.tmexecution.ExecutionHandle)
   */
  public ExecutionResult getExecutionResult(ExecutionHandle handle) throws SCTMException {
    try {
      return execService.getExecutionResult(this.sessionId, handle);
    } catch (RemoteException e) {
      if (handleLostSessionException(e))
        return getExecutionResult(handle);
      LOGGER.log(Level.SEVERE, e.getMessage(), e);
      throw new SCTMException(MessageFormat.format(Messages.getString("SCTMService.err.commonFatalError"), e.getMessage())); //$NON-NLS-1$
    }
  }

  @Override
  public boolean addBuildNumber(int buildNumber, int nodeId) throws SCTMException {
    try {
      ExecutionNode node = execService.getNode(sessionId, nodeId);
      String version = getExecutionNodePropertyValue(node, "PROP_VERSIONNAME");
      String productName = getProductName(node);
      return adminService.addBuild(sessionId, productName, version, String.valueOf(buildNumber), "build number generated by hudson continuous integration system", true);
    } catch (RemoteException e) {
      if (handleLostSessionException(e))
        addBuildNumber(buildNumber, nodeId);
      LOGGER.log(Level.SEVERE, e.getMessage(), e);
      throw new SCTMException(MessageFormat.format(Messages.getString("SCTMService.err.commonFatalError"), e.getMessage())); //$NON-NLS-1$
    }
  }

  @Override
  public boolean buildNumberExists(int buildNumber, int nodeId) throws SCTMException {
    try {
      String[] builds = getAllBuildNumbersForProductAndVersion(nodeId);
      String value = String.valueOf(buildNumber);
      for (String build : builds) {
        if (value.equals(build))
          return true;
      }
      return false;
    } catch (RemoteException e) {
      if (handleLostSessionException(e))
        return buildNumberExists(buildNumber, nodeId);
      LOGGER.log(Level.SEVERE, e.getMessage(), e);
      throw new SCTMException(MessageFormat.format(Messages.getString("SCTMService.err.commonFatalError"), e.getMessage())); //$NON-NLS-1$
    }
  }

  @Override
  public int getLatestSCTMBuildnumber(int nodeId) throws SCTMException {
    try {
      String[] builds = getAllBuildNumbersForProductAndVersion(nodeId);
      int latestBuildnumber = -1;
      for (String bn : builds) {
        int buildnumber = 0;
        try {
          buildnumber = Integer.parseInt(bn);
          if (buildnumber > latestBuildnumber)
            latestBuildnumber = buildnumber;          
        } catch (NumberFormatException e) {
          LOGGER.warning(MessageFormat.format("Build number ({0}) is not a number. Ignoring it.", buildnumber));
        }
      }
      return latestBuildnumber;
    } catch (RemoteException e) {
      if (handleLostSessionException(e))
        return getLatestSCTMBuildnumber(nodeId);
      LOGGER.log(Level.SEVERE, e.getMessage(), e);
      throw new SCTMException(MessageFormat.format(Messages.getString("SCTMService.err.commonFatalError"), e.getMessage())); //$NON-NLS-1$
    }
  }

  @Override
  public String getExecDefinitionName(int execDefId) throws SCTMException {
    try {
      String name = execDefIdToName.get(execDefId);
      if (name == null) {
        ExecutionNode node = getExecDefNode(execDefId);
        name = getExecutionNodePropertyValue(node, "PROP_NAME");
        execDefIdToName.put(execDefId, name);
      }
      return name;  
    } catch (RemoteException e) {
      if (handleLostSessionException(e))
        return getExecDefinitionName(execDefId);
      LOGGER.log(Level.SEVERE, e.getMessage(), e);
      throw new SCTMException(MessageFormat.format(Messages.getString("SCTMService.err.commonFatalError"), e.getMessage())); //$NON-NLS-1$
    }
  }

  @Override
  public SPNamedEntity[] getResultFiles(int testDefRunId) throws SCTMException {
    try {
      return performerService.getExecutionFiles(sessionId, testDefRunId);
    } catch (RemoteException e) {
      if (handleLostSessionException(e))
        return getResultFiles(testDefRunId);
      LOGGER.log(Level.SEVERE, e.getMessage(), e);
      throw new SCTMException(MessageFormat.format(Messages.getString("SCTMService.err.commonFatalError"), e.getMessage())); //$NON-NLS-1$
    }
  }

  @Override
  public FilePath loadResultFile(int fileId, String fileName) {
    InputStream responseBodyAsStream = null;
    OutputStream fileOutputStream = null;
    try {
      URL url = new URL(String.format(this.serviceExchangeURL, this.sessionId, fileId));
      
      HttpClient client = new HttpClient();
      HttpMethod get = new GetMethod(url.toExternalForm());
      client.executeMethod(get);
      responseBodyAsStream = get.getResponseBodyAsStream();
      
      File resultFile = new File(fileName);
      fileOutputStream = new BufferedOutputStream(new FileOutputStream(resultFile));
      IOUtils.copy(responseBodyAsStream, fileOutputStream );
      fileOutputStream.flush();
      fileOutputStream.close();
      
      return new FilePath(resultFile);
    } catch (MalformedURLException e) {
      LOGGER.log(Level.SEVERE, "Cannot access to exchange service on SCTM. Check the service URL and if SCTM up and running.!", e);
    } catch (HttpException e) {
      // handle lost session here
    } catch (IOException e) {
      LOGGER.log(Level.SEVERE, "Cannot load result file from SCTM.", e);
    } finally {
      try {
        if (responseBodyAsStream != null)
          responseBodyAsStream.close();
        if (fileOutputStream != null)
          fileOutputStream.close();      
      } catch (IOException e) {
        LOGGER.log(Level.SEVERE, "Cannot close file stream.", e);
      }
    }
    return null;
  }
}
