package com.example.fuel.ui.fragment.scanner

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.ImageFormat
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureRequest
import android.media.ImageReader
import android.media.MediaRecorder
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.util.Size
import android.util.SparseIntArray
import android.view.LayoutInflater
import android.view.Surface
import android.view.TextureView
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.Navigation
import com.example.fuel.R
import com.example.fuel.databinding.FragmentScannerBinding
import com.example.fuel.viewmodel.ScannerViewModel
import com.example.fuel.viewmodel.ViewModelFactory
import java.util.*


class ScannerFragment : Fragment(R.layout.fragment_scanner) {
    private lateinit var binding: FragmentScannerBinding
    private lateinit var viewModel: ScannerViewModel

    private lateinit var textureView: TextureView
    private lateinit var cameraId: String
    private lateinit var backgroundHandlerThread: HandlerThread
    private lateinit var backgroundHandler: Handler
    private lateinit var cameraManager: CameraManager
    private lateinit var cameraDevice: CameraDevice
    private lateinit var captureRequestBuilder: CaptureRequest.Builder
    private lateinit var cameraCaptureSession: CameraCaptureSession
    private lateinit var imageReader: ImageReader
    private lateinit var previewSize: Size
    private lateinit var videoSize: Size
    private var orientations : SparseIntArray = SparseIntArray(4).apply {
        append(Surface.ROTATION_0, 90)
        append(Surface.ROTATION_90, 0)
        append(Surface.ROTATION_180, 270)
        append(Surface.ROTATION_270, 180)
    }

    private var shouldProceedWithOnResume: Boolean = true

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentScannerBinding.inflate(inflater, container, false)
        viewModel = ViewModelProvider(requireActivity(), ViewModelFactory())[ScannerViewModel::class.java]

        textureView = binding.textureView
        cameraManager = requireActivity().getSystemService(Context.CAMERA_SERVICE) as CameraManager

        binding.takePhotoBtn.apply { setOnClickListener { takePhoto() } }

        initUploadFuelPricesObserver()
        startBackgroundThread()

        return binding.root
    }

    private fun initUploadFuelPricesObserver() {
        viewModel.uploadFuelPrices.observe(viewLifecycleOwner) { response ->
        }
    }

    override fun onResume() {
        super.onResume()
        startBackgroundThread()

        if (textureView.isAvailable && shouldProceedWithOnResume) {
            setupCamera()
        } else if (!textureView.isAvailable) {
            textureView.surfaceTextureListener = surfaceTextureListener
        }

        shouldProceedWithOnResume = !shouldProceedWithOnResume
    }

    @SuppressLint("MissingPermission")
    private fun setupCamera() {
        cameraId = getRearFacingCameraId()

        val cameraCharacteristics = cameraManager.getCameraCharacteristics(cameraId)
        val streamConfigurationMap = cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)

        if (streamConfigurationMap != null) {
            previewSize = cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)!!.getOutputSizes(ImageFormat.JPEG).maxBy { it.height * it.width }
            videoSize = cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)!!.getOutputSizes(MediaRecorder::class.java).maxBy { it.height * it.width }
            imageReader = ImageReader.newInstance(previewSize.width, previewSize.height, ImageFormat.JPEG, 1)
            imageReader.setOnImageAvailableListener(onImageAvailableListener, backgroundHandler)
        }

        cameraManager.openCamera(cameraId, cameraStateCallback, backgroundHandler)
    }

    private fun createCameraPreviewSession() {
        val surfaceTexture = textureView.surfaceTexture
        surfaceTexture?.setDefaultBufferSize(previewSize.width, previewSize.height)
        val previewSurface = Surface(surfaceTexture)

        captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
        captureRequestBuilder.addTarget(previewSurface)

        cameraDevice.createCaptureSession(listOf(previewSurface, imageReader.surface), captureStateCallback, null)
    }

    private fun getRearFacingCameraId(): String {
        return cameraManager.cameraIdList.first { id ->
            val cameraCharacteristics = cameraManager.getCameraCharacteristics(id)
            cameraCharacteristics.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK
        }
    }

    private fun takePhoto() {
        captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE)
        captureRequestBuilder.addTarget(imageReader.surface)
        val rotation = requireActivity().windowManager.defaultDisplay.rotation
        captureRequestBuilder.set(CaptureRequest.JPEG_ORIENTATION, orientations.get(rotation))
        cameraCaptureSession.capture(captureRequestBuilder.build(), captureCallback, null)
    }

    private fun startBackgroundThread() {
        backgroundHandlerThread = HandlerThread("CameraVideoThread")
        backgroundHandlerThread.start()
        backgroundHandler = Handler(backgroundHandlerThread.looper)
    }

    private val onImageAvailableListener = ImageReader.OnImageAvailableListener { reader ->
        val image = reader.acquireLatestImage()
        viewModel.setImage(image)

        requireActivity().runOnUiThread {
            Navigation.findNavController(binding.root).navigate(R.id.priceUploadingFragment)
        }
    }

    private val surfaceTextureListener = object : TextureView.SurfaceTextureListener {
        override fun onSurfaceTextureAvailable(texture: SurfaceTexture, width: Int, height: Int) {
            setupCamera()
        }

        override fun onSurfaceTextureSizeChanged(texture: SurfaceTexture, width: Int, height: Int) { }

        override fun onSurfaceTextureDestroyed(texture: SurfaceTexture): Boolean {
            cameraCaptureSession.close()
            cameraDevice.close()

            return true
        }

        override fun onSurfaceTextureUpdated(texture: SurfaceTexture) { }
    }

    private val captureCallback = object : CameraCaptureSession.CaptureCallback() { }

    private val cameraStateCallback = object : CameraDevice.StateCallback() {
        override fun onOpened(camera: CameraDevice) {
            cameraDevice = camera
            createCameraPreviewSession()
        }

        override fun onDisconnected(camera: CameraDevice) {
            cameraDevice.close()
        }

        override fun onError(camera: CameraDevice, error: Int) {
            onDisconnected(cameraDevice)
            this@ScannerFragment.activity?.finish()
        }
    }

    private val captureStateCallback = object : CameraCaptureSession.StateCallback() {
        override fun onConfigured(session: CameraCaptureSession) {
            cameraCaptureSession = session
            cameraCaptureSession.setRepeatingRequest(captureRequestBuilder.build(), null, backgroundHandler)
        }

        override fun onConfigureFailed(p0: CameraCaptureSession) { }
    }

    override fun onDestroyView() {
        super.onDestroyView()

        cameraDevice.close()
        imageReader.close()
    }
}