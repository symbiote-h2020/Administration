package eu.h2020.symbiote.security.helpers;

import eu.h2020.symbiote.security.commons.Certificate;
import eu.h2020.symbiote.security.commons.SecurityConstants;
import eu.h2020.symbiote.security.commons.exceptions.custom.InvalidArgumentsException;
import eu.h2020.symbiote.security.commons.exceptions.custom.NotExistingUserException;
import eu.h2020.symbiote.security.commons.exceptions.custom.ValidationException;
import eu.h2020.symbiote.security.commons.exceptions.custom.WrongCredentialsException;
import eu.h2020.symbiote.security.communication.AAMClient;
import eu.h2020.symbiote.security.communication.payloads.CertificateRequest;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.*;
import java.security.cert.CertificateException;

/**
 * Builds a key store with platform certificate and it's issuer
 *
 * @author Jakub Toczek (PSNC)
 */
public class PlatformAAMCertificateKeyStoreFactory {

    private static Log log = LogFactory.getLog(PlatformAAMCertificateKeyStoreFactory.class);

    /**
     * Generates a platform AAM keystore
     * TODO fill properly all the fields to get the platform AAM keystore
     */
    public static void main(String[] args) {
        // given to you for integration, in the end should be available in public
        // from spring bootstrap file: symbiote.coreaam.url
        String coreAAMAddress = "";
        // of the user registered through administration in the Symbiote Core
        String platformOwnerUsername = "";
        String platformOwnerPassword = "";
        // of the platform registered to the given platform Owner
        String platformId = "";

        // where you want to have the keystore generated
        String keyStorePath = "";
        // used to access the keystore. MUST NOT be longer than 7 chars
        // from spring bootstrap file: aam.security.KEY_STORE_PASSWORD
        String keyStorePassword = "";
        // used to access the AAM private key. MUST NOT be longer than 7 chars
        // from spring bootstrap file: aam.security.PV_KEY_PASSWORD
        String privateKeyPassword = "";
        // platform AAM key/certificate alias... case INSENSITIVE (all lowercase)
        // from spring bootstrap file: aam.security.CERTIFICATE_ALIAS
        String aamCertificateAlias = "";
        // root CA certificate alias... case INSENSITIVE (all lowercase)
        // from spring bootstrap file:  aam.security.ROOT_CA_CERTIFICATE_ALIAS
        String rootCACertificateAlias = "";

        try {
            getPlatformAAMKeystore(
                    coreAAMAddress,
                    platformOwnerUsername,
                    platformOwnerPassword,
                    platformId,
                    keyStorePath,
                    keyStorePassword,
                    rootCACertificateAlias,
                    aamCertificateAlias,
                    privateKeyPassword
            );
            log.info("OK");
        } catch (WrongCredentialsException | ValidationException | KeyStoreException | IOException | NoSuchAlgorithmException | CertificateException | InvalidArgumentsException | InvalidAlgorithmParameterException | NoSuchProviderException | NotExistingUserException e) {
            log.error(e.getMessage());
            log.error(e.getCause());
        }
    }

    public static void getPlatformAAMKeystore(String coreAAMAddress,
                                              String platformOwnerUsername,
                                              String platformOwnerPassword,
                                              String platformId,
                                              String keyStorePath,
                                              String keyStorePassword,
                                              String rootCACertificateAlias,
                                              String aamCertificateAlias,
                                              String aamCertificatePrivateKeyPassword) throws
            KeyStoreException,
            IOException,
            CertificateException,
            NoSuchAlgorithmException,
            NoSuchProviderException,
            InvalidAlgorithmParameterException,
            InvalidArgumentsException,
            WrongCredentialsException,
            NotExistingUserException,
            ValidationException {

        File keyStoreFile = new File(keyStorePath);

        if (keyStorePassword.length() > 7 || aamCertificatePrivateKeyPassword.length() > 7)
            throw new InvalidArgumentsException("The passwords must not be longer that 7 chars... don't ask why...");

        ECDSAHelper.enableECDSAProvider();
        KeyStore ks = getKeystore(keyStorePath, keyStorePassword);
        log.info("Key Store acquired.");
        KeyPair pair = CryptoHelper.createKeyPair();
        log.info("Key pair for the platform AAM generated.");
        String csr = CryptoHelper.buildPlatformCertificateSigningRequestPEM(platformId, pair);
        log.info("CSR for the platform AAM generated.");
        CertificateRequest request = new CertificateRequest(platformOwnerUsername, platformOwnerPassword, platformId, csr);
        log.info("Request created");
        AAMClient aamClient = new AAMClient(coreAAMAddress);
        log.info("Connection with AAMClient established");
        String platformAAMCertificate = aamClient.signCertificateRequest(request);
        log.info("Platform Certificate acquired");
        if (!aamClient.getAvailableAAMs().getAvailableAAMs().get(platformId).getAamCACertificate().getCertificateString().equals(platformAAMCertificate)) {
            throw new CertificateException("Wrong certificate under the platformId");
        }
        Certificate aamCertificate = aamClient.getAvailableAAMs().getAvailableAAMs().get(SecurityConstants.CORE_AAM_INSTANCE_ID).getAamCACertificate();
        ks.setCertificateEntry(rootCACertificateAlias, aamCertificate.getX509());
        ks.setKeyEntry(aamCertificateAlias, pair.getPrivate(), aamCertificatePrivateKeyPassword.toCharArray(),
                new java.security.cert.Certificate[]{CryptoHelper.convertPEMToX509(platformAAMCertificate), aamCertificate.getX509()});
        FileOutputStream fOut = new FileOutputStream(keyStoreFile);
        try {
            ks.store(fOut, keyStorePassword.toCharArray());
        } catch (Exception e) {
            log.error(e);
            try {
                fOut.close();
                keyStoreFile.delete();
            } catch (IOException ioe) {
                // Do nothing. We're doomed.
            }
            throw e;
        }
        fOut.close();
        log.info("Certificates and private key saved in keystore");
    }

    private static KeyStore getKeystore(String path, String password) throws
            KeyStoreException,
            IOException,
            CertificateException,
            NoSuchAlgorithmException, NoSuchProviderException {
        ECDSAHelper.enableECDSAProvider();
        KeyStore trustStore = KeyStore.getInstance("PKCS12", "BC");
        File f = new File(path);
        if (f.exists() && !f.isDirectory()) {
            log.warn("KeyStore already exists. It was overridden");
            f.delete();
        }
        trustStore.load(null, null);
        return trustStore;
    }
}
