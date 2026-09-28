package secret.kinetic.utils.misc;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;


















public final class IpChanger {

    
    private static final String SOAP_BODY =
            "<?xml version=\"1.0\" encoding=\"utf-8\"?>" +
            "<s:Envelope xmlns:s=\"http://schemas.xmlsoap.org/soap/envelope/\" " +
            "s:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\">" +
            "<s:Body><u:ForceTermination xmlns:u=\"urn:schemas-upnp-org:service:WANIPConnection:1\"/></s:Body>" +
            "</s:Envelope>";

    
    private static final String[] BASE_URLS = {
            "http://fritz.box:49000/",
            "https://fritz.box:49443/"
    };

    private static final String[] PREFIXES = {
            "igdupnp/control/",
            "upnp/control/"
    };

    
    private static final String[][] SERVICES = {
            { "WANIPConn1",  "urn:schemas-upnp-org:service:WANIPConnection:1#ForceTermination" },
            { "WANPPPConn1", "urn:schemas-upnp-org:service:WANPPPConnection:1#ForceTermination" }
    };

    
    private static final int CONNECT_TIMEOUT_MS = 3000;
    private static final int REQUEST_TIMEOUT_MS = 5000;

    
    private static volatile boolean success;

    
    private static volatile SSLSocketFactory trustAllFactory;

    private IpChanger() {
    }

    
    public static boolean isSuccess() {
        return success;
    }

    




    public static String run() {
        success = false;
        StringBuilder log = new StringBuilder();

        for (String base : BASE_URLS) {
            for (String prefix : PREFIXES) {
                for (String[] service : SERVICES) {
                    String url = base + prefix + service[0];
                    try {
                        int status = postForceTermination(url, service[1]);
                        if (status == 200 || status == 204) {
                            success = true;
                            return "ForceTermination sent to " + url;
                        }
                        log.append("Status ").append(status).append(" at ").append(url).append(System.lineSeparator());
                    } catch (Exception e) {
                        log.append("Error at ").append(url).append(": ").append(e).append(System.lineSeparator());
                    }
                }
            }
        }

        
        System.out.println(log);
        return "Could not reach the FRITZ!Box";
    }

    



    private static int postForceTermination(String url, String soapAction) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        if (connection instanceof HttpsURLConnection) {
            HttpsURLConnection https = (HttpsURLConnection) connection;
            https.setSSLSocketFactory(trustAllSocketFactory());
            https.setHostnameVerifier((hostname, session) -> true);
        }
        try {
            connection.setRequestMethod("POST");
            connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
            connection.setReadTimeout(REQUEST_TIMEOUT_MS);
            connection.setUseCaches(false);
            connection.setRequestProperty("Content-Type", "text/xml; charset=\"utf-8\"");
            connection.setRequestProperty("SOAPAction", "\"" + soapAction + "\"");
            connection.setRequestProperty("Connection", "close");

            byte[] body = SOAP_BODY.getBytes(StandardCharsets.UTF_8);
            connection.setDoOutput(true);
            connection.setFixedLengthStreamingMode(body.length);
            try (OutputStream out = connection.getOutputStream()) {
                out.write(body);
            }
            return connection.getResponseCode();
        } finally {
            connection.disconnect();
        }
    }

    
    private static SSLSocketFactory trustAllSocketFactory() throws Exception {
        SSLSocketFactory factory = trustAllFactory;
        if (factory == null) {
            synchronized (IpChanger.class) {
                factory = trustAllFactory;
                if (factory == null) {
                    TrustManager[] trustAll = {
                            new X509TrustManager() {
                                @Override
                                public X509Certificate[] getAcceptedIssuers() {
                                    return new X509Certificate[0];
                                }

                                @Override
                                public void checkClientTrusted(X509Certificate[] chain, String authType) {
                                }

                                @Override
                                public void checkServerTrusted(X509Certificate[] chain, String authType) {
                                }
                            }
                    };
                    SSLContext context = SSLContext.getInstance("TLS");
                    context.init(null, trustAll, new SecureRandom());
                    trustAllFactory = factory = context.getSocketFactory();
                }
            }
        }
        return factory;
    }
}
