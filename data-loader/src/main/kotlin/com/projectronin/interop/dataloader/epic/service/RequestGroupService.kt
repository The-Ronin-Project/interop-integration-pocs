package com.projectronin.interop.dataloader.epic.service

import com.projectronin.interop.ehr.epic.client.EpicClient
import com.projectronin.interop.fhir.r4.resource.RequestGroup

class RequestGroupService(epicClient: EpicClient) : BaseEpicService<RequestGroup>(epicClient) {
    override val fhirURLSearchPart = "/api/FHIR/R4/RequestGroup"
    override val fhirResourceType = RequestGroup::class.java
}
