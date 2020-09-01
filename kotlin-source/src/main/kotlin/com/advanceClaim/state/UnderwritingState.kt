package com.advanceClaim.state

import com.advanceClaim.schema.UnderwritingSchemaVI
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
 * @param advanceMoneyNode the party issuing the Claim (Advance Money Company).
 * @param underwriterNode the Undertaking party for the Claim.
 */

data class UnderwritingState(
                val advanceMoneyNode: Party,
                val underwriterNode: Party,
                val fname: String,
                val lname: String,
                val insuranceID: String,
                val type: String,
                val value:Int,
                val reason: String,
                var approvedAmount: Int,
                var advanceMoneyStatus: String,
                override var linearId: UniqueIdentifier = UniqueIdentifier()):
        LinearState, QueryableState {
    /** The public keys of the involved parties. */
    override val participants: List<AbstractParty> get() = listOf(advanceMoneyNode, underwriterNode)

    override fun generateMappedObject(schema: MappedSchema): PersistentState {
        return when (schema) {
            is UnderwritingSchemaVI -> UnderwritingSchemaVI.PersistentUnderwriting(
                    this.advanceMoneyNode.name.toString(),
                    this.underwriterNode.name.toString(),
                    this.fname,
                    this.lname,
                    this.insuranceID,
                    this.type,
                    this.value,
                    this.reason,
                    this.approvedAmount,
                    this.advanceMoneyStatus,
                    this.linearId.id
            )
            else -> throw IllegalArgumentException("Unrecognised schema $schema")
        }
    }

    override fun supportedSchemas(): Iterable<MappedSchema> = listOf(UnderwritingSchemaVI)
}
