package org.ovirt.engine.api.restapi.resource;

import java.util.List;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.ovirt.engine.core.common.action.AddVmInterfaceParameters;
import org.ovirt.engine.core.common.action.RemoveVmInterfaceParameters;
import org.ovirt.engine.core.common.action.VdcActionParametersBase;
import org.ovirt.engine.core.common.action.VdcActionType;
import org.ovirt.engine.core.common.businessentities.Network;
import org.ovirt.engine.core.common.businessentities.VM;
import org.ovirt.engine.core.common.businessentities.VmNetworkInterface;
import org.ovirt.engine.core.common.queries.GetVmByVmIdParameters;
import org.ovirt.engine.core.common.queries.VdcQueryType;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.api.restapi.resource.AbstractBackendSubResource.ParametersProvider;

import org.ovirt.engine.api.common.util.DetailHelper;
import org.ovirt.engine.api.common.util.LinkHelper;
import org.ovirt.engine.api.model.Fault;
import org.ovirt.engine.api.model.NIC;
import org.ovirt.engine.api.model.Statistic;
import org.ovirt.engine.api.model.Statistics;
import org.ovirt.engine.api.resource.VmNicResource;
import org.ovirt.engine.api.resource.VmNicsResource;

public class BackendVmNicsResource
             extends BackendNicsResource
        implements VmNicsResource {

    public BackendVmNicsResource(Guid parentId) {
        super(parentId,
              VdcQueryType.GetVmInterfacesByVmId,
              new GetVmByVmIdParameters(parentId),
              VdcActionType.AddVmInterface,
              VdcActionType.RemoveVmInterface,
              VdcActionType.UpdateVmInterface);
    }

    @Override
    protected ParametersProvider<NIC, VmNetworkInterface> getUpdateParametersProvider() {
        return new UpdateParametersProvider();
    }

    protected class UpdateParametersProvider implements ParametersProvider<NIC, VmNetworkInterface> {
        @Override
        public VdcActionParametersBase getParameters(NIC incoming, VmNetworkInterface entity) {
            return new AddVmInterfaceParameters(parentId, map(incoming, entity));
        }
    }

    @Override
    public Response add(NIC device) {
        //TODO: this is temporary mapping between engine boolean port mirroring parameter, and REST
        //      port mirroring network collection, next engine version will support the network collection
        //      in port mirroring

        validateEnums(NIC.class, device);
        boolean fault = false;
        String faultString = "The port mirroring network must match the Network set on the NIC";
        boolean isPortMirroring = device.isSetPortMirroring() && device.getPortMirroring().isSetNetworks();
        boolean isPortMirroringExceeded =
                isPortMirroring && device.getPortMirroring().getNetworks().getNetworks().size() > 1;
        if (!fault && isPortMirroringExceeded) {
            fault = true;
            faultString = "Cannot set more than one network in port mirroring mode";
        }
        isPortMirroring = isPortMirroring && device.getPortMirroring().getNetworks().getNetworks().size() == 1;
        if (!fault && isPortMirroring) {
            org.ovirt.engine.api.model.Network pmNetwork = device.getPortMirroring().getNetworks().getNetworks().get(0);
            String pmNetworkId = (pmNetwork.isSetId() ? pmNetwork.getId() : null);
            String pmNetworkName = (pmNetwork.isSetName() ? pmNetwork.getName() : null);
            String networkId =
                    (device.isSetNetwork() && device.getNetwork().isSetId()) ? device.getNetwork().getId() : null;
            String networkName =
                    (device.isSetNetwork() && device.getNetwork().isSetName()) ? device.getNetwork().getName() : null;
            if (pmNetworkId != null) {
                networkId = (networkId == null) ? getNetworkId(networkName) : networkId;
                fault = (!pmNetworkId.equals(networkId));
            } else if (pmNetworkName != null) {
                if (networkName == null && networkId != null) {
                    pmNetworkId = getNetworkId(pmNetworkName);
                    fault = (!pmNetworkId.equals(networkId));
                }
                fault = fault || (!pmNetworkName.equals(networkName));
            } else {
                fault = true;
                faultString = "Network must have name or id property for port mirroring";
            }
        }
        if (fault) {
            Fault f = new Fault();
            f.setReason(faultString);
            Response response = Response.status(Response.Status.BAD_REQUEST).entity(f).build();
            throw new WebApplicationException(response);
        }
        return super.add(device);
    }

    private String getNetworkId(String networkName) {
        Guid clusterId = getClusterId();
        org.ovirt.engine.core.common.businessentities.Network n =
                getClusterNetwork(clusterId, null, networkName);
        if (n != null) {
            return n.getId().toString();
        }
        return null;
    }

    @Override
    protected NIC populate(NIC model, VmNetworkInterface entity) {
        return addStatistics(model, entity, uriInfo, httpHeaders);
    }

    NIC addStatistics(NIC model, VmNetworkInterface entity, UriInfo ui, HttpHeaders httpHeaders) {
        if (DetailHelper.include(httpHeaders, "statistics")) {
            model.setStatistics(new Statistics());
            NicStatisticalQuery query = new NicStatisticalQuery(newModel(model.getId()));
            List<Statistic> statistics = query.getStatistics(entity);
            for (Statistic statistic : statistics) {
                LinkHelper.addLinks(ui, statistic, query.getParentType());
            }
            model.getStatistics().getStatistics().addAll(statistics);
        }
        return model;
    }

    @Override
    protected VdcActionParametersBase getAddParameters(VmNetworkInterface entity, NIC nic) {
        return new AddVmInterfaceParameters(parentId, setNetwork(nic, entity));
    }

    @Override
    protected VdcActionParametersBase getRemoveParameters(String id) {
        return new RemoveVmInterfaceParameters(parentId, asGuid(id));
    }

    @Override
    @SingleEntityResource
    public VmNicResource getDeviceSubResource(String id) {
        return inject(new BackendVmNicResource(id,
                                             this,
                                             updateType,
                                             getUpdateParametersProvider(),
                                             getRequiredUpdateFields(),
                                             subCollections));
    }

    @Override
    protected VmNetworkInterface setNetwork(NIC device, VmNetworkInterface ni) {
        if (device.isSetNetwork()) {
            Guid clusterId = getClusterId();
            Network net = lookupClusterNetwork(clusterId, device.getNetwork().isSetId() ? asGuid(device.getNetwork().getId()) : null, device.getNetwork().getName());
            if (net != null) {
                ni.setNetworkName(net.getname());
            }
        }
        return ni;
    }

    @Override
    protected Guid getClusterId() {
        Guid clusterId = getEntity(VM.class,
            VdcQueryType.GetVmByVmId,
            new GetVmByVmIdParameters(parentId), "id").getvds_group_id();
        return clusterId;
    }
}
