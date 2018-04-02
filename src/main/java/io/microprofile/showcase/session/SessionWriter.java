package io.microprofile.showcase.session;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonWriter;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.ext.Provider;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.core.MultivaluedMap;


@Provider
@Produces(MediaType.APPLICATION_JSON)
public class SessionWriter implements MessageBodyWriter<Session> { 

    public SessionWriter() {}

    @Override
    public void writeTo(Session session, Class<?> clazz, Type type, Annotation[] annotations, MediaType mediaType,
                    MultivaluedMap<String, Object> map, OutputStream os) throws IOException, WebApplicationException 
    {
        JsonArrayBuilder speakers = Json.createArrayBuilder();
        for (String s : session.getSpeakers()) { 
            speakers.add (s);
        }
        JsonObject obj = Json.createObjectBuilder()
            .add("id", session.getId())
            .add("abstract", session.getAbstract())
            .add("title", session.getTitle())
            .add("code", session.getCode())
            .add("type", session.getType())
            .add("speakers", speakers.build())
            .add("schedule", session.getSchedule())
            .build();
        JsonWriter jsonWriter = Json.createWriter(os);
        jsonWriter.write(obj);
        jsonWriter.close();
    }

    @Override
    public long getSize(Session attendee, Class<?> clazz, Type type, Annotation[] annotations, MediaType mediaType) {
        return 0;
    }

    @Override 
    public boolean isWriteable (Class<?> clazz, Type type, Annotation[] annotations, MediaType mediaType) {
        return clazz.equals(Session.class);
    }
}
