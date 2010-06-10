package test;

import test.Party.Session;

import javax.crypto.spec.DHParameterSpec;
import java.security.AlgorithmParameterGenerator;
import java.security.AlgorithmParameters;
import java.security.NoSuchAlgorithmException;
import java.security.GeneralSecurityException;
import java.security.spec.InvalidParameterSpecException;

public class App {
    /**
     * Secure communication between two programs (think of NetBeans and Hudson) using DH key exchange.
     *
     * <p>
     * All the data that can be seen in this method are considered public and accessible to the attacker.
     */
    public static void main(String[] args) throws Exception {
        // hard-coded parameter set on both sides
        final DHParameterSpec spec = getParameter();

        // this scenario is between two programs --- NetBeans and Hudson
        Party netbeans = new Party("NetBeans",spec);
        Party hudson = new Party("Hudson",spec);

        // they initiate a session
        Session sn = netbeans.new Session();
        Session sh = hudson.new Session();

        // they exchange the key, and authenticate each other.
        //              they both confirm with the user that he's willing to let the other guy talk to it.
        keyexchange(sn, sh);

        // command is sent from Hudson to NetBeans
        //              for implementation over HTTP, the standard CSRF protection will prevent an attacker
        //              from fooling the user into making a harmful request.
        sn.receiveRequest(sh.createRequest("can you open Abc.java?"));

        // this completes a session.
        // it involves the total of 3 data exchange:
        // H -> N : key exchange
        // N -> H : key exchange
        // H -> N : command request

// Implementing this on HTTP
//=============================================
        // Hudson needs to find the endpoints of IDEs. It does so by creating <img src="http://localhost:PORT/info-about-hudson>
        // for ports in a certain range. NetBeans need to run a small HTTP server in one of the supported port range
        // and respond to this request. While using <script> would be easier for discovery, Use of <img> protects the web page
        // from receiving malicious JavaScript.

        // Data exchange can be implemented as a series of HTTP redirects. this works as long as
        // payload isn't too long.
        //
        // 1. user initiates the whole thing by POST-ing to http://hudson/endpoint with endpoint=http://netbeans/endpoint, request=open-abc
        // 2. this redirects to http://netbeans/endpoint with endpoint=http://hudson/endpoint?state=encrypted-state1, payload=key-exchange-data1
        // 3. this redirects to http://hudson/endpoint?state=encrypted-state1 with endpoint=http://netbeans/endpoint?state=encrypted-state2, payload=key-exchange-data2
        // 4. this redirects to http://netbeans/endpoint?state=encrypted-state2 with payload=command-request

        // the point of DH is to prevent a replay attack without using a timestamp --- session key uses
        // a random number that NetBeans generated, so it knows that the other party generated a request
        // after the key exchange took place.

        // the 2nd session will not involve any user confirmation as both parties remember other identities
        System.out.println("=============");
        sn = netbeans.new Session();
        sh = hudson.new Session();
        keyexchange(sn,sh);
        sn.receiveRequest(sh.createRequest("can you open Def.java?"));

    }

    private static void keyexchange(Session a, Session b) throws GeneralSecurityException {
        a.receivePublicSessionKeyOfOtherParty(b.getPublicPartOfSessionKey());
        b.receivePublicSessionKeyOfOtherParty(a.getPublicPartOfSessionKey());
    }


    /**
     * Publicly agreed-upon parameter set to be used for key exchange.
     */
    private static DHParameterSpec getParameter() throws NoSuchAlgorithmException, InvalidParameterSpecException {
        AlgorithmParameterGenerator paramGen = AlgorithmParameterGenerator.getInstance("DH");
        paramGen.init(1024);

        AlgorithmParameters params = paramGen.generateParameters();
        return params.getParameterSpec(DHParameterSpec.class);
    }
}
