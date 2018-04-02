package io.microprofile.showcase.session;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;

import javax.json.Json;
import javax.json.JsonNumber;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonString;
import javax.ws.rs.Consumes;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.Provider;
import javax.ws.rs.core.MultivaluedMap;

@Provider
@Consumes(MediaType.APPLICATION_JSON)
public class SessionReader implements MessageBodyReader<Session> {

    @Override
    public boolean isReadable(Class<?> clazz, Type type, Annotation[] annotations, MediaType mediaType) {
       return clazz.equals(Session.class);
    }

    @Override
    public Session readFrom(Class<Session> clazz, Type type, Annotation[] annotations, MediaType mediaType,
                             MultivaluedMap<String, String> map, InputStream is) throws IOException, WebApplicationException {
        JsonReader reader = Json.createReader(is);
        JsonObject jsonObject = reader.readObject();
        Session result = buildSession (jsonObject);
        reader.close();
        return result;
    }

    public static Session buildSession (JsonObject jsonObject) { 
        System.out.println ("Into SessionReader.buildSession with " + jsonObject.toString());
        JsonString idj = jsonObject.getJsonString("id");
        String id = idj.getString();
        Collection<String> speakers = new ArrayList<>();
        for (JsonString s : jsonObject.getJsonArray("speakers").getValuesAs(JsonString.class)) { 
          speakers.add(s.getString());
        }
        JsonNumber schedule = jsonObject.getJsonNumber("schedule");
        Session result = new Session (id, jsonObject);
        result.setSpeakers (speakers);
        result.setSchedule (schedule.intValue());

        System.out.println ("SessionReader.buildSession built " + result.toString());
        return result;
    }
}