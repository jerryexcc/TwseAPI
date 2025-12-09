package idv.jerry.twseapi.data.repository

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import idv.jerry.twseapi.data.model.StockDayAll
import idv.jerry.twseapi.data.model.StockDayAvg
import idv.jerry.twseapi.data.model.StockInfo
import idv.jerry.twseapi.data.network.RetrofitInstance
import idv.jerry.twseapi.domain.model.Stock
import idv.jerry.twseapi.domain.repository.IStockRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import java.io.File
import java.io.FileOutputStream
import java.io.FileReader
import java.io.InputStream

class StockRepositoryImpl(private val cacheDir: File) : IStockRepository {

    override suspend fun fetchAndSaveStockData(): Result<File> {
        return try {
            coroutineScope {
                val deferredStockInfo = async { fetchAndSaveStockInfo() }
                val deferredStockDayAvg = async { fetchAndSaveStockDayAvg() }
                val deferredStockDayAll = async { fetchAndSaveStockDayAll() }
                
                // Wait for all downloads
                val results = awaitAll(deferredStockInfo, deferredStockDayAvg, deferredStockDayAll)
                
                // Return success if at least one succeeded, or handle failure appropriately.
                results.firstOrNull { it.isSuccess } ?: Result.failure(Exception("All downloads failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun fetchAndSaveStockInfo(): Result<File> {
        val response = RetrofitInstance.api.getStockInfo()
        return if (response.isSuccessful) {
            val body = response.body()
            if (body != null) {
                val file = File(cacheDir, "BWIBBU_ALL.json")
                saveFile(body.byteStream(), file)
                Result.success(file)
            } else {
                Result.failure(Exception("Response body is null"))
            }
        } else {
            Result.failure(Exception("Error: ${response.code()} ${response.message()}"))
        }
    }

    private suspend fun fetchAndSaveStockDayAvg(): Result<File> {
        val response = RetrofitInstance.api.getStockDayAvg()
        return if (response.isSuccessful) {
            val body = response.body()
            if (body != null) {
                val file = File(cacheDir, "STOCK_DAY_AVG_ALL.json")
                saveFile(body.byteStream(), file)
                Result.success(file)
            } else {
                Result.failure(Exception("Response body is null"))
            }
        } else {
            Result.failure(Exception("Error: ${response.code()} ${response.message()}"))
        }
    }

    private suspend fun fetchAndSaveStockDayAll(): Result<File> {
        val response = RetrofitInstance.api.getStockDayAll()
        return if (response.isSuccessful) {
            val body = response.body()
            if (body != null) {
                val file = File(cacheDir, "STOCK_DAY_ALL.json")
                saveFile(body.byteStream(), file)
                Result.success(file)
            } else {
                Result.failure(Exception("Response body is null"))
            }
        } else {
            Result.failure(Exception("Error: ${response.code()} ${response.message()}"))
        }
    }

    private fun saveFile(inputStream: InputStream, file: File) {
        try {
            val outputStream = FileOutputStream(file)
            val buffer = ByteArray(4096)
            var bytesRead: Int
            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                outputStream.write(buffer, 0, bytesRead)
            }
            outputStream.flush()
            outputStream.close()
            inputStream.close()
        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        }
    }

    override fun loadStockData(): List<Stock> {
        val fileInfo = File(cacheDir, "BWIBBU_ALL.json")
        val fileDayAvg = File(cacheDir, "STOCK_DAY_AVG_ALL.json")
        val fileDayAll = File(cacheDir, "STOCK_DAY_ALL.json")
        
        val stockInfos = loadJson<List<StockInfo>>(fileInfo)
        val stockDayAvgs = loadJson<List<StockDayAvg>>(fileDayAvg)
        val stockDayAlls = loadJson<List<StockDayAll>>(fileDayAll)

        // Merge data based on stock Code
        val dayAvgMap = stockDayAvgs.associateBy { it.code }
        val dayAllMap = stockDayAlls.associateBy { it.code }

        return stockInfos.map { info ->
            val avg = dayAvgMap[info.code]
            val all = dayAllMap[info.code]
            Stock(
                code = info.code,
                name = info.name,
                peRatio = info.peRatio,
                dividendYield = info.dividendYield,
                pbRatio = info.pbRatio,
                closingPrice = avg?.closingPrice ?: all?.closingPrice, // fallback to all if avg is missing or logic differs
                monthlyAveragePrice = avg?.monthlyAveragePrice,
                tradeVolume = all?.tradeVolume,
                tradeValue = all?.tradeValue,
                openingPrice = all?.openingPrice,
                highestPrice = all?.highestPrice,
                lowestPrice = all?.lowestPrice,
                change = all?.change,
                transaction = all?.transaction
            )
        }
    }
    
    private inline fun <reified T> loadJson(file: File): T {
        return if (file.exists()) {
            try {
                val type = object : TypeToken<T>() {}.type
                Gson().fromJson<T>(FileReader(file), type)
            } catch (e: Exception) {
                if (T::class == List::class) emptyList<Any>() as T else throw e
            }
        } else {
            if (T::class == List::class) emptyList<Any>() as T else throw RuntimeException("File not found")
        }
    }
}
