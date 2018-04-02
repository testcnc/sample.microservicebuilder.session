package io.microprofile.showcase.session;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.lang.Exception;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URISyntaxException;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonWriter;

import org.arquillian.cube.kubernetes.annotations.Named;
import org.arquillian.cube.kubernetes.annotations.PortForward;
import org.arquillian.cube.kubernetes.impl.requirement.RequiresKubernetes;
import org.arquillian.cube.requirement.ArquillianConditionalRunner;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(ArquillianConditionalRunner.class)
@RequiresKubernetes
public class SessionIT { 

    @Named("microservice-session-service")
    // PortForward is REQUIRED for clients outside Kubernetes and FORBIDDEN for those within :(
    // @PortForward
    @ArquillianResource
    URL url;

    enum Method {GET, PUT, POST, DELETE};

    /* 
    August 10th: two major known problems: 
    1. @PortForward is mandatory if test client is running outside kube, but forbidden if inside Kube
    2. Jax-rs client does not work inside Kube when calling services in other namespaces. 
       With a client running on Kubernetes I'm seeing
         java.net.SocketTimeoutException: connect timed out
         at java.net.HttpURLConnection.getResponseCode(HttpURLConnection.java:480)
  	     at org.apache.cxf.transport.http.URLConnectionHTTPConduit$URLConnectionWrappedOutputStream.getResponseCode(URLConnectionHTTPConduit.java:370)
         at org.apache.cxf.transport.http.HTTPConduit$WrappedOutputStream.doProcessResponseCode(HTTPConduit.java:1587)
       for many operations. 
    */

    // Test SessionStore.initStore(), SessionResource.allSessions()
    @Test
    public void testGetSessionsNoJaxRS () throws Exception { 
        URL url = getBaseURI().toURL();
        String allSessions = getContent (url, Method.GET, null);
        JsonReader rdr = Json.createReader(new StringReader(allSessions));
        JsonArray arr = rdr.readArray();
        Map<String, Session> sessions = new HashMap<>();
        for (int i = 0; i < arr.size(); i++) {
            JsonObject jsonObject = arr.getJsonObject(i);
            Session session = SessionReader.buildSession (jsonObject);
            sessions.put(session.getId(), session);
        }
        
        Session s44 = sessions.get("44");
        assertTrue (s44.getAbstract().contains("Writing code is easy"));

        Session s45 = sessions.get("45");
        assertTrue (s45.getTitle().contains("Creating Amazing Game Concepts"));

        Session s47 = sessions.get("47");
        Collection<String> speakers = s47.getSpeakers();
        assertTrue ("Expected 159 in " + speakers, speakers.contains("159"));
    }

    // Test SessionResource.createSession()
    @Test
    public void testAddSessionNoJaxRS () throws Exception { 

        JsonObject newSession = Json.createObjectBuilder()
            .add("id", "999")
            .add("abstract", "Very abstract")
            .add("title", "Compelling title")
            .add("code", "CON9999")
            .add("type", "Conference Session")
            .add("speakers", Json.createArrayBuilder().add("122"))
            .add("schedule", 999)
            .build();

        StringWriter sw = new StringWriter();
        JsonWriter jw = Json.createWriter(sw);
        jw.write(newSession);
        String postThis = sw.getBuffer().toString();

        System.out.println ("Post this: `" + postThis + "`");

        String result = getContent(getBaseURI().toURL(), Method.POST, postThis);

        System.out.println ("testAddSessionNoJaxRS got `" + result + "`");

        // Could use GSON to convert the result to a class object? 
        // https://stackoverflow.com/questions/35210070/converting-jsonobject-to-java-object
        // WebTarget.request() will build class instances but it has to make a request - which doesn't work across namespaces

        assertTrue ("Expected 'Very abstract' in " + result, result.contains("Very abstract"));
        assertTrue ("Expected ''Compelling title' in " + result, result.contains("Compelling title"));
           
    }

    private URI getBaseURI() throws URISyntaxException { 
        assertNotNull (url);
        System.out.println ("URL = " + url.toString());
        String uri = url.toURI().toString();
        if (!uri.endsWith("/")) {
            uri += "/";
        }
        uri += "sessions";
        return new URI (uri);
    }

    private String getContent (URL url, Method method, String payload) throws IOException { 
        StringBuilder response = new StringBuilder();
        int retries = 5;
        while (response.toString().isEmpty() && retries > 0) { 
            try { 
                Writer writer = null;
                BufferedReader reader;
                HttpURLConnection urlc = (HttpURLConnection) url.openConnection();
                urlc.setRequestProperty("content-type", "application/json");
                urlc.setRequestMethod(method.toString());

                if (method == Method.PUT || method == Method.POST) { 
                    urlc.setDoOutput(true);
                    writer = new OutputStreamWriter(urlc.getOutputStream());
                    writer.write(payload);
                    writer.flush();
                }
                reader = new BufferedReader (new InputStreamReader(urlc.getInputStream()));
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append (line + "\n");
                }
                if (writer != null) writer.close();
                reader.close();
            } catch (IOException iox) { 
                if (retries <= 1) { 
                    throw iox;
                }
                try { 
                    Thread.sleep (5000);
                } catch (InterruptedException ix) {}
            } finally { 
                retries--;
            }
        }
        return response.toString();
    }

    /*

    The following code is mothballed while we dig into the JAXRS client problems seen on Kubernetes when calling
    URLs in a different namespace

    import java.io.InputStream;
import java.net.URLConnection;
import java.util.ArrayList;
import javax.json.JsonNumber;
import javax.json.JsonString;
import javax.ws.rs.client.Client;
import javax.ws.rs.core.Response;
import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import static org.junit.Assert.assertEquals;

    // Test SessionResource.createSession()
    // @Test
    public void testAddSession() throws Exception { 

        JsonObject newSession = Json.createObjectBuilder()
            .add("id", "999")
            .add("abstract", "Very abstract")
            .add("title", "Compelling title")
            .add("code", "CON9999")
            .add("type", "Conference Session")
            .add("speakers", Json.createArrayBuilder().add("122"))
            .add("schedule", 999)
            .build();
        
        Client client = ClientBuilder.newClient();
        client.register(SessionWriter.class);
        client.register(SessionReader.class);
        Session s = SessionReader.buildSession (newSession);
        Entity<Session> entity = Entity.entity(s, MediaType.APPLICATION_JSON);
        WebTarget target = client.target(url.toString()).path("sessions");
        Builder builder = target.request(MediaType.APPLICATION_JSON_TYPE);
        
        //target.request(MediaType.APPLICATION_JSON_TYPE).post(entity, Response.class);

        Response resp = builder.post(entity, Response.class);
        
        System.out.println ("Posted status: rc = " + resp.getStatus());

        Session registeredSession = resp.readEntity(Session.class);
        System.out.println("Posted data: " + render(registeredSession));

        assertEquals ("Wrong abstract", "Very abstract", registeredSession.getAbstract());
        assertEquals ("Wrong title", "Compelling title", registeredSession.getTitle());
    }

    // Test SessionStore.initStore(), SessionResource.allSessions()
    //@Test
    public void checkBootstrapSessions() throws Exception { 
        Map<String, Session> sessions = getSessions();
        
        Session s44 = sessions.get("44");
        assertTrue (s44.getAbstract().contains("Writing code is easy"));

        Session s45 = sessions.get("45");
        assertTrue (s45.getTitle().contains("Creating Amazing Game Concepts"));

        Session s47 = sessions.get("47");
        Collection<String> speakers = s47.getSpeakers();
        assertTrue ("Expected 159 in " + speakers, speakers.contains("159"));
    }

        private String render (Session session) { 
        StringBuffer result = new StringBuffer();

        result.append("id -> " + session.getId() + "\n");
        result.append("abstract -> " + session.getAbstract() + "\n");
        result.append("title -> " + session.getTitle() + "\n");
        result.append("code -> " + session.getCode() + "\n");
        result.append("type -> " + session.getType() + "\n");
        result.append("speakers -> " + session.getSpeakers() + "\n");
        result.append("schedule -> " + session.getSchedule() + "\n");

        return result.toString();
    }

    private Map<String, Session> getSessions () throws URISyntaxException { 
        Map<String, Session> results = new HashMap<>();
        Client client = ClientBuilder.newBuilder().build();
        Response resp = client.target(getBaseURI()).request(MediaType.APPLICATION_JSON).get();
        JsonReader rdr = Json.createReader((InputStream)resp.getEntity());
        JsonArray arr = rdr.readArray();
        for (int i = 0; i < arr.size(); i++) {
            JsonObject jsonObject = arr.getJsonObject(i);
            Session session = SessionReader.buildSession (jsonObject);
            results.put(session.getId(), session);
        }
        return results;
    }

    */
} 