package test;

import static test.ExchangedPacket.digest;

import javax.crypto.KeyAgreement;
import javax.crypto.SecretKey;
import javax.crypto.Cipher;
import javax.crypto.spec.DHParameterSpec;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.util.HashSet;
import java.util.Set;

/**
 * Model of the participating party to this protocol (namely, a program.)
 *
 * <p>
 * Private members indicate data inaccessible to the attacker.
 *
 * @author Kohsuke Kawaguchi
 */
public class Party {
    /**
     * Both side needs to have a-priori knowledge of the DH key exchange parameter.
     * We can just hard code this in our code.
     */
    private final DHParameterSpec spec;

    /**
     * RSA key pair that is the identity of this party. The other party in the protocol
     * relies on this to authenticate who it's talking to.
     */
    private final KeyPair identity;

    /**
     * Peers that the user acknowledged as a legitimate peer of the protocol.
     */
    private final Set<String> knownSenders = new HashSet<String>();

    /**
     * Used just so that the output shows who's doing what.
     */
    private final String name;

    public Party(String name, DHParameterSpec spec) throws GeneralSecurityException {
        this.name = name;
        this.spec = spec;
        this.identity = KeyPairGenerator.getInstance("RSA").generateKeyPair();
        System.out.println(name+" has identity "+digest(identity.getPublic().getEncoded()));
    }

    public class Session {
        /**
         * Diffie-Hellman key pair for generating a session key over HTTP.
         */
        protected final KeyPair key;

        /**
         * 3DES secret session key obtained after key exchange.
         */
        private SecretKey sessionKey;

        public Session() throws GeneralSecurityException {
            this.key = generateRandomKey();
        }

        /**
         * Generates a random key. Done privately on each side.
         */
        private KeyPair generateRandomKey() throws GeneralSecurityException {
            KeyPairGenerator kpg = KeyPairGenerator.getInstance(DIFFIE_HELLMAN);
            kpg.initialize(spec);
            return kpg.generateKeyPair();
        }

        /**
         * Each party exposes its public portion of the session key.
         */
        public ExchangedPacket getPublicPartOfSessionKey() throws GeneralSecurityException {
            // public part of the DH key exchange
            byte[] data = key.getPublic().getEncoded();

            Signature s = Signature.getInstance("SHA1withRSA");
            s.initSign(identity.getPrivate());
            s.update(data);
            byte[] signature = s.sign();

            return new ExchangedPacket(data, identity.getPublic().getEncoded(), signature);
        }

        /**
         * Each part receives the other party's {@link #getPublicPartOfSessionKey()} and produce a secret session key.
         */
        public void receivePublicSessionKeyOfOtherParty(ExchangedPacket ex) throws GeneralSecurityException {
            PublicKey publicKey = KeyFactory.getInstance(DIFFIE_HELLMAN).generatePublic(new X509EncodedKeySpec(ex.publicPartOfDH));

            KeyAgreement ka = KeyAgreement.getInstance(DIFFIE_HELLMAN);
            ka.init(key.getPrivate());
            ka.doPhase(publicKey, true);

            sessionKey = ka.generateSecret(SESSIONKEY_CRYPTO);

            // verify that this actually came from the right source
            Signature s = Signature.getInstance("SHA1withRSA");
            s.initVerify(KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(ex.identity)));
            s.update(ex.publicPartOfDH);
            if (!s.verify(ex.signature))
                throw new AssertionError("packet tampered");

            // do we know this sender?
            String sender = digest(ex.identity);
            if (knownSenders.add(sender))
                System.out.println(name+" asks the user if he's really willing to let "+sender+" operate "+name);
            // in a real implementation over HTTP, we can show the browser referer, in addition to the key fingerprint 
        }

        /**
         * Creates a request to be sent over an unsecured channel.
         *
         * @param request
         *      Arbitrary request, modeled as a string. The point of this demo is to show that
         *      the receiver will be able to decode this, with the assurance that it came from
         *      the authenticated sender. 
         */
        public byte[] createRequest(String request) throws GeneralSecurityException {
            Cipher c = Cipher.getInstance(SESSIONKEY_CRYPTO);
            c.init(Cipher.ENCRYPT_MODE,sessionKey);
            return c.doFinal(request.getBytes());
        }

        /**
         * Receives a request.
         */
        public void receiveRequest(byte[] request) throws GeneralSecurityException {
            Cipher c = Cipher.getInstance(SESSIONKEY_CRYPTO);
            c.init(Cipher.DECRYPT_MODE,sessionKey);
            System.out.println(name+" received a request: "+new String(c.doFinal(request)));
        }
    }

    /**
     * Key exchange algorithm.
     */
    private static final String DIFFIE_HELLMAN = "DiffieHellman";

    /**
     * Crypto algorithm used for the session.
     */
    private static final String SESSIONKEY_CRYPTO = "DES";
}
