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

import static java.util.stream.Collectors.toList;

import org.ovirt.engine.api.model.Bookmark;
import org.ovirt.engine.api.model.Bookmarks;
import org.ovirt.engine.api.v3.V3Adapter;
import org.ovirt.engine.api.v3.types.V3Bookmark;
import org.ovirt.engine.api.v3.types.V3Bookmarks;

public class V3BookmarkOutAdapter implements V3Adapter<Bookmark, V3Bookmark> {
    @Override
    public V3Bookmark adapt(Bookmark from) {
        V3Bookmark to = new V3Bookmark();
        if (from.isSetComment()) {
            to.setComment(from.getComment());
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
        if (from.isSetValue()) {
            to.setValue(from.getValue());
        }
        return to;
    }
    public V3Bookmarks adapt(Bookmarks from) {
        V3Bookmarks to = new V3Bookmarks();
        to.getBookmarks().addAll(from.getBookmarks().stream().map(this::adapt).collect(toList()));
        return to;
    }
}
