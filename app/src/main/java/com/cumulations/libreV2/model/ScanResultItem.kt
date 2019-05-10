package com.cumulations.libreV2.model

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class ScanResultItem(@SerializedName("Security")
                          var security: String = "",
                          @SerializedName("SSID")
                          var ssid: String = ""):Serializable