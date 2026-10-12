package com.example.aqisensor.data.model

import com.google.gson.annotations.SerializedName

/**
 * Request body for POST /toggle/{sensor}.
 * Body: { "on": true|false }
 */
data class ToggleRequest(
    @SerializedName("on") val on: Boolean,
)
