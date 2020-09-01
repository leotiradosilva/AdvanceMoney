package com.advanceClaim.client

import com.advanceClaim.state.ClaimState
import net.corda.client.rpc.CordaRPCClient
import net.corda.core.contracts.StateAndRef
import net.corda.core.utilities.NetworkHostAndPort
import net.corda.core.utilities.loggerFor
import org.slf4j.Logger

/**
 *  Demonstration of using the CordaRPCClient to connect to a Corda Node and
 *  steam some State data from the node.
 **/

fun main(args: Array<String>) {
    AdvanceMoneyClientRPC().main(args)
}

private class AdvanceMoneyClientRPC {
    companion object {
        val logger: Logger = loggerFor<AdvanceMoneyClientRPC>()
        private fun logState(state: StateAndRef<ClaimState>) = logger.info("{}", state.state.data)
    }

    fun main(args: Array<String>) {
        require(args.size == 1) { "Usage: InsuranceClientRPC <node address>" }
        val nodeAddress = NetworkHostAndPort.parse(args[0])
        val client = CordaRPCClient(nodeAddress)

        // Can be amended in the com.insuranceClaim.MainKt file.
        val proxy = client.start("user1", "test").proxy

        // Grab all existing and future Claim states in the vault.
        val (snapshot, updates) = proxy.vaultTrack(ClaimState::class.java)

        // Log the 'placed' Claim states and listen for new ones.
        snapshot.states.forEach { logState(it) }
        updates.toBlocking().subscribe { update ->
            update.produced.forEach { logState(it) }
        }
    }
}
