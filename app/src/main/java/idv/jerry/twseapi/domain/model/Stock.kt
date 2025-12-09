package idv.jerry.twseapi.domain.model

data class Stock(
    val code: String,
    val name: String,
    val peRatio: String?,
    val dividendYield: String?,
    val pbRatio: String?,
    val closingPrice: String? = null,
    val monthlyAveragePrice: String? = null,
    val tradeVolume: String? = null,
    val tradeValue: String? = null,
    val openingPrice: String? = null,
    val highestPrice: String? = null,
    val lowestPrice: String? = null,
    val change: String? = null,
    val transaction: String? = null
)
