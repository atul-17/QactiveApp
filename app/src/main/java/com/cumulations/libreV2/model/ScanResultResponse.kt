package com.cumulations.libreV2.model

import com.google.gson.annotations.SerializedName

data class ScanResultResponse(@SerializedName("Items")
                              val items: List<ScanResultItem>?)