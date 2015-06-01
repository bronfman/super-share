package com.robertlancer.util;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.services.json.AbstractGoogleJsonClient;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson.JacksonFactory;
import com.google.api.client.util.Maps;
import com.google.api.services.admin.directory.Directory;
import com.google.api.services.admin.directory.DirectoryScopes;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.GmailScopes;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.security.*;
import java.security.cert.CertificateException;
import java.util.Collection;
import java.util.Map;

public class ServiceFactory {

    public static class KeyParts {

        public KeyParts(String emailAddress, String keyFingerprints) throws KeyStoreException, CertificateException, NoSuchAlgorithmException, IOException, UnrecoverableKeyException {
            this.emailAddress = emailAddress;
            this.keyFingerprints = keyFingerprints;
            InputStream iss = new FileInputStream(new File("WEB-INF/privatekeys/" + keyFingerprints + "-privatekey.p12"));
            KeyStore keystore = KeyStore.getInstance("PKCS12");
            keystore.load(iss, "notasecret".toCharArray());
            key = (PrivateKey) keystore.getKey("privatekey", "notasecret".toCharArray());
        }

        public String emailAddress;
        public String keyFingerprints;
        public PrivateKey key;

    }

    public static KeyParts key;

    static {
        try {
            key = new KeyParts(
                    "805415761755-pfu94u912lqlq3oa5uhpbo0s7jl1apln@developer.gserviceaccount.com",
                    "d830d98c5bb43999fbc13d5ea10e611e4f38823c");
        } catch (UnrecoverableKeyException e) {
            e.printStackTrace(System.err);
        } catch (CertificateException e) {
            e.printStackTrace(System.err);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace(System.err);
        } catch (KeyStoreException e) {
            e.printStackTrace(System.err);
        } catch (IOException e) {
            e.printStackTrace(System.err);
        }
    }


    public static Gmail getGmailService(String email) {
        return (Gmail) getService_Singleton(email, Gmail.class);
    }

    public static Drive getDriveService(String email) {
        return (Drive) getService_Singleton(email, Drive.class);
    }


    public static String getAccessToken(AbstractGoogleJsonClient jsonClient) {

        GoogleCredential googleCredential = ((Intializer) jsonClient.getRequestFactory().getInitializer()).credential;
        if (googleCredential.getAccessToken() == null || googleCredential.getExpirationTimeMilliseconds() <= System.currentTimeMillis()) {
            try {
                System.out.println("Access token is null so refreshing token");
                googleCredential.refreshToken();
            } catch (IOException e) {
                System.err.println("Error refreshing token for " + googleCredential.getServiceAccountUser());
                // e.printStackTrace();
                throw new ClientException(e);
            }
        } else {
            //    System.out.println("Not refreshing access token.");
        }
        return googleCredential.getAccessToken();
    }


    private static Map<String, AbstractGoogleJsonClient> SERVICES_SINGLETONS = Maps.newHashMap();

    private static AbstractGoogleJsonClient getService_Singleton(String email, Class classOf) {

        String key = email + classOf.getName().toLowerCase();

        AbstractGoogleJsonClient clients = SERVICES_SINGLETONS.get(key);

        if (clients == null) {
            clients = getService(email, classOf);
            SERVICES_SINGLETONS.put(key, clients);
        }

        return clients;
    }

    private static AbstractGoogleJsonClient getService(String email, Class classOf) {

        Collection<String> sets = null;

        String classSimpleName = classOf.getSimpleName();

        java.util.Set<String> set = new java.util.HashSet<String>();
        set.add(DriveScopes.DRIVE_READONLY);
        sets = java.util.Collections.unmodifiableSet(set);

        HttpTransport httpTransport = new NetHttpTransport();
        JacksonFactory jsonFactory = new JacksonFactory();

        final GoogleCredential credential = new GoogleCredential.Builder()
                .setTransport(httpTransport)
                .setJsonFactory(jsonFactory)
                .setServiceAccountId(key.emailAddress)
                .setServiceAccountScopes(sets)
                .setServiceAccountUser(email)
                .setServiceAccountPrivateKey(key.key)
                .build();

        try {
            return ((AbstractGoogleJsonClient.Builder) getInnerClass(classOf, "Builder").getConstructors()[0].newInstance(httpTransport, jsonFactory, null))
                    .setApplicationName("Robert Lancer")
                    .setHttpRequestInitializer(new Intializer(credential))
                    .build();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }

        return null;
    }

    public static Class getInnerClass(Class classOf, String className) {

        for (Class inner : classOf.getDeclaredClasses()) {
            if (inner.getName().toLowerCase().endsWith(className.toLowerCase())) ;
            return inner;
        }

        return null;
    }

    public static class Intializer implements HttpRequestInitializer {

        public GoogleCredential credential;

        public Intializer(GoogleCredential credential) {
            this.credential = credential;
        }

        @Override
        public void initialize(HttpRequest httpRequest) throws IOException {
            credential.initialize(httpRequest);
            httpRequest.setConnectTimeout(3 * 60000);
            httpRequest.setReadTimeout(3 * 60000);
        }
    }

    public static class ClientException extends RuntimeException {

        public Exception e;

        public ClientException(Exception e) {
            this.e = e;
        }
    }
}



