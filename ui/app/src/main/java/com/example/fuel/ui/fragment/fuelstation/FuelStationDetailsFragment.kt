package com.example.fuel.ui.fragment.fuelstation

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.AppCompatImageButton
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.core.widget.NestedScrollView
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.Navigation
import com.example.fuel.R
import com.example.fuel.databinding.FragmentFuelStationDetailsBinding
import com.example.fuel.mock.Auth
import com.example.fuel.model.FuelStationDetails
import com.example.fuel.model.FuelStationLocation
import com.example.fuel.model.FuelStationService
import com.example.fuel.model.FuelTypeWithPrice
import com.example.fuel.model.review.Review
import com.example.fuel.ui.common.initChipAppearance
import com.example.fuel.utils.allPermissionsGranted
import com.example.fuel.utils.calculateDistance
import com.example.fuel.utils.converters.UnitConverter
import com.example.fuel.utils.getUserLocation
import com.example.fuel.utils.isGpsEnabled
import com.example.fuel.viewmodel.FuelStationDetailsViewModel
import com.example.fuel.viewmodel.ViewModelFactory
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup


class FuelStationDetailsFragment : BottomSheetDialogFragment() {
    private lateinit var binding: FragmentFuelStationDetailsBinding
    private lateinit var viewModel: FuelStationDetailsViewModel
    private lateinit var fuelStationDetailsView: View
    private var fuelStationId: Long? = null

    private val ocrRequiredPermissions: Array<String> = arrayOf(
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.CAMERA)

    private val requestPermissionsResultLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()) { permissions -> handlePermissions(permissions) }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        viewModel = ViewModelProvider(requireActivity(), ViewModelFactory())[FuelStationDetailsViewModel::class.java]
        binding = FragmentFuelStationDetailsBinding.inflate(inflater, container, false)
        fuelStationDetailsView = inflater.inflate(R.layout.fragment_fuel_station_details, container, false)

        fuelStationId = requireArguments().getLong("fuelStationId")

        loadFuelStationData()

        return fuelStationDetailsView
    }

    private fun loadFuelStationData() {
        viewModel.getFuelStationDetails(fuelStationId!!)
        viewModel.fuelStationDetails.observe(viewLifecycleOwner) { response ->
            val fuelStationData = response.body()

            populateViewWithData(fuelStationData!!)
            showLayout()
            addFuelPriceCards(fuelStationData.fuelTypes)
            addFuelStationServices(fuelStationData.services)
            initReviewObserver()
            initUserReview()
            initReviewSection()
            initAddReviewButton()
            initNewReviewObserver()
            initEditedReviewObserver()
            initDeleteUserReviewObserver()
            initUserFavouriteObserver()
            initAddToFavouriteObserver()
            initRemoveFavouriteObserver()
            initOcrButton()
        }
    }

    private fun populateViewWithData(fuelStation: FuelStationDetails) {
        fuelStationDetailsView.findViewById<TextView>(R.id.tv_stationChainName).text = fuelStation.name ?: fuelStation.stationChain.name
        fuelStationDetailsView.findViewById<TextView>(R.id.tv_fuelStationAddress).text = fuelStation.address.toString()
        setDistance(fuelStation.location)
    }

    private fun setDistance(fuelStationLocation: FuelStationLocation) {
        val textView = fuelStationDetailsView.findViewById<TextView>(R.id.tv_distanceBetweenUserAndStation)

        if (!isGpsEnabled(requireContext())) {
            textView.visibility = View.GONE
            return
        }

        val location = getUserLocation(requireContext()) ?: return
        val distance = calculateDistance(location.latitude, location.longitude, fuelStationLocation.latitude, fuelStationLocation.longitude)

        textView.text =  resources.getString(R.string.from_you, UnitConverter.fromMetersToTarget(distance.toDouble()))
        textView.visibility = View.VISIBLE
    }

    private fun addFuelPriceCards(fuelTypes: Array<FuelTypeWithPrice>) {
        val fragmentManager = childFragmentManager
        val fragmentTransaction = fragmentManager.beginTransaction()

        val parent = fuelStationDetailsView.findViewById<LinearLayoutCompat>(R.id.llc_fuelPriceContainer)
        var llc: LinearLayoutCompat? = null

        for (i in fuelTypes.indices) {
            if (i % 2 == 0) {
                llc = createEmptyRow()
                parent.addView(llc)
            }

            val fuelPriceCardFragment = FuelPriceCardFragment(fuelTypes[i])
            fragmentTransaction.add(llc!!.id, fuelPriceCardFragment)
        }

        fragmentTransaction.commitNow()
    }

    private fun createEmptyRow(): LinearLayoutCompat {
        val llc = LinearLayoutCompat(requireContext())
        llc.id = View.generateViewId()
        llc.orientation = LinearLayoutCompat.HORIZONTAL
        return llc
    }

    private fun addFuelStationServices(services: Array<FuelStationService>) {
        val serviceContainer = fuelStationDetailsView.findViewById<ChipGroup>(R.id.cg_fuelStationServicesContainer);

        for (service in services) {
            val chip = createServiceChip(service)
            serviceContainer.addView(chip)
        }

        if (!viewModel.hasAnyServices()) hideFuelStationServicesSection()
    }

    private fun hideFuelStationServicesSection() {
        val servicesSection = fuelStationDetailsView.findViewById<LinearLayoutCompat>(R.id.llc_servicesSection)
        servicesSection.visibility = View.GONE

        val serviceSectionSpacer = fuelStationDetailsView.findViewById<View>(R.id.v_serviceSectionSpacer)
        serviceSectionSpacer.visibility = View.GONE
    }

    private fun createServiceChip(service: FuelStationService): Chip {
        val chip = LayoutInflater.from(requireContext()).inflate(R.layout.filter_chip, null, false) as Chip
        initChipAppearance(chip, requireContext())
        chip.text = service.name
        chip.isChecked = false
        chip.isEnabled = false
        return chip
    }

    private fun initReviewSection() {
        loadReviews()

        fuelStationDetailsView.findViewById<NestedScrollView>(R.id.nsv_fuel_details_bottom_sheet)
            .setOnScrollChangeListener { v, scrollX, scrollY, oldScrollX, oldScrollY ->
                val nsv = v as NestedScrollView

                if (oldScrollY < scrollY
                    && scrollY == (nsv.getChildAt(0).measuredHeight - nsv.measuredHeight)
                    && viewModel.hasMoreReviews()) {

                    loadReviews()
                }
            }
    }

    private fun loadReviews() {
        viewModel.getNextPageOfFuelStationReviews(fuelStationId!!)
    }

    private fun initReviewObserver() {
        viewModel.fuelStationReviews.observe(viewLifecycleOwner) { response ->
            val fragmentManager = childFragmentManager
            val fragmentTransaction = fragmentManager.beginTransaction()
            val parent = fuelStationDetailsView.findViewById<LinearLayoutCompat>(R.id.llc_reviewsContainer)

            val page = response.body()

            for (review in page?.data!!) {
                if (review.username == Auth.username) continue

                val reviewFragment = FuelStationReviewFragment(review)
                fragmentTransaction.add(parent.id, reviewFragment)
            }
            fragmentTransaction.commitNow()

            if (!viewModel.hasMoreReviews()) hideReviewSectionProgressBar()
        }
    }

    private fun initNewReviewObserver() {
        viewModel.newUserReview.observe(viewLifecycleOwner) { response ->
            viewModel.getUserReview(fuelStationId!!)

            val text = if (response.isSuccessful) resources.getString(R.string.published)
                       else resources.getString(R.string.an_error_occurred)
            val toast = Toast.makeText(requireContext(), text, Toast.LENGTH_SHORT)
            toast.show()
        }
    }

    private fun initEditedReviewObserver() {
        viewModel.updateUserReview.observe(viewLifecycleOwner) { response ->
            viewModel.getUserReview(fuelStationId!!)

            val text = if (response.isSuccessful) resources.getString(R.string.edited_review)
                       else resources.getString(R.string.an_error_occurred)
            val toast = Toast.makeText(requireContext(), text, Toast.LENGTH_SHORT)
            toast.show()
        }
    }

    private fun initUserReview() {
        viewModel.getUserReview(fuelStationId!!)
        viewModel.userReview.observe(viewLifecycleOwner) { response ->
            if (response.isSuccessful && response.body() != null) {
                addUserReviewToReviewSection(response.body()!!)
                hideAddReviewButton()
            }
        }
    }

    private fun addUserReviewToReviewSection(review: Review) {
        val fragmentManager = childFragmentManager
        val fragmentTransaction = fragmentManager.beginTransaction()
        val parent = fuelStationDetailsView.findViewById<LinearLayoutCompat>(R.id.llc_userReviewContainer)
        parent.removeAllViews()

        val reviewFragment = FuelStationReviewFragment(review)
        fragmentTransaction.add(parent.id, reviewFragment)

        fragmentTransaction.commitNow()
    }

    private fun initDeleteUserReviewObserver() {
        viewModel.deleteUserReview.observe(viewLifecycleOwner) { response ->
            if (response.isSuccessful) {
                removeUserReviewFromReviewSection()
                showAddReviewButton()
            }

            val text = if (response.isSuccessful) resources.getString(R.string.deleted)
                       else resources.getString(R.string.an_error_occurred)
            val toast = Toast.makeText(requireContext(), text, Toast.LENGTH_SHORT)
            toast.show()
        }
    }

    private fun initUserFavouriteObserver() {
        viewModel.getUserFavourite(fuelStationId!!)
        viewModel.userFavourite.observe(viewLifecycleOwner) { response ->
            if (response.isSuccessful) {
                prepareFavouriteButtonToRemoving()
            } else {
                prepareFavouriteButtonStateToAdding()
            }
        }
    }

    private fun initAddToFavouriteObserver() {
        viewModel.addToFavourite.observe(viewLifecycleOwner) { response ->
            if (response.isSuccessful) {
                prepareFavouriteButtonToRemoving()
            }

            val text = if (response.isSuccessful) resources.getString(R.string.added_to_favourite)
            else resources.getString(R.string.an_error_occurred)
            val toast = Toast.makeText(requireContext(), text, Toast.LENGTH_SHORT)
            toast.show()
        }
    }

    private fun initRemoveFavouriteObserver() {
        viewModel.deleteFavourite.observe(viewLifecycleOwner) { response ->
            if (response.isSuccessful) {
                prepareFavouriteButtonStateToAdding()
            }

            val text = if (response.isSuccessful) resources.getString(R.string.removed_from_favourite)
            else resources.getString(R.string.an_error_occurred)
            val toast = Toast.makeText(requireContext(), text, Toast.LENGTH_SHORT)
            toast.show()
        }
    }

    private fun removeUserReviewFromReviewSection() {
        val parent = fuelStationDetailsView.findViewById<LinearLayoutCompat>(R.id.llc_userReviewContainer)
        parent.removeAllViews()
    }

    private fun initAddReviewButton() {
        val button = fuelStationDetailsView.findViewById<MaterialButton>(R.id.mb_rateFuelStation)
        button.setOnClickListener {
            val reviewEditorFragment = FuelStationReviewEditorFragment(null, false)
            reviewEditorFragment.show(requireFragmentManager(), FuelStationReviewEditorFragment.TAG)
        }
    }

    private fun initOcrButton() {
        val button = fuelStationDetailsView.findViewById<AppCompatImageButton>(R.id.acib_ocrButton)
        button.setOnClickListener {
            if (allPermissionsGranted(requireContext(), ocrRequiredPermissions)
                && requireContext().packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)) {
                navigateToScanner()
            } else if (!allPermissionsGranted(requireContext(), ocrRequiredPermissions)) {
                requestPermissionsResultLauncher.launch(ocrRequiredPermissions)
            } else if (!requireContext().packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)) {
                val toast = Toast.makeText(requireContext(), resources.getString(R.string.camera_not_detected), Toast.LENGTH_SHORT)
                toast.show()
            }
        }
    }

    private fun handlePermissions(permissions: Map<String, Boolean>) {
        val deniedList = permissions.filter { !it.value }.map { it.key }

        if (deniedList.isNotEmpty()) {
            val missingPermissions = deniedList.map {
                val v = it.split(".")
                v[v.size - 1]
            }.joinToString(",")
            val text = resources.getString(R.string.missing_permissions, missingPermissions)
            val toast = Toast.makeText(requireContext(), text, Toast.LENGTH_SHORT)
            toast.show()
        } else {
            navigateToScanner()
        }
    }

    private fun navigateToScanner() {
        Navigation.findNavController(requireActivity(), R.id.fragmentContainerView).navigate(R.id.scannerFragment)
    }

    private fun prepareFavouriteButtonToRemoving() {
        val button = fuelStationDetailsView.findViewById<AppCompatImageButton>(R.id.acib_addToFavourite)
        button.isClickable = true
        button.setOnClickListener {
            button.isClickable = false
            viewModel.removeFuelStationFromFavourite(fuelStationId!!)
        }
        button.setImageResource(R.drawable.ic_baseline_star_24)
    }

    private fun prepareFavouriteButtonStateToAdding() {
        val button = fuelStationDetailsView.findViewById<AppCompatImageButton>(R.id.acib_addToFavourite)
        button.isClickable = true
        button.setOnClickListener {
            button.isClickable = false
            viewModel.addFuelStationToFavourite(fuelStationId!!)
        }
        button.setImageResource(R.drawable.ic_baseline_star_border_24)
    }

    private fun showLayout() {
        val layout = fuelStationDetailsView.findViewById<LinearLayoutCompat>(R.id.ll_mainFuelDetailsLayout)
        layout.visibility = View.VISIBLE

        val progressSpinner = fuelStationDetailsView.findViewById<ProgressBar>(R.id.fuel_station_details_loading_spinner)
        progressSpinner.visibility = View.GONE
    }

    private fun hideLayout() {
        val layout = fuelStationDetailsView.findViewById<LinearLayoutCompat>(R.id.ll_mainFuelDetailsLayout)
        layout.visibility = View.GONE

        val progressSpinner = fuelStationDetailsView.findViewById<ProgressBar>(R.id.fuel_station_details_loading_spinner)
        progressSpinner.visibility = View.VISIBLE
    }

    private fun showAddReviewButton() {
        val button = fuelStationDetailsView.findViewById<MaterialButton>(R.id.mb_rateFuelStation)
        button.visibility = View.VISIBLE
    }

    private fun hideAddReviewButton() {
        val button = fuelStationDetailsView.findViewById<MaterialButton>(R.id.mb_rateFuelStation)
        button.visibility = View.GONE
    }

    private fun hideReviewSectionProgressBar() {
        val progressBar = fuelStationDetailsView.findViewById<ProgressBar>(R.id.pb_reviewsLoad)
        progressBar.visibility = View.GONE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        viewModel.clear()
        hideLayout()
    }

    companion object {
        const val TAG = "FuelStationDetails"
    }
}