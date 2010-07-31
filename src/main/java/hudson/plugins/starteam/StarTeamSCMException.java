package hudson.plugins.starteam;


/**
 * This exception signals an error with StarTeam SCM.
 * 
 * @author Ilkka Laukkanen <ilkka.s.laukkanen@gmail.com>
 * 
 */
public class StarTeamSCMException extends Exception {

	public StarTeamSCMException(String message, Throwable cause) {
		super(message, cause);
	}

	public StarTeamSCMException(String message) {
		super(message);
	}

}