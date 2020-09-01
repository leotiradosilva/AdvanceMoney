package com.advanceClaim.plugin

import com.advanceClaim.api.ClaimApi
import net.corda.core.messaging.CordaRPCOps
import net.corda.webserver.services.WebServerPluginRegistry
import java.util.function.Function

class ClaimPlugin : WebServerPluginRegistry {
    /**
     * A list of classes that expose web APIs.
     */
    override val webApis: List<Function<CordaRPCOps, out Any>> = listOf(Function(::ClaimApi))

    /**
     * A list of directories in the resources directory that will be served by Jetty under /web.
     */
    override val staticServeDirs = mapOf(
            // This will serve the advanceMoneyWeb directory in resources to /web/advanceMoneyClaim
            "insuranceClaim" to javaClass.classLoader.getResource("advanceMoneyClaimWeb").toExternalForm()
    )
}