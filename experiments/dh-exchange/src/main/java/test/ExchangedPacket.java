package test;

import javax.xml.bind.DatatypeConverter;

/**
 * @author Kohsuke Kawaguchi
 */
public class ExchangedPacket {
    /**
     * Public portion of the DH key exchange.
     */
    public final byte[] publicPartOfDH;
    /**
     * SHA1+RSA signature of {@link #publicPartOfDH}. 
     */
    public final byte[] signature;
    /**
     * RSA public key that identifies the sender.
     */
    public final byte[] identity;

    public ExchangedPacket(byte[] publicPartOfDH, byte[] identity, byte[] signature) {
        this.publicPartOfDH = publicPartOfDH;
        this.identity = identity;
        this.signature = signature;
    }

    /**
     * Digest a long byte sequence into a short one.
     */
    public static String digest(byte[] data) {
        byte[] digest = new byte[4];
        for (int i=0; i< data.length; i++)
            digest[i%digest.length] ^= data[i];
        return DatatypeConverter.printHexBinary(digest);
    }
}
