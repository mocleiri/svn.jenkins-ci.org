package test;

import org.bouncycastle.util.encoders.Base64;
import sun.security.util.DerInputStream;
import sun.security.util.DerValue;
import sun.security.x509.CertAndKeyGen;
import sun.security.x509.X500Name;

import java.io.*;
import java.math.BigInteger;
import java.security.*;
import java.security.cert.*;
import java.security.cert.Certificate;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAPrivateKeySpec;
import java.util.Date;

/**
 * Generates a self-signed certificate into a key store.
 */
public class App 
{
    public static void main(String[] args) throws Exception {
//        generateSelfSignedCert();

//        CertificateFactory cf = CertificateFactory.getInstance("X509");
//        Certificate crt = cf.generateCertificate(new FileInputStream("/tmp/server.crt"));
//        PrivateKey key = ((KeyPair)new PEMReader(new FileReader("/tmp/server.key")).readObject()).getPrivate();

        System.out.println(readPEMRSAPrivateKey(new FileReader("/tmp/server.key")));
    }

    private static PrivateKey readPEMRSAPrivateKey(Reader reader) throws IOException, GeneralSecurityException {
        // TODO: should have more robust format error handling
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            BufferedReader r = new BufferedReader(reader);
            String line;
            boolean in = false;
            while ((line=r.readLine())!=null) {
                if (line.startsWith("-----")) {
                    in = !in;
                    continue;
                }
                if (in)
                    baos.write(Base64.decode(line));
            }
        } finally {
            reader.close();
        }


        DerInputStream dis = new DerInputStream(baos.toByteArray());
        DerValue[] seq = dis.getSequence(0);

        // int v = seq[0].getInteger();
        BigInteger mod = seq[1].getBigInteger();
        // pubExpo
        BigInteger privExpo = seq[3].getBigInteger();
        // p1, p2, exp1, exp2, crtCoef

        KeyFactory kf = KeyFactory.getInstance("RSA");
        return kf.generatePrivate (new RSAPrivateKeySpec(mod,privExpo));
    }

    private static void generateSelfSignedCert() throws Exception {
        CertAndKeyGen ckg = new CertAndKeyGen("RSA", "SHA1WithRSA", null);
        ckg.generate(1024);
        PrivateKey privKey = ckg.getPrivateKey();

        X500Name xn = new X500Name("Test site", "Unknown", "Unknown", "Unknown");
        X509Certificate cert = ckg.getSelfCertificate(xn, new Date(), 3650L * 24 * 60 * 60);

        KeyStore ks = KeyStore.getInstance("JKS");
        ks.load(null);
        ks.setKeyEntry("hudson", privKey, "changeit".toCharArray(), new Certificate[]{cert});
        FileOutputStream fos = new FileOutputStream("key");
        ks.store(fos,"changeit".toCharArray());
        fos.close();
    }
}
