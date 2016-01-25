/*
Copyright (c) 2016 Red Hat, Inc.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package org.ovirt.engine.api.v3.servers;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import org.ovirt.engine.api.model.Actionable;
import org.ovirt.engine.api.resource.AttachedStorageDomainResource;
import org.ovirt.engine.api.v3.V3Server;
import org.ovirt.engine.api.v3.types.V3Action;
import org.ovirt.engine.api.v3.types.V3StorageDomain;

@Produces({"application/xml", "application/json"})
public class V3AttachedStorageDomainServer extends V3Server<AttachedStorageDomainResource> {
    public V3AttachedStorageDomainServer(AttachedStorageDomainResource delegate) {
        super(delegate);
    }

    @POST
    @Consumes({"application/xml", "application/json"})
    @Actionable
    @Path("activate")
    public Response activate(V3Action action) {
        return adaptAction(delegate::activate, action);
    }

    @POST
    @Consumes({"application/xml", "application/json"})
    @Actionable
    @Path("deactivate")
    public Response deactivate(V3Action action) {
        return adaptAction(delegate::deactivate, action);
    }

    @GET
    public V3StorageDomain get() {
        return adaptGet(delegate::get);
    }

    @DELETE
    public Response remove() {
        return adaptRemove(delegate::remove);
    }

    @Path("disks")
    public V3DisksServer getDisksResource() {
        return new V3DisksServer(delegate.getDisksResource());
    }

    @Path("{action: (activate|deactivate)}/{oid}")
    public V3ActionServer getActionResource(@PathParam("action") String action, @PathParam("oid") String oid) {
        return new V3ActionServer(delegate.getActionResource(action, oid));
    }
}
