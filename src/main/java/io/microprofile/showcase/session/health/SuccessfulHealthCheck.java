/**
 * 
 */
package io.microprofile.showcase.session.health;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.eclipse.microprofile.health.Health;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;

import io.microprofile.showcase.session.SessionResource;

/**
 * @author jagraj
 *
 */
@Health
@ApplicationScoped
public class SuccessfulHealthCheck implements HealthCheck {
	@Inject
	private SessionResource sessionResource;
	@Override
	public HealthCheckResponse call() {
		try {
			if(sessionResource.nessProbe().getStatus()==200) {
				return HealthCheckResponse.named("Session:successful-check").up().build();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return HealthCheckResponse.named("Session:failed-check").down().build();
	}
}
