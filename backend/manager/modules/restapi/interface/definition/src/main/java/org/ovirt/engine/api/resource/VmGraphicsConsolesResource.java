package org.ovirt.engine.api.resource;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import org.ovirt.engine.api.model.GraphicsConsoles;

@Produces({ApiMediaType.APPLICATION_XML, ApiMediaType.APPLICATION_JSON, ApiMediaType.APPLICATION_X_YAML})
public interface VmGraphicsConsolesResource {

    @GET
    public GraphicsConsoles list();

    @Path("{iden}")
    public VmGraphicsConsoleResource getVmGraphicsConsoleResource(@PathParam("iden") String id);

}
