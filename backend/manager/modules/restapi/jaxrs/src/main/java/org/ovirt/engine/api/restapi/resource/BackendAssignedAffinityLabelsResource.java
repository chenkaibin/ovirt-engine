package org.ovirt.engine.api.restapi.resource;

import java.util.List;

import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

import org.ovirt.engine.api.model.AffinityLabel;
import org.ovirt.engine.api.model.AffinityLabels;
import org.ovirt.engine.api.resource.AssignedAffinityLabelResource;
import org.ovirt.engine.api.resource.AssignedAffinityLabelsResource;
import org.ovirt.engine.api.restapi.utils.GuidUtils;
import org.ovirt.engine.core.common.action.ActionType;
import org.ovirt.engine.core.common.action.LabelActionParameters;
import org.ovirt.engine.core.common.businessentities.BusinessEntity;
import org.ovirt.engine.core.common.businessentities.Label;
import org.ovirt.engine.core.common.businessentities.LabelBuilder;
import org.ovirt.engine.core.common.queries.IdQueryParameters;
import org.ovirt.engine.core.common.queries.QueryType;
import org.ovirt.engine.core.compat.Guid;

public class BackendAssignedAffinityLabelsResource extends AbstractBackendCollectionResource<AffinityLabel, org.ovirt.engine.core.common.businessentities.Label>
        implements AssignedAffinityLabelsResource {

    public interface ReferencedEntityConstructor<T extends BusinessEntity<Guid>> {
        T create();
    }

    public BackendAssignedAffinityLabelsResource(String parentId, ReferencedEntityConstructor<BusinessEntity<Guid>> constructor) {
        super(AffinityLabel.class, org.ovirt.engine.core.common.businessentities.Label.class);
        this.parentId = parentId;
        this.constructor = constructor;
    }

    private final String parentId;
    private final ReferencedEntityConstructor<BusinessEntity<Guid>> constructor;

    @Override
    public Response add(AffinityLabel label) {
        validateParameters(label, "id");

        IdQueryParameters parameters = new IdQueryParameters(GuidUtils.asGuid(label.getId()));
        org.ovirt.engine.core.common.businessentities.Label entity = getEntity(
                org.ovirt.engine.core.common.businessentities.Label.class,
                QueryType.GetLabelById,
                parameters,
                label.getId(),
                true);

        BusinessEntity<Guid> parent = constructor.create();
        parent.setId(GuidUtils.asGuid(parentId));

        org.ovirt.engine.core.common.businessentities.Label updatedLabel = new LabelBuilder(entity)
                .entity(parent)
                .build();

        // Add the affinity label using the backend "update" operation. As the backend will return the added label as
        // the result of the operation, we can fetch it using a simple "identity" resolver, that just returns the same
        // value it is passed.
        LabelActionParameters updateParams = new LabelActionParameters(updatedLabel);
        return performCreate(ActionType.UpdateLabel, updateParams, (IResolver<Label, Label>) result -> result);
    }

    @Override
    public AffinityLabels list() {
        return mapCollection(
                getBackendCollection(
                        QueryType.GetLabelByEntityId, new IdQueryParameters(GuidUtils.asGuid(parentId))));
    }

    @Override
    public AssignedAffinityLabelResource getLabelResource(@PathParam("id") String id) {
        return new BackendAssignedAffinityLabelResource(parentId, constructor, id);
    }

    private AffinityLabels mapCollection(List<org.ovirt.engine.core.common.businessentities.Label> entities) {
        AffinityLabels collection = new AffinityLabels();
        for (org.ovirt.engine.core.common.businessentities.Label entity : entities) {
            collection.getAffinityLabels().add(addLinks(populate(map(entity), entity)));
        }
        return collection;
    }
}
