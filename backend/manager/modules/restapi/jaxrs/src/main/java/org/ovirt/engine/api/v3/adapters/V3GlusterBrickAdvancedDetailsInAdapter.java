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

import org.ovirt.engine.api.model.GlusterBrickAdvancedDetails;
import org.ovirt.engine.api.model.GlusterClients;
import org.ovirt.engine.api.model.GlusterMemoryPools;
import org.ovirt.engine.api.v3.V3Adapter;
import org.ovirt.engine.api.v3.types.V3GlusterBrickAdvancedDetails;

public class V3GlusterBrickAdvancedDetailsInAdapter implements V3Adapter<V3GlusterBrickAdvancedDetails, GlusterBrickAdvancedDetails> {
    @Override
    public GlusterBrickAdvancedDetails adapt(V3GlusterBrickAdvancedDetails from) {
        GlusterBrickAdvancedDetails to = new GlusterBrickAdvancedDetails();
        if (from.isSetComment()) {
            to.setComment(from.getComment());
        }
        if (from.isSetDescription()) {
            to.setDescription(from.getDescription());
        }
        if (from.isSetDevice()) {
            to.setDevice(from.getDevice());
        }
        if (from.isSetFsName()) {
            to.setFsName(from.getFsName());
        }
        if (from.isSetGlusterClients()) {
            to.setGlusterClients(new GlusterClients());
            to.getGlusterClients().getGlusterClients().addAll(adaptIn(from.getGlusterClients().getGlusterClients()));
        }
        if (from.isSetId()) {
            to.setId(from.getId());
        }
        if (from.isSetHref()) {
            to.setHref(from.getHref());
        }
        if (from.isSetMemoryPools()) {
            to.setMemoryPools(new GlusterMemoryPools());
            to.getMemoryPools().getGlusterMemoryPools().addAll(adaptIn(from.getMemoryPools().getGlusterMemoryPools()));
        }
        if (from.isSetMntOptions()) {
            to.setMntOptions(from.getMntOptions());
        }
        if (from.isSetName()) {
            to.setName(from.getName());
        }
        if (from.isSetPid()) {
            to.setPid(from.getPid());
        }
        if (from.isSetPort()) {
            to.setPort(from.getPort());
        }
        return to;
    }
}
