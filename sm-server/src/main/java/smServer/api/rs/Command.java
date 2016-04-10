package smServer.api.rs;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.List;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.servlet.ServletContext;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import smServer.AbstractPeriodicEventWatcher;
import smServer.AppContext;
import smServer.Refreshable;
import smServer.listener.AppListener;

@Path("command")
public class Command {

	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response processCommand(@NotNull JsonObject command, @Context ServletContext sc) {
		String cmd = command.getString("command");
		if(cmd == null) {
			throw new IllegalArgumentException("command must be specified!");
		}

		AppContext ctx = (AppContext) sc.getAttribute(AppListener.ATTRIBUTE_CONTEXT);
		switch(cmd) {
		case "refreshPeriodic":
		{
			Refreshable r = (Refreshable) ctx.get(AppContext.PMW);
			r.refresh();
			return Response.ok().build();
		}
		case "sendMessages":
		{
			Refreshable r = (Refreshable) ctx.get(AppContext.NMW);
			r.refresh();
			return Response.ok().build();
		}
		case "listPeriodic":
		{
			AbstractPeriodicEventWatcher pwm = (AbstractPeriodicEventWatcher) ctx.get(AppContext.PMW);
			List<Calendar> dates = pwm.getTimerTasks();
			JsonArrayBuilder ab = Json.createArrayBuilder();
			DateFormat df = DateFormat.getDateInstance();
			for(Calendar c: dates) {
				ab.add(Json.createObjectBuilder().add("date",df.format(c.getTime())));
			}
			return Response.ok(ab.build()).build();
		}
		default:
			return Response.status(Status.NOT_FOUND).build();
		}
	}

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public JsonObject ready() {
		return Json.createObjectBuilder().add("status", "ready").build();
	}
}
