package com.example.fuel.ui.fragment.scanner

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import com.example.fuel.databinding.FragmentPriceUploadingBinding
import com.example.fuel.viewmodel.ScannerViewModel
import com.example.fuel.viewmodel.ViewModelFactory

class PriceUploadingFragment : Fragment() {
    private lateinit var binding: FragmentPriceUploadingBinding
    private lateinit var viewModel: ScannerViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentPriceUploadingBinding.inflate(inflater, container, false)
        viewModel = ViewModelProvider(requireActivity(), ViewModelFactory())[ScannerViewModel::class.java]

        initUploadFuelPricesObserver()
        showUploadingScreen()

        return binding.root
    }

    private fun initUploadFuelPricesObserver() {
        viewModel.uploadFuelPrices(requireActivity())
        viewModel.uploadFuelPrices.observe(viewLifecycleOwner) { response ->
            showUploadedScreen()

            var str = binding.tvUploadedInfo.text.toString()
            str += str + " " + response.code()
            binding.tvUploadedInfo.text = str
        }
    }

    private fun showUploadingScreen() {
        binding.clUploadedContainer.visibility = View.GONE
        binding.clUploadContainer.visibility = View.VISIBLE
    }

    private fun showUploadedScreen() {
        binding.clUploadedContainer.visibility = View.VISIBLE
        binding.clUploadContainer.visibility = View.GONE
    }

    override fun onDestroyView() {
        super.onDestroyView()

        viewModel.clear()
    }
}