package org.ovirt.engine.core.bll.storage.repoimage;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.ovirt.engine.core.bll.Backend;
import org.ovirt.engine.core.bll.CommandBase;
import org.ovirt.engine.core.bll.SerialChildCommandsExecutionCallback;
import org.ovirt.engine.core.bll.SerialChildExecutingCommand;
import org.ovirt.engine.core.bll.VmTemplateHandler;
import org.ovirt.engine.core.bll.context.CommandContext;
import org.ovirt.engine.core.bll.job.ExecutionHandler;
import org.ovirt.engine.core.bll.provider.storage.OpenStackImageException;
import org.ovirt.engine.core.bll.provider.storage.OpenStackImageProviderProxy;
import org.ovirt.engine.core.bll.quota.QuotaConsumptionParameter;
import org.ovirt.engine.core.bll.quota.QuotaStorageConsumptionParameter;
import org.ovirt.engine.core.bll.quota.QuotaStorageDependent;
import org.ovirt.engine.core.bll.storage.disk.image.ImagesHandler;
import org.ovirt.engine.core.bll.tasks.interfaces.CommandCallback;
import org.ovirt.engine.core.bll.utils.PermissionSubject;
import org.ovirt.engine.core.bll.utils.VmDeviceUtils;
import org.ovirt.engine.core.bll.validator.storage.StorageDomainValidator;
import org.ovirt.engine.core.bll.validator.storage.StoragePoolValidator;
import org.ovirt.engine.core.common.AuditLogType;
import org.ovirt.engine.core.common.VdcObjectType;
import org.ovirt.engine.core.common.action.AddDiskParameters;
import org.ovirt.engine.core.common.action.AddVmTemplateParameters;
import org.ovirt.engine.core.common.action.DownloadImageCommandParameters;
import org.ovirt.engine.core.common.action.ImportRepoImageParameters;
import org.ovirt.engine.core.common.action.RemoveDiskParameters;
import org.ovirt.engine.core.common.action.VdcActionParametersBase.EndProcedure;
import org.ovirt.engine.core.common.action.VdcActionType;
import org.ovirt.engine.core.common.action.VdcReturnValueBase;
import org.ovirt.engine.core.common.asynctasks.EntityInfo;
import org.ovirt.engine.core.common.businessentities.ActionGroup;
import org.ovirt.engine.core.common.businessentities.ArchitectureType;
import org.ovirt.engine.core.common.businessentities.Cluster;
import org.ovirt.engine.core.common.businessentities.DisplayType;
import org.ovirt.engine.core.common.businessentities.HttpLocationInfo;
import org.ovirt.engine.core.common.businessentities.VmStatic;
import org.ovirt.engine.core.common.businessentities.VmTemplate;
import org.ovirt.engine.core.common.businessentities.storage.DiskImage;
import org.ovirt.engine.core.common.businessentities.storage.DiskInterface;
import org.ovirt.engine.core.common.businessentities.storage.DiskVmElement;
import org.ovirt.engine.core.common.businessentities.storage.ImageStatus;
import org.ovirt.engine.core.common.businessentities.storage.RepoImage;
import org.ovirt.engine.core.common.businessentities.storage.VolumeFormat;
import org.ovirt.engine.core.common.businessentities.storage.VolumeType;
import org.ovirt.engine.core.common.constants.StorageConstants;
import org.ovirt.engine.core.common.errors.EngineMessage;
import org.ovirt.engine.core.common.osinfo.OsRepository;
import org.ovirt.engine.core.common.utils.SimpleDependencyInjector;
import org.ovirt.engine.core.compat.Guid;

public class ImportRepoImageCommand<T extends ImportRepoImageParameters> extends CommandBase<T> implements SerialChildExecutingCommand, QuotaStorageDependent {

    @Inject
    private VmDeviceUtils vmDeviceUtils;

    private OpenStackImageProviderProxy providerProxy;

    public ImportRepoImageCommand(T parameters, CommandContext cmdContext) {
        super(parameters, cmdContext);
        getParameters().setCommandType(getActionType());
        setDefaultTemplateName();
        addAuditLogCustomValues();
    }

    private void setDefaultTemplateName() {
        if (getParameters().getImportAsTemplate() && getParameters().getTemplateName() == null) {
            // Following the same convention as the glance disk name,
            // using a GlanceTemplate prefix, followed by a short identifier.
            getParameters().setTemplateName("GlanceTemplate-" + Guid.newGuid().toString().substring(0, 7));
        }
    }

    @Override
    protected void executeCommand() {
        setupParameters();
        persistCommand(getParameters().getParentCommand(), true);
        Backend.getInstance().runInternalAction(VdcActionType.AddDisk,
                createAddDiskParameters(),
                ExecutionHandler.createDefaultContextForTasks(getContext()));
        getParameters().setNextPhase(ImportRepoImageParameters.Phase.DOWNLOAD);
        persistCommand(getParameters().getParentCommand(), true);
        setSucceeded(true);
    }

    private void setupParameters() {
        getParameters().setImageGroupID(Guid.newGuid());
        getParameters().setDestinationImageId(Guid.newGuid());
        getParameters().setEntityInfo(
                new EntityInfo(VdcObjectType.Disk, getParameters().getImageGroupID()));
    }

    @Override
    public CommandCallback getCallback() {
        return new SerialChildCommandsExecutionCallback();
    }

    protected AddDiskParameters createAddDiskParameters() {
        DiskImage diskImage = getParameters().getDiskImage();
        ArrayList<Guid> storageIds = new ArrayList<>();
        storageIds.add(getParameters().getStorageDomainId());
        diskImage.setDiskAlias(getParameters().getDiskAlias());
        diskImage.setStorageIds(storageIds);
        diskImage.setStoragePoolId(getParameters().getStoragePoolId());
        diskImage.setId(getParameters().getImageGroupID());
        diskImage.setDiskProfileId(getParameters().getDiskProfileId());
        diskImage.setImageId(getParameters().getDestinationImageId());
        diskImage.setQuotaId(getParameters().getQuotaId());
        AddDiskParameters parameters = new AddDiskParameters(diskImage);
        parameters.setStorageDomainId(getParameters().getStorageDomainId());
        parameters.setParentCommand(getActionType());
        parameters.setParentParameters(getParameters());
        parameters.setShouldRemainIllegalOnFailedExecution(true);
        parameters.setShouldRemainLockedOnSuccesfulExecution(true);
        parameters.setEndProcedure(EndProcedure.COMMAND_MANAGED);
        parameters.setUsePassedDiskId(true);
        parameters.setUsePassedImageId(true);
        return parameters;
    }

    private HttpLocationInfo prepareRepoImageLocation() {
        return new HttpLocationInfo(
                getProviderProxy().getImageUrl(getParameters().getSourceRepoImageId()),
                getProviderProxy().getDownloadHeaders());
    }

    protected DownloadImageCommandParameters createDownloadImageParameters() {
        DownloadImageCommandParameters parameters = new DownloadImageCommandParameters();
        parameters.setDestinationImageId(getParameters().getDestinationImageId());
        parameters.setStoragePoolId(getParameters().getStoragePoolId());
        parameters.setStorageDomainId(getParameters().getStorageDomainId());
        parameters.setImageGroupID(getParameters().getImageGroupID());
        parameters.setHttpLocationInfo(prepareRepoImageLocation());
        parameters.setParentCommand(getActionType());
        parameters.setParentParameters(getParameters());
        return parameters;
    }

    @Override
    public boolean performNextOperation(int completedChildCount) {
        if (getParameters().getNextPhase() == ImportRepoImageParameters.Phase.DOWNLOAD) {
            getParameters().setNextPhase(ImportRepoImageParameters.Phase.END);
            persistCommand(getParameters().getParentCommand(), true);
            Backend.getInstance().runInternalAction(VdcActionType.DownloadImage,
                    createDownloadImageParameters(),
                    ExecutionHandler.createDefaultContextForTasks(getContext()));
            return true;
        }

        return false;
    }

    @Override
    public void handleFailure() {
        updateDiskStatus(ImageStatus.ILLEGAL);
        removeDisk();
    }

    public void removeDisk() {
        Backend.getInstance().runInternalAction(VdcActionType.RemoveDisk,
                new RemoveDiskParameters(getParameters().getImageGroupID()));
    }

    protected OpenStackImageProviderProxy getProviderProxy() {
        if (providerProxy == null) {
            providerProxy = OpenStackImageProviderProxy
                    .getFromStorageDomainId(getParameters().getSourceStorageDomainId());
        }
        return providerProxy;
    }

    @Override
    public void endSuccessfully() {
        super.endSuccessfully();
        if (getParameters().getImportAsTemplate()) {
            Guid newTemplateId = createTemplate();
            // No reason for this to happen, but checking just to make sure
            if (newTemplateId != null) {
                attachDiskToTemplate(newTemplateId);
            }
        }
        updateDiskStatus(ImageStatus.OK);
        setSucceeded(true);
    }

    @Override
    public void endWithFailure() {
        updateDiskStatus(ImageStatus.ILLEGAL);
        setSucceeded(true);
    }

    private void updateDiskStatus(ImageStatus status) {
        getParameters().getDiskImage().setImageStatus(status);
        ImagesHandler.updateImageStatus(getParameters().getDestinationImageId(), status);
    }

    private Guid createTemplate() {

        VmTemplate blankTemplate = vmTemplateDao.get(VmTemplateHandler.BLANK_VM_TEMPLATE_ID);
        VmStatic masterVm = new VmStatic(blankTemplate);
        OsRepository osRepository = SimpleDependencyInjector.getInstance().get(OsRepository.class);

        DiskImage templateDiskImage = getParameters().getDiskImage();
        String vmTemplateName = getParameters().getTemplateName();
        AddVmTemplateParameters parameters = new AddVmTemplateParameters(masterVm, vmTemplateName, templateDiskImage.getDiskDescription());

        // Setting the user from the parent command, as the session might already be invalid
        parameters.setParametersCurrentUser(getParameters().getParametersCurrentUser());

        // Setting the cluster ID, and other related properties derived from it
        if (getParameters().getClusterId() != null) {
            masterVm.setClusterId(getParameters().getClusterId());
            Cluster vdsGroup = getCluster(masterVm.getClusterId());
            masterVm.setOsId(osRepository.getDefaultOSes().get(vdsGroup.getArchitecture()));
            DisplayType defaultDisplayType =
                    osRepository.getGraphicsAndDisplays(masterVm.getOsId(), vdsGroup.getCompatibilityVersion()).get(0).getSecond();
            masterVm.setDefaultDisplayType(defaultDisplayType);
        }

        parameters.setBalloonEnabled(true);

        VdcReturnValueBase addVmTemplateReturnValue =
                Backend.getInstance().runInternalAction(VdcActionType.AddVmTemplate,
                        parameters,
                        ExecutionHandler.createDefaultContextForTasks(getContext()));

        // No reason for this to return null, but checking just to make sure, and returning the created template, or null if failed
        return addVmTemplateReturnValue.getActionReturnValue() != null ? (Guid) addVmTemplateReturnValue.getActionReturnValue() : null;
    }

    public Cluster getCluster(Guid clusterId) {
        return clusterDao.get(clusterId);
    }

    private void attachDiskToTemplate(Guid templateId) {
        DiskImage templateDiskImage = getParameters().getDiskImage();
        DiskVmElement dve = new DiskVmElement(templateDiskImage.getId(), templateId);
        dve.setDiskInterface(DiskInterface.VirtIO);
        diskVmElementDao.save(dve);
        vmDeviceUtils.addDiskDevice(templateId, templateDiskImage.getId());
    }


    @Override
    public List<PermissionSubject> getPermissionCheckSubjects() {
        List<PermissionSubject> permissionSubjects = new ArrayList<>();
        // NOTE: there's no read-permission from a storage domain
        permissionSubjects.add(new PermissionSubject(getParameters().getStorageDomainId(),
                VdcObjectType.Storage, ActionGroup.CREATE_DISK));
        permissionSubjects.add(new PermissionSubject(getParameters().getSourceStorageDomainId(),
                VdcObjectType.Storage, ActionGroup.ACCESS_IMAGE_STORAGE));
        return permissionSubjects;
    }

    @Override
    protected void setActionMessageParameters() {
        addValidationMessage(EngineMessage.VAR__ACTION__IMPORT);
        addValidationMessage(EngineMessage.VAR__TYPE__DISK);
    }

    @Override
    public Guid getStorageDomainId() {
        return getParameters().getStorageDomainId();
    }

    @Override
    public List<QuotaConsumptionParameter> getQuotaStorageConsumptionParameters() {
        List<QuotaConsumptionParameter> list = new ArrayList<>();
        list.add(new QuotaStorageConsumptionParameter(
                getParameters().getQuotaId(), null, QuotaConsumptionParameter.QuotaAction.CONSUME,
                getParameters().getStorageDomainId(), (double) getDiskImage().getSizeInGigabytes()));
        return list;
    }

    protected DiskImage getDiskImage() {
        if (getParameters().getDiskImage() == null) {
            DiskImage diskImage = getProviderProxy().getImageAsDiskImage(getParameters().getSourceRepoImageId());
            if (diskImage != null) {
                if (diskImage.getVolumeFormat() == VolumeFormat.RAW &&
                        getStorageDomain().getStorageType().isBlockDomain()) {
                    diskImage.setVolumeType(VolumeType.Preallocated);
                } else {
                    diskImage.setVolumeType(VolumeType.Sparse);
                }
                if (getParameters().getDiskAlias() == null) {
                    diskImage.setDiskAlias(RepoImage.getRepoImageAlias(
                            StorageConstants.GLANCE_DISK_ALIAS_PREFIX, getParameters().getSourceRepoImageId()));
                } else {
                    diskImage.setDiskAlias(getParameters().getDiskAlias());
                }
            }
            getParameters().setDiskImage(diskImage);
        }
        return getParameters().getDiskImage();
    }

    public String getRepoImageName() {
        return getDiskImage() != null ? getDiskImage().getDiskAlias() : "";
    }

    @Override
    public Map<String, String> getJobMessageProperties() {
        if (jobProperties == null) {
            jobProperties = super.getJobMessageProperties();
            jobProperties.put("repoimagename", getRepoImageName());
            jobProperties.put("storage", getStorageDomainName());
        }
        return jobProperties;
    }

    @Override
    public AuditLogType getAuditLogTypeValue() {
        switch (getActionState()) {
            case EXECUTE:
                if (!getParameters().getTaskGroupSuccess()) {
                    return getParameters().getImportAsTemplate() ?
                            AuditLogType.USER_IMPORT_IMAGE_AS_TEMPLATE_FINISHED_FAILURE :
                            AuditLogType.USER_IMPORT_IMAGE_FINISHED_FAILURE;
                }
                if (getSucceeded()) {
                    return getParameters().getImportAsTemplate() ? AuditLogType.USER_IMPORT_IMAGE_AS_TEMPLATE :
                            AuditLogType.USER_IMPORT_IMAGE;
                }
                break;
            case END_SUCCESS:
                return getParameters().getImportAsTemplate() ?
                        AuditLogType.USER_IMPORT_IMAGE_AS_TEMPLATE_FINISHED_SUCCESS :
                        AuditLogType.USER_IMPORT_IMAGE_FINISHED_SUCCESS;
            case END_FAILURE:
                return getParameters().getImportAsTemplate() ?
                        AuditLogType.USER_IMPORT_IMAGE_AS_TEMPLATE_FINISHED_FAILURE :
                        AuditLogType.USER_IMPORT_IMAGE_FINISHED_FAILURE;
        }
        return AuditLogType.UNASSIGNED;
    }

    @Override
    protected boolean validate() {
        if (!validate(new StoragePoolValidator(getStoragePool()).isUp())) {
            return false;
        }

        if (getParameters().getImportAsTemplate()) {
            if (getParameters().getClusterId() == null) {
                addValidationMessage(EngineMessage.VDS_CLUSTER_IS_NOT_VALID);
                return false;
            }

            setClusterId(getParameters().getClusterId());
            if (getCluster() == null) {
                addValidationMessage(EngineMessage.VDS_CLUSTER_IS_NOT_VALID);
                return false;
            }

            // A Template cannot be added in a cluster without a defined architecture
            if (getCluster().getArchitecture() == ArchitectureType.undefined) {
                return failValidation(EngineMessage.ACTION_TYPE_FAILED_CLUSTER_UNDEFINED_ARCHITECTURE);
            }

            setStoragePoolId(getParameters().getStoragePoolId());
        }

        DiskImage diskImage = null;

        try {
            diskImage = getDiskImage();
        } catch (OpenStackImageException e) {
            log.error("Unable to get the disk image from the provider proxy: ({}) {}",
                    e.getErrorType(),
                    e.getMessage());
            switch (e.getErrorType()) {
                case UNSUPPORTED_CONTAINER_FORMAT:
                case UNSUPPORTED_DISK_FORMAT:
                    return failValidation(EngineMessage.ACTION_TYPE_FAILED_IMAGE_NOT_SUPPORTED);
                case UNABLE_TO_DOWNLOAD_IMAGE:
                    return failValidation(EngineMessage.ACTION_TYPE_FAILED_IMAGE_DOWNLOAD_ERROR);
                case UNRECOGNIZED_IMAGE_FORMAT:
                    return failValidation(EngineMessage.ACTION_TYPE_FAILED_IMAGE_UNRECOGNIZED);
            }
        }

        if (diskImage == null) {
            return failValidation(EngineMessage.ACTION_TYPE_FAILED_DISK_NOT_EXIST);
        }

        return validateSpaceRequirements(diskImage);
    }

    protected boolean validateSpaceRequirements(DiskImage diskImage) {
        diskImage.getSnapshots().add(diskImage); // Added for validation purposes.
        StorageDomainValidator sdValidator = createStorageDomainValidator();
        boolean result = validate(sdValidator.isDomainExistAndActive())
                && validate(sdValidator.isDomainWithinThresholds())
                && validate(sdValidator.hasSpaceForClonedDisk(diskImage));
        diskImage.getSnapshots().remove(diskImage);
        return result;
    }

    protected StorageDomainValidator createStorageDomainValidator() {
        return new StorageDomainValidator(getStorageDomain());
    }

    private void addAuditLogCustomValues() {
        if (getParameters().getImportAsTemplate()) {
            addCustomValue("TemplateName", getParameters().getTemplateName());
        }
    }
}