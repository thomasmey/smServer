package smServer.filter;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Priority;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.Provider;

@Provider
@Priority(Priorities.AUTHORIZATION)
public class AccessFilter implements ContainerRequestFilter {

	private static final String ENV_TOKEN = "TOKEN";

	private Logger log;

	public AccessFilter() {
		log = Logger.getLogger(AccessFilter.class.getName());
	}

	@Override
	public void filter(ContainerRequestContext requestContext) throws IOException {

		if(log.isLoggable(Level.FINE))
			log.log(Level.FINE, "Processing request path {0}", requestContext.getUriInfo().getPath());

		String authHeader = requestContext.getHeaderString("Authorization");
		String bearer = "Bearer ";
		if(authHeader == null || !authHeader.startsWith(bearer)) {
			if(log.isLoggable(Level.FINE))
				log.log(Level.FINE, "Abort because of missing Auth header!");
			requestContext.abortWith(Response.status(Status.FORBIDDEN).build());
			return;
		}

		String bearerToken = authHeader.substring(bearer.length());

		if(log.isLoggable(Level.FINE))
			log.log(Level.FINE, "Token {0}", bearerToken);

		String tokenExpected = System.getenv(ENV_TOKEN);
		if(!tokenExpected.equals(bearerToken)) {
			if(log.isLoggable(Level.FINE))
				log.log(Level.FINE, "Invalid token!");
			requestContext.abortWith(Response.status(Status.FORBIDDEN).build());
			return;
		}
	}
}

