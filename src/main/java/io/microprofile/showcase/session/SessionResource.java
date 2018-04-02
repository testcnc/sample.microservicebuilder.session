/*
 * Copyright 2016 Microprofile.io
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.microprofile.showcase.session;

import static javax.ws.rs.core.MediaType.TEXT_PLAIN;

import java.util.Calendar;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.eclipse.microprofile.metrics.annotation.Counted;
import org.eclipse.microprofile.metrics.annotation.Metered;

import io.microprofile.showcase.session.health.HealthCheckBean;



/**
 * @author Ken Finnigan
 * @author Heiko Braun
 */
@Path("sessions")
@ApplicationScoped
@Metered(name="io.microprofile.showcase.session.SessionResource.Type.Metered",tags="app=session")
public class SessionResource {

    @Inject
    private SessionStore sessionStore;
	@Inject HealthCheckBean healthCheckBean;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Counted(monotonic = true,tags="app=session")
    public Collection<Session> allSessions() throws Exception {
        return sessionStore.getSessions();
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Counted(monotonic = true,tags="app=session")
    public Session createSession(final Session session) throws Exception {
        System.out.println ("createSession called for " + session.toString());
        Session result = sessionStore.save(session);
        System.out.println ("createSession returning " + result.toString());
        return result;
    }

    // For use as a k8s readinessProbe for this service
    @GET
    @Path("/nessProbe")
    @Produces(MediaType.TEXT_PLAIN)
    public Response nessProbe() throws Exception {

        return Response.ok("sessions ready at " + Calendar.getInstance().getTime()).build();
    }

    @GET
    @Path("/{sessionId}")
    @Produces(MediaType.APPLICATION_JSON)
    @Counted(monotonic = true,tags="app=session")
    public Response retrieveSession(@PathParam("sessionId") final String sessionId) throws Exception {
        final Optional<Session> result = sessionStore.find(sessionId);

        if (result.isPresent())
            return Response.ok(result.get()).build();
        else
            return Response.status(404).build();

    }

    @PUT
    @Path("/{sessionId}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Counted(monotonic = true,tags="app=session")
    public Response updateSession(@PathParam("sessionId") final String sessionId, final Session session) throws Exception {
        final Optional<Session> updated = sessionStore.update(sessionId, session);
        if (updated.isPresent())
            return Response.ok(updated.get()).build();
        else
            return Response.status(404).build();
    }

    @DELETE
    @Path("/{sessionId}")
    @Counted(monotonic = true,tags="app=session")
    public Response deleteSession(@PathParam("sessionId") final String sessionId) throws Exception {
        final Optional<Session> removed = sessionStore.remove(sessionId);
        if (removed.isPresent())
            return Response.ok().build();
        else
            return Response.status(404).build();

    }

    //TODO Add Search

    @GET
    @Path("/{sessionId}/speakers")
    @Produces(MediaType.APPLICATION_JSON)
    @Counted(monotonic = true,tags="app=session")
    public Response sessionSpeakers(@PathParam("sessionId") final String sessionId) throws Exception {

        final Optional<Session> session = sessionStore.getSessions().stream()
                .filter(s -> Objects.equals(s.getId(), sessionId))
                .findFirst();

        if (session.isPresent())
            return Response.ok(session.get().getSpeakers()).build();
        else
            return Response.status(404).build();

    }

    @PUT
    @Path("/{sessionId}/speakers/{speakerId}")
    @Produces(MediaType.APPLICATION_JSON)
    @Counted(monotonic = true,tags="app=session")
    public Response addSessionSpeaker(@PathParam("sessionId") final String sessionId, @PathParam("speakerId") final String speakerId) throws Exception {

        final Optional<Session> result = sessionStore.find(sessionId);

        if (result.isPresent()) {
            final Session session = result.get();
            final Collection<String> speakers = session.getSpeakers();
            speakers.add(speakerId);
            sessionStore.update(sessionId, session);
            return Response.ok(session).build();
        }

        return Response.status(404).build();
    }

    @DELETE
    @Path("/{sessionId}/speakers/{speakerId}")
    @Counted(monotonic = true,tags="app=session")
    public Response removeSessionSpeaker(@PathParam("sessionId") final String sessionId, @PathParam("speakerId") final String speakerId) throws Exception {
        final Optional<Session> result = sessionStore.find(sessionId);

        if (result.isPresent()) {
            final Session session = result.get();
            final Collection<String> speakers = session.getSpeakers();
            speakers.remove(speakerId);
            sessionStore.update(sessionId, session);
            return Response.ok(session).build();
        }

        return Response.status(404).build();
    }

    @POST
    @Path("/updateHealthStatus")
    @Produces(TEXT_PLAIN)
    @Consumes(TEXT_PLAIN)
    @Counted(name="io.microprofile.showcase.session.SessionResource.updateHealthStatus.monotonic.absolute(true)",monotonic=true,absolute=true,tags="app=vote")
    public void updateHealthStatus(@QueryParam("isAppDown") Boolean isAppDown) {
    	healthCheckBean.setIsAppDown(isAppDown);
    }
    

}
