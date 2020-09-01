package com.advanceClaim.state

import com.advanceClaim.schema.ClaimSchemaV1
import net.corda.core.contracts.ContractState
import net.corda.core.contracts.LinearState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party
import net.corda.core.schemas.MappedSchema
import net.corda.core.schemas.PersistentState
import net.corda.core.schemas.QueryableState

/**
 * The state object recording Claim applications between two parties.
 *
 * A state must implement [ContractState] or one of its descendants.
 *
 * @param value the amount of the Claim.
 * @param insurerNode the party issuing the Claim (Insurance Company).
 * @param applicantNode the party applying for the Claim.
 */

data class ClaimState(
        val applicantNode: Party,
        val advanceMoneyNode: Party,
        val fname: String,
        val lname: String,
        val address: String,
        var advanceMoneyID: String,
        val type: String,
        val value: Int,
        val reason: String,
        val approvedAmount:Int,
        var advanceMoneyStatus: String,
        var referenceID: String,
        override val linearId: UniqueIdentifier = UniqueIdentifier()):
        LinearState, QueryableState {
    /** The public keys of the involved parties. */
    override val participants: List<AbstractParty> get() = listOf(advanceMoneyNode,applicantNode)

    override fun generateMappedObject(schema: MappedSchema): PersistentState {
        return when (schema) {
            is ClaimSchemaV1 -> ClaimSchemaV1.PersistentClaim(
                    this.applicantNode.name.toString(),
                    this.advanceMoneyNode.name.toString(),
                    this.fname,
                    this.lname,
                    this.address,
                    this.advanceMoneyID,
                    this.type,
                    this.value,
                    this.reason,
                    this.approvedAmount,
                    this.advanceMoneyStatus,
                    this.referenceID,
                    this.linearId.id
            )
            else -> throw IllegalArgumentException("Unrecognised schema $schema")
        }
    }

    override fun supportedSchemas(): Iterable<MappedSchema> = listOf(ClaimSchemaV1)
}
