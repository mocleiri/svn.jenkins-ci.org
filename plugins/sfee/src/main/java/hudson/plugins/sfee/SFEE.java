package hudson.plugins.sfee;

import hudson.plugins.sfee.webservice.IllegalArgumentFault;
import hudson.plugins.sfee.webservice.InvalidSessionFault;
import hudson.plugins.sfee.webservice.LoginFault;
import hudson.plugins.sfee.webservice.NoSuchObjectFault;
import hudson.plugins.sfee.webservice.PermissionDeniedFault;
import hudson.plugins.sfee.webservice.ProjectSoapList;
import hudson.plugins.sfee.webservice.ProjectSoapRow;
import hudson.plugins.sfee.webservice.RbacAppSoap;
import hudson.plugins.sfee.webservice.RoleClusterSoapList;
import hudson.plugins.sfee.webservice.RoleClusterSoapRow;
import hudson.plugins.sfee.webservice.RoleSoapList;
import hudson.plugins.sfee.webservice.RoleSoapRow;
import hudson.plugins.sfee.webservice.SourceForgeSoap;
import hudson.plugins.sfee.webservice.SystemFault;
import hudson.plugins.sfee.webservice.UserSoapRow;

import java.lang.reflect.Method;
import java.net.URL;
import java.rmi.RemoteException;

import org.acegisecurity.AuthenticationServiceException;
import org.acegisecurity.BadCredentialsException;
import org.apache.axis.AxisFault;

public class SFEE {

	/**
	 * Returns a stub for the webservice.
	 */
	@SuppressWarnings("unchecked")
	public static <T> T getSourceForgeApp(String host, Class<T> klazz) {
		try {
			URL endpoint = new URL("http://" + host
					+ ":8080/sf-soap43/services/"
					+ klazz.getSimpleName().replace("Soap", ""));
			String serviceName = klazz.getSimpleName();
			String packageName = klazz.getName().substring(0,
					klazz.getName().lastIndexOf('.'));
			serviceName = serviceName.substring(0, serviceName.length() - 4);
			String stubName = packageName + "." + serviceName
					+ "SoapServiceLocator";
			Class stubClass = Class.forName(stubName);
			Method m = stubClass.getMethod("get" + serviceName,
					new Class[] { URL.class });
			return (T) m.invoke(stubClass.newInstance(),
					new Object[] { endpoint });
		} catch (Exception e) {
			throw new RuntimeException("Error getting service stub", e);
		}
	}

	public static String createSession(String host, String userId,
			String password) {
		SourceForgeSoap sfSoap = getSourceForgeApp(host, SourceForgeSoap.class);
		try {
			String sessionId = sfSoap.login(userId, password);
			return sessionId;
		} catch (LoginFault e) {
			throw new BadCredentialsException("Wrong username or password.");
		} catch (SystemFault e) {
			throw new AuthenticationServiceException(
					"Error while contacting SFEE", e);
		} catch (AxisFault e) {
			if ("LoginFault".equals(e.getFaultCode().getLocalPart())) {
				throw new BadCredentialsException("Wrong username or password.");
			} else {
				throw new AuthenticationServiceException(
						"Error while contacting SFEE", e);
			}
		} catch (RemoteException e) {
			throw new AuthenticationServiceException(
					"Error while contacting SFEE", e);
		}
	}

	public static ProjectSoapRow[] getProjects(String sessionId, String host) {
		try {
			SourceForgeSoap sfSoap = getSourceForgeApp(host,
					SourceForgeSoap.class);
			ProjectSoapList projectList = sfSoap.getProjectList(sessionId);
			return projectList.getDataRows();
		} catch (InvalidSessionFault e) {
			throw new SFEEException("Error retrieving projects from " + host, e);
		} catch (SystemFault e) {
			throw new SFEEException("Error retrieving projects from " + host, e);
		} catch (RemoteException e) {
			throw new SFEEException("Error retrieving projects from " + host, e);
		}
	}

}
