package com.example.fuel.api

import com.example.fuel.model.price.NewFuelPriceResponse
import com.example.fuel.model.price.NewPricesAtFuelStation
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface FuelPriceApiService {

    @POST("api/v1/fuel-prices")
    suspend fun createFuelPrices(
        @Body fuelPrices: NewPricesAtFuelStation,
        @Header("Authorization") authHeader: String
    ): Response<Array<NewFuelPriceResponse>>

    @Multipart
    @POST("api/v1/fuel-prices/extract")
    suspend fun extractFuelPrices(
        @Part file: MultipartBody.Part,
        @Header("Authorization") authHeader: String
    ): Response<Void>
}