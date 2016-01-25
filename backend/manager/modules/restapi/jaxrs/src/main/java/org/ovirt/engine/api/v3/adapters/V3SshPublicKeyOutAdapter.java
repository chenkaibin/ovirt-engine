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

import static org.ovirt.engine.api.v3.adapters.V3OutAdapters.adaptOut;

import org.ovirt.engine.api.model.SshPublicKey;
import org.ovirt.engine.api.v3.V3Adapter;
import org.ovirt.engine.api.v3.types.V3SSHPublicKey;

public class V3SshPublicKeyOutAdapter implements V3Adapter<SshPublicKey, V3SSHPublicKey> {
    @Override
    public V3SSHPublicKey adapt(SshPublicKey from) {
        V3SSHPublicKey to = new V3SSHPublicKey();
        if (from.isSetComment()) {
            to.setComment(from.getComment());
        }
        if (from.isSetContent()) {
            to.setContent(from.getContent());
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
        if (from.isSetName()) {
            to.setName(from.getName());
        }
        if (from.isSetUser()) {
            to.setUser(adaptOut(from.getUser()));
        }
        return to;
    }
}
