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

package org.ovirt.engine.api.v3.adapters;

import static org.ovirt.engine.api.v3.adapters.V3InAdapters.adaptIn;

import org.ovirt.engine.api.model.Session;
import org.ovirt.engine.api.v3.V3Adapter;
import org.ovirt.engine.api.v3.types.V3Session;

public class V3SessionInAdapter implements V3Adapter<V3Session, Session> {
    @Override
    public Session adapt(V3Session from) {
        Session to = new Session();
        if (from.isSetComment()) {
            to.setComment(from.getComment());
        }
        if (from.isSetConsoleUser()) {
            to.setConsoleUser(from.isConsoleUser());
        }
        if (from.isSetDescription()) {
            to.setDescription(from.getDescription());
        }
        if (from.isSetId()) {
            to.setId(from.getId());
        }
        if (from.isSetHref()) {
            to.setHref(from.getHref());
        }
        if (from.isSetIp()) {
            to.setIp(adaptIn(from.getIp()));
        }
        if (from.isSetName()) {
            to.setName(from.getName());
        }
        if (from.isSetProtocol()) {
            to.setProtocol(from.getProtocol());
        }
        if (from.isSetUser()) {
            to.setUser(adaptIn(from.getUser()));
        }
        if (from.isSetVm()) {
            to.setVm(adaptIn(from.getVm()));
        }
        return to;
    }
}
