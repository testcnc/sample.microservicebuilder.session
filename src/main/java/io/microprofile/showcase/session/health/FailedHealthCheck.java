package io.microprofile.showcase.session.health;

import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.health.Health;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;

import io.microprofile.showcase.session.SessionResource;

/**
 * 
 * @author jagraj
 *
 */
@Health
@ApplicationScoped
public class FailedHealthCheck implements HealthCheck{
	@Inject
	private SessionResource sessionResource;
	@Inject 
	@ConfigProperty(name="isAppDown") Optional<String> isAppDown;
	@Inject HealthCheckBean healthCheckBean;
	
    @Override
    public HealthCheckResponse call() {
		try {
			if(sessionResource.nessProbe().getStatus()!=200 || ((isAppDown.isPresent()) && (isAppDown.get().equals("true")))) {
				return HealthCheckResponse.named("Session:failed-check").down().build();
			}
			else if(healthCheckBean.getIsAppDown()!=null && healthCheckBean.getIsAppDown().booleanValue()==true) {
				return HealthCheckResponse.named("Session:failed-check").down().build();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
        return HealthCheckResponse.named("Session:successful-check").up().build();
    }
}
