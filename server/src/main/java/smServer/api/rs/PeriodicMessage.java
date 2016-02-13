package smServer.api.rs;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.text.DateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Resource;
import javax.enterprise.context.RequestScoped;
import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.servlet.ServletContext;
import javax.sql.DataSource;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import smServer.AbstractPeriodicMessageWatcher;
import smServer.AppContext;
import smServer.listener.AppListener;

@Path("periodic-message")
@RequestScoped
public class PeriodicMessage {

	@Resource(lookup = "jdbc/DefaultDS")
	private DataSource dataSource;
	private Logger log;

	public PeriodicMessage() {
		log = Logger.getLogger(PeriodicMessage.class.getName());
	}

	@PUT
	@Consumes(MediaType.APPLICATION_JSON)
	public Response create(@NotNull JsonObject newMessage, @Context ServletContext sc) throws SQLException {

		log.log(Level.INFO, "ds={0}", dataSource);
		Connection c = dataSource.getConnection();
		PreparedStatement ps = c.prepareStatement("insert into sms_message_periodic (message_id, receiver_no, message_text, period, send_at)" +
				" values (?, ?, ?, ?, ?)");

		ps.setInt(1, newMessage.getInt("id"));
		ps.setString(2, newMessage.getString("receiverNo"));
		ps.setString(3, newMessage.getString("text"));
		ps.setString(4, newMessage.getString("period"));
		ps.setString(5, newMessage.getString("at"));
		ps.execute();

		ps.close();
		c.close();

		AppContext ctx = (AppContext) sc.getAttribute(AppListener.ATTRIBUTE_CONTEXT);
		AbstractPeriodicMessageWatcher pwm = (AbstractPeriodicMessageWatcher) ctx.get(AppContext.PMW);
		pwm.refresh();

		return Response.ok().build();
	}

	@DELETE
	public Response delete(@QueryParam("id") int id, @Context ServletContext sc) throws SQLException {

		Connection c = dataSource.getConnection();
		PreparedStatement ps = c.prepareStatement("delete from sms_message_periodic where message_id = ?");
		ps.setInt(1, id);
		ps.execute();

		ps.close();
		c.close();

		AppContext ctx = (AppContext) sc.getAttribute(AppListener.ATTRIBUTE_CONTEXT);
		AbstractPeriodicMessageWatcher pwm = (AbstractPeriodicMessageWatcher) ctx.get(AppContext.PMW);
		pwm.refresh();

		return Response.ok().build();
	}

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public Response getAll(@Context ServletContext sc) {
		AppContext ctx = (AppContext) sc.getAttribute(AppListener.ATTRIBUTE_CONTEXT);

		AbstractPeriodicMessageWatcher pwm = (AbstractPeriodicMessageWatcher) ctx.get(AppContext.PMW);
		List<Calendar> dates = pwm.getTimerTasks();

		JsonArrayBuilder ab = Json.createArrayBuilder();
		DateFormat df = DateFormat.getDateInstance();
		for(Calendar c: dates) {
			ab.add(Json.createObjectBuilder().add("date",df.format(c.getTime())));
		}
		return Response.ok(ab.build()).build();
	}
}
