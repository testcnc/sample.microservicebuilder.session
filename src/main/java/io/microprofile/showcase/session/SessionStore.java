package io.microprofile.showcase.session;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.eclipse.microprofile.metrics.Gauge;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.annotation.Counted;
import org.eclipse.microprofile.metrics.annotation.Metered;

import io.microprofile.showcase.bootstrap.BootstrapData;
import io.microprofile.showcase.bootstrap.SessionFactory;

/**
 * @author Heiko Braun
 * @since 16/09/16
 */
@ApplicationScoped
@Metered(name="io.microprofile.showcase.session.SessionStore.Type.Metered",tags="app=session")
public class SessionStore {

    @Inject
    BootstrapData bootstrapData;
    @Inject
	MetricRegistry metrics;
    

    private final ConcurrentHashMap<String, Session> storage = new ConcurrentHashMap<>();

    public Session save(final Session session) {
        session.setId(UUID.randomUUID().toString());
        storage.put(session.getId(), session);
        return session;
    }

    @PostConstruct
    private void initStore() {
        Logger.getLogger(SessionStore.class.getName()).log(Level.INFO, "Initialise sessions from bootstrap data");
		Gauge<Long> gauge = metrics.getGauges().get("io.microprofile.showcase.session.SessionStore.sessionGauge");
		if (gauge == null) {
			gauge = () -> { return (long) storage.size(); }; 
			metrics.register("io.microprofile.showcase.session.SessionStore.sessionGauge", gauge);
		}


        bootstrapData.getSessions()
            .forEach(bootstrap -> storage.put(bootstrap.getId(), SessionFactory.fromBootstrap(bootstrap)));

}
    
    @Counted(monotonic = true,tags="app=session")
    public Collection<Session> getSessions() {
        return storage.values();
    }

    public Optional<Session> find(final String sessionId) {
        final Session result = storage.get(sessionId);
        return result!=null ? Optional.of(result) : Optional.empty();
    }

    public Optional<Session> update(final String sessionId, final Session session) {
        final Optional<Session> existing = find(sessionId);
        if(existing.isPresent()) {
            session.setId(sessionId);
            storage.put(sessionId, session);
        }
        return existing;
    }

    public Optional<Session> remove(final String sessionId) {
        final Optional<Session> existing = find(sessionId);
        if(existing.isPresent()) {
            storage.remove(existing.get().getId());
        }
        return existing;
    }
}
