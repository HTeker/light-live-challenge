package org.light.challenge.rest

import org.light.challenge.model.ApprovalDecisionRequest
import org.light.challenge.service.ApprovalService
import javax.ws.rs.*
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

@Path("/approvals")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
class ApprovalResource(private val approvalService: ApprovalService) {

    @GET
    @Path("/pending")
    fun getPendingApprovals(): Response {
        val pending = approvalService.getPendingApprovalDetails()
        return Response.ok(pending).build()
    }

    @POST
    @Path("/{id}/decide")
    fun decide(
        @PathParam("id") approvalRequestId: String,
        decision: ApprovalDecisionRequest
    ): Response {
        return try {
            val result = approvalService.processDecision(approvalRequestId, decision)
            Response.ok(result).build()
        } catch (e: IllegalArgumentException) {
            Response.status(404).entity(mapOf("error" to e.message)).build()
        } catch (e: IllegalStateException) {
            Response.status(400).entity(mapOf("error" to e.message)).build()
        }
    }
}
