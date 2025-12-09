package idv.jerry.twseapi.data.repository

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import idv.jerry.twseapi.data.model.StockInfo
import idv.jerry.twseapi.data.network.RetrofitInstance
import java.io.File
import java.io.FileOutputStream
import java.io.FileReader
import java.io.InputStream

class StockRepository(private val cacheDir: File) {

    suspend fun fetchAndSaveStockData(): Result<File> {
        return try {
            val response = RetrofitInstance.api.getStockInfo()
            if (response.isSuccessful) {
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
        } catch (e: Exception) {
            Result.failure(e)
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

    fun loadStockData(): List<StockInfo> {
        val file = File(cacheDir, "BWIBBU_ALL.json")
        if (!file.exists()) return emptyList()

        return try {
            val type = object : TypeToken<List<StockInfo>>() {}.type
            Gson().fromJson(FileReader(file), type)
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
}
