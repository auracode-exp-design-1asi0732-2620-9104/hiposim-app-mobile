package com.auracode.hiposim.testing

import com.auracode.hiposim.feature.leads.data.AgenciesService
import com.auracode.hiposim.feature.leads.data.AgencyDto
import com.auracode.hiposim.feature.leads.data.CreateLeadRequestDto
import com.auracode.hiposim.feature.leads.data.FakeAgenciesService
import com.auracode.hiposim.feature.leads.data.FakeLeadsService
import com.auracode.hiposim.feature.leads.data.LeadResponseDto
import com.auracode.hiposim.feature.leads.data.LeadsService

/** A leads service whose answer each test decides. It behaves like the fake by default. */
class StubLeadsService : LeadsService {
    var onCreate: suspend (CreateLeadRequestDto) -> LeadResponseDto = { FakeLeadsService().createLead(it) }
    val requests = mutableListOf<CreateLeadRequestDto>()

    override suspend fun createLead(request: CreateLeadRequestDto): LeadResponseDto {
        requests += request
        return onCreate(request)
    }
}

/** An agencies service whose answer each test decides. It answers with the demo agency by default. */
class StubAgenciesService : AgenciesService {
    var onGet: suspend () -> List<AgencyDto> = { FakeAgenciesService().getAgencies() }
    var calls = 0

    override suspend fun getAgencies(): List<AgencyDto> {
        calls++
        return onGet()
    }
}
