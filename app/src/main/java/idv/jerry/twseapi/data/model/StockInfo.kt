package idv.jerry.twseapi.data.model

import com.google.gson.annotations.SerializedName

data class StockInfo(
    @SerializedName("Date") val date: String,
    @SerializedName("Code") val code: String,
    @SerializedName("Name") val name: String,
    @SerializedName("PEratio") val peRatio: String?,
    @SerializedName("DividendYield") val dividendYield: String?,
    @SerializedName("PBratio") val pbRatio: String?
)
