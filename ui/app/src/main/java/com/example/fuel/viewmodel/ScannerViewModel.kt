package com.example.fuel.viewmodel

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.Image
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fuel.repository.FuelPriceRepository
import kotlinx.coroutines.launch
import retrofit2.Response
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class ScannerViewModel(
    private val fuelPriceRepository: FuelPriceRepository): ViewModel() {

    var uploadFuelPrices: MutableLiveData<Response<Void>> = MutableLiveData()

    private var _image: Image? = null
    private val image get() = _image!!

    fun uploadFuelPrices(activity: FragmentActivity) {
        val file = saveImage(activity)
        image.close()

        viewModelScope.launch {
            uploadFuelPrices.value = fuelPriceRepository.extractPrices(file)
        }
    }

    fun setImage(image: Image) {
        _image = image
    }

    private fun saveImage(activity: FragmentActivity): File {
        val file = createFile(activity)

        val buffer = image.planes[0].buffer
        val bytes = ByteArray(buffer.capacity())
        buffer.get(bytes)
        val bitmapImage = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, null)

        FileOutputStream(file).use {
            bitmapImage.compress(Bitmap.CompressFormat.JPEG, 50, it)
        }

        return file
    }

    private fun createFile(activity: FragmentActivity): File {
        val sdf = SimpleDateFormat("yyyy_MM_dd_HH_mm_ss_SSS", Locale.US)
        return File(activity.filesDir, "PHOTO_${sdf.format(Date())}.jpeg")
    }
}