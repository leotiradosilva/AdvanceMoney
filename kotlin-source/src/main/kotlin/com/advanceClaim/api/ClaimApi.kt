package com.advanceClaim.api

import com.advanceClaim.flow.ApplyClaimFlow
import com.advanceClaim.flow.UnderwritingCreationFlow
import com.advanceClaim.flow.ClaimResponseFlow
import com.advanceClaim.flow.UnderwritingResponseFlow
import com.advanceClaim.state.ClaimState
import com.advanceClaim.state.UnderwritingState
import net.corda.core.identity.CordaX500Name
import net.corda.core.messaging.CordaRPCOps
import net.corda.core.messaging.vaultQueryBy
import net.corda.core.node.services.IdentityService
import net.corda.core.utilities.getOrThrow
import net.corda.core.utilities.loggerFor
import org.slf4j.Logger
import javax.ws.rs.*
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response
import javax.ws.rs.core.Response.Status.*
val SERVICE_NAMES = listOf("Notary", "Network Map Service")

// This API is accessible from /api/insurance. All paths specified below are relative to it.
@Path("claim")
class ClaimApi(private val rpcOps: CordaRPCOps) {
    private val myLegalName: CordaX500Name = rpcOps.nodeInfo().legalIdentities.first().name

    companion object {
        private val logger: Logger = loggerFor<ClaimApi>()
    }

    /**
     * Returns the node's name.
     */
    @GET
    @Path("me")
    @Produces(MediaType.APPLICATION_JSON)
    fun whoami() = mapOf("me" to myLegalName)

    /**
     * Returns all parties registered with the [NetworkMapService]. These names can be used to look up identities
     * using the [IdentityService].
     */

    @GET
    @Path("peers")
    @Produces(MediaType.APPLICATION_JSON)
    fun getPeers(): Map<String, List<CordaX500Name>> {
        val nodeInfo = rpcOps.networkMapSnapshot()
        return mapOf("peers" to nodeInfo
                .map { it.legalIdentities.first().name }
                //filter out myself, notary and eventual network map started by driver
                .filter { it.organisation !in (SERVICE_NAMES + myLegalName.organisation) })
    }
    /**
     * Displays all Advance Money ClaimApplication states that exist in the node's vault.
     */
    @GET
    @Path("ApplicationStates")
    @Produces(MediaType.APPLICATION_JSON)
    fun getApplicationStates() = rpcOps.vaultQueryBy<ClaimState>().states


    @GET
    @Path("UnderwritingStates")
    @Produces(MediaType.APPLICATION_JSON)
    fun getUnderwritingStates() = rpcOps.vaultQueryBy<UnderwritingState>().states

    /**
     * Initiates Advance Money ClaimApplication flow.
     */
    @PUT
    @Path("create-advancemoney")
    fun createInsurance(@QueryParam("value") value: Int,
                        @QueryParam("reason") reason: String,
                        @QueryParam("advanceMoneyID") advanceMoneyID: String,
                        @QueryParam("type") type: String,
                        @QueryParam ("fname") fname: String,
                        @QueryParam ("lname") lname: String,
                        @QueryParam ("address") address: String): Response
    {
        val advanceMoneyStatus: String = "RECEIVED"
        val companyName=CordaX500Name.parse("O=PartyB, L=New York, C=US")
        val advanceMoneyNode = rpcOps.wellKnownPartyFromX500Name(companyName) ?:
        return Response.status(BAD_REQUEST).entity("Company named $companyName cannot be found.\n").build()

        if (value <= 0) {
            return Response.status(BAD_REQUEST).entity(" parameter 'value' must be non-negative.\n").build()
        }

        if (advanceMoneyID == null) {
            return Response.status(BAD_REQUEST).entity(" Insurance ID is missing.\n").build()
        }

        if (fname == null) {
            return Response.status(BAD_REQUEST).entity("First name is missing. \n").build()
        }
        if (lname == null) {
            return Response.status(BAD_REQUEST).entity("Last name is missing. \n").build()
        }


        return try {
            val signedTx = rpcOps
                    .startFlowDynamic(ApplyClaimFlow.ClaimInitiator::class.java,advanceMoneyNode,value,reason,fname,lname,address,advanceMoneyID,type,advanceMoneyStatus).returnValue.getOrThrow()
            Response.status(CREATED).entity("Transaction id ${signedTx.id} committed to ledger.\n").build()

        } catch (ex: Throwable) {
            logger.error(ex.message, ex)
            Response.status(BAD_REQUEST).entity(ex.message!!).build()
        }
    }

        /**
         * Initiates Company forwarding ClaimApplication to Underwriting.
         */
        @PUT
        @Path("advancemoney-underwriting")
        @Consumes(MediaType.APPLICATION_JSON)
        fun advanceMoneyUnderwriting(@QueryParam ("fname") fname: String,
                                  @QueryParam ("lname") lname: String,
                                  @QueryParam("advanceMoneyID") advanceMoneyID: String,
                                  @QueryParam("type") type: String,
                                  @QueryParam("value") value:Int,
                                  @QueryParam("reason") reason: String,
                                  @QueryParam("advanceMoneyStatus") advanceMoneyStatus: String,
                                  @QueryParam("claimID") claimID: String): Response
        {
            val underwriter =CordaX500Name.parse("O=PartyC, L=Paris, C=FR")
            val underwriterNode = rpcOps.wellKnownPartyFromX500Name(underwriter) ?:
            return Response.status(BAD_REQUEST).entity("Underwriter named $underwriter cannot be found.\n").build()

            if(fname==null){
                return Response.status(BAD_REQUEST).entity("parameter 'First Name' missing or has wrong format.\n").build()
            }

            if(lname==null){
                return Response.status(BAD_REQUEST).entity("parameter 'Last Name' missing or has wrong format.\n").build()
            }

            if(advanceMoneyStatus==null){
                return Response.status(BAD_REQUEST).entity("parameter 'Insurance Status' missing or has wrong format.\n").build()
            }

            if(advanceMoneyID==null){
                return Response.status(BAD_REQUEST).entity("parameter 'Insurance id' missing or has wrong format.\n").build()
            }


            return try {
                val signedTx = rpcOps
                        .startFlowDynamic(UnderwritingCreationFlow.UnderwritingInitiator::class.java,underwriterNode,fname,lname,advanceMoneyID,type,value,reason,advanceMoneyStatus,claimID)
                        .returnValue.getOrThrow()

                Response.status(CREATED).entity("Transaction id ${signedTx.id} committed to ledger.\n").build()

            } catch (ex: Throwable) {
                logger.error(ex.message, ex)
                Response.status(BAD_REQUEST).entity(ex.message!!).build()
            }

        }

        /**
         * Initiates Underwriting Evaluation and response to the Insurance Company flow
         */
        @PUT
        @Path("underwriting-evaluation")
        @Consumes(MediaType.APPLICATION_JSON)
        fun underwritingEvaluation(@QueryParam("referenceID") referenceID: String,
                                    @QueryParam("advanceMoneyStatus") advanceMoneyStatus: String,
                                    @QueryParam("fname") fname: String,
                                    @QueryParam("lname") lname: String,
                                   @QueryParam("value") value: Int,
                                    @QueryParam("approvedAmount") approvedAmount: Int): Response {

            val companyName=CordaX500Name.parse("O=PartyB, L=New York, C=US")
            val advanceMoneyNode = rpcOps.wellKnownPartyFromX500Name(companyName) ?:
            return Response.status(BAD_REQUEST).entity("Insurance Company named $companyName cannot be found.\n").build()

            if (approvedAmount > value) {
                return Response.status(BAD_REQUEST).entity("Approved Amount cannot be greater than Claim Amount\n").build()
            }
            if (approvedAmount <=0) {
                return Response.status(BAD_REQUEST).entity("Approved Amount cannot be Negative or Zero\n").build()
            }

            if(fname==null){
                return Response.status(BAD_REQUEST).entity("parameter 'First Name' missing or has wrong format.\n").build()
            }

            if(lname==null){
                return Response.status(BAD_REQUEST).entity("parameter 'Last Name' missing or has wrong format.\n").build()
            }

            if(advanceMoneyStatus==null){
                return Response.status(BAD_REQUEST).entity("parameter 'Insurance Status' missing or has wrong format.\n").build()
            }

            if(referenceID==null){
                return Response.status(BAD_REQUEST).entity("parameter 'Underwriting linear id' missing or has wrong format.\n").build()
            }

            return try {
                val signedTx = rpcOps
                        .startTrackedFlowDynamic(UnderwritingResponseFlow.UnderwritingEvaluationInitiator::class.java, advanceMoneyNode,referenceID,advanceMoneyStatus,fname,lname,value,approvedAmount)
                        .returnValue.getOrThrow()
                        Response.status(CREATED).entity("Transaction id ${signedTx.id} committed to ledger.\n").build()

            } catch (ex: Throwable) {
                logger.error(ex.message, ex)
                return Response.status(BAD_REQUEST).entity(ex.message).build()
            }
        }
        /**
         * Initiates Insurance Company response to Applicant flow
         */

        @PUT
        @Path("company-response")
        fun CompanyResponse(@QueryParam("advanceMoneyStatus") advanceMoneyStatus: String,
                            @QueryParam("advanceMoneyID") advanceMoneyID: String,
                            @QueryParam("fname") fname: String,
                            @QueryParam("lname") lname: String,
                            @QueryParam("approvedAmount") approvedAmount: Int): Response {

            val applicant=CordaX500Name.parse("O=PartyA, L=London, C=GB")
            val applicantNode = rpcOps.wellKnownPartyFromX500Name(applicant) ?:
            return Response.status(BAD_REQUEST).entity("Company named $applicant cannot be found.\n").build()

            if (advanceMoneyID == null) {
                return Response.status(BAD_REQUEST).entity("Advance Money ID is missing . \n").build()
            }

            return try {
                val signedTx = rpcOps
                        .startTrackedFlowDynamic(ClaimResponseFlow.ClaimResponseInitiator::class.java,applicantNode,fname,lname,approvedAmount,advanceMoneyStatus,advanceMoneyID)
                        .returnValue.getOrThrow()
                        Response.status(CREATED).entity("Transaction id ${signedTx.id} committed to ledger.\n").build()

            } catch (ex: Throwable) {
                logger.error(ex.message, ex)
                return Response.status(BAD_REQUEST).entity(ex.message).build()
            }

        }
}
