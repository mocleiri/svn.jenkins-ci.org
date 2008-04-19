package hudson.plugins.sfee;

import org.acegisecurity.GrantedAuthority;
import org.acegisecurity.providers.UsernamePasswordAuthenticationToken;

public class SFEEAuthenticationToken extends UsernamePasswordAuthenticationToken {

	private String sessionId;
	private long lastSessionRequest;

	public SFEEAuthenticationToken(Object principal, Object credentials,
			GrantedAuthority[] authorities, String sessionId) {
		super(principal, credentials, authorities);
		this.sessionId = sessionId;
	}
	
	public synchronized String getSessionId() {
		if (sessionId == null
				|| (System.currentTimeMillis() - lastSessionRequest > 15000)) {
			sessionId = SourceForgeSite.DESCRIPTOR.getSite().createSession(getName(), (String) getCredentials());
			lastSessionRequest = System.currentTimeMillis();
		}
		return sessionId;
	}


}
