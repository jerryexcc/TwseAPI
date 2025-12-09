package idv.jerry.twseapi.data.network

import idv.jerry.twseapi.data.model.StockInfo
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Headers

interface ApiService {
    @Headers(
        "accept: application/json",
        "If-Modified-Since: Mon, 26 Jul 1997 05:00:00 GMT",
        "Cache-Control: no-cache",
        "Pragma: no-cache"
    )
    @GET("v1/exchangeReport/BWIBBU_ALL")
    suspend fun getStockInfo(): Response<ResponseBody>

    @Headers(
        "accept: application/json",
        "If-Modified-Since: Mon, 26 Jul 1997 05:00:00 GMT",
        "Cache-Control: no-cache",
        "Pragma: no-cache"
    )
    @GET("v1/exchangeReport/STOCK_DAY_AVG_ALL")
    suspend fun getStockDayAvg(): Response<ResponseBody>

    @Headers(
        "accept: application/json",
        "If-Modified-Since: Mon, 26 Jul 1997 05:00:00 GMT",
        "Cache-Control: no-cache",
        "Pragma: no-cache"
    )
    @GET("v1/exchangeReport/STOCK_DAY_ALL")
    suspend fun getStockDayAll(): Response<ResponseBody>
}
