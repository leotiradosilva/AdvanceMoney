package com.advanceClaim.schema

import net.corda.core.schemas.MappedSchema
import net.corda.core.schemas.PersistentState
import java.util.*
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Table

/**
 * The family of schemas for advanceMoneyState.
 */
object ClaimSchema

/**
 * A advanceMoneyState schema.
 */
object ClaimSchemaV1 : MappedSchema(
        schemaFamily = ClaimSchema.javaClass,
        version = 1,
        mappedTypes = listOf(PersistentClaim::class.java)) {
    @Entity
    @Table(name = "advanceMoney_states")
    class PersistentClaim(
            @Column(name = "applicant")
            var applicant: String,

            @Column(name = "advanceMoney")
            var advanceMoney: String,

            @Column(name = "fname")
            var fname: String,

            @Column(name = "lname")
            var lname: String,

            @Column(name = "address")
            var address: String,

            @Column(name= "advanceMoneyID")
            var insuranceID: String,

            @Column(name= "type")
            var type: String,

            @Column(name = "value")
            var value: Int,

            @Column(name = "reason")
            var reason: String,

            @Column(name = "approvedAmount")
            var approvedAmount: Int,

            @Column(name = "advanceMoneyStatus")
            var insuranceStatus: String,

            @Column(name = "referenceID")
            var referenceID: String,

            @Column(name = "claimID")
            var claimID: UUID

    ) : PersistentState() {
        // Default constructor required by hibernate.
        constructor(): this("", "", "","","","","",0,"",0,"","", UUID.randomUUID())
    }
}