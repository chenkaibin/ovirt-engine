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

import javax.ws.rs.DELETE;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import org.ovirt.engine.api.resource.AffinityGroupVmResource;
import org.ovirt.engine.api.v3.V3Server;

@Produces({"application/xml", "application/json"})
public class V3AffinityGroupVmServer extends V3Server<AffinityGroupVmResource> {
    public V3AffinityGroupVmServer(AffinityGroupVmResource delegate) {
        super(delegate);
    }

    @DELETE
    public Response remove() {
        return adaptRemove(delegate::remove);
    }
}
