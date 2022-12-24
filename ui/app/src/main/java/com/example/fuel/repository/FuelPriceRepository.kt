package com.example.fuel.repository

import com.example.fuel.api.RetrofitInstance
import com.example.fuel.mock.Auth
import com.example.fuel.model.price.NewFuelPriceResponse
import com.example.fuel.model.price.NewPricesAtFuelStation
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import retrofit2.Response
import java.io.File

class FuelPriceRepository {

    suspend fun createNewFuelPrices(fuelPrices: NewPricesAtFuelStation): Response<Array<NewFuelPriceResponse>> {
        return RetrofitInstance.fuelPriceApiService.createFuelPrices(fuelPrices, Auth.token);
    }

    suspend fun extractPrices(file: File): Response<Void> {
        val filePart = MultipartBody.Part.createFormData("file", file.name, file.asRequestBody("image/*".toMediaTypeOrNull()))
        return RetrofitInstance.fuelPriceApiService.extractFuelPrices(filePart, Auth.token)
    }
}