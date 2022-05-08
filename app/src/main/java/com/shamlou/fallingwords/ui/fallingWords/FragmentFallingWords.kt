package com.shamlou.fallingwords.ui.fallingWords

import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.shamlou.fallingwords.R
import com.shamlou.fallingwords.databinding.FragmentFallingWordsBinding
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.util.*


class FragmentFallingWords : Fragment(R.layout.fragment_falling_words) {

    private val viewModel: FallingWordsViewModel by viewModel()

    private var _binding: FragmentFallingWordsBinding? = null

    // This property is only valid between onCreateView and onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFallingWordsBinding.inflate(inflater, container, false)
        val view = binding.root
        return view
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setUpView()
        observeViewModel()
    }

    private fun setUpView() {

        binding.buttonCorrect.setOnClickListener {
            startTimer()
        }
        setupTimeNumberPicker()
        setupSpeedNumberPicker()
    }

    private fun observeViewModel() {

        //gets all words and also have the state of loading, error, success
        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.allWords.collect {

                    Toast.makeText(
                        requireContext(),
                        "${it.data?.size} - ${it.status.name}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

        //observes timeleft and show the last value in UI
        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.timeLeft.collect {

                    val minutes = (it / 1000).toInt() / 60
                    val seconds = (it / 1000).toInt() % 60
                    val timeLeftFormatted: String = java.lang.String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
                    binding.textViewCountDown.text = timeLeftFormatted
                }
            }
        }
        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.viewState.collect {

                    binding.viewGroupGaming.isVisible = it is ViewState.Gaming

                    when(it){
                        is ViewState.SetUpGame -> {


                        }
                        is ViewState.Gaming -> {


                        }
                        is ViewState.Result -> {


                        }
                    }
                }
            }
        }
    }

    //starts the counter with value of [timeLeft] in viewModel
    //i keep the time in viewmodel so it can survive configure changes
    private fun startTimer() {

        object : CountDownTimer(viewModel.timeLeft.value, 1000) {
            override fun onTick(millisUntilFinished: Long) {

                viewModel.setTimeLeft(millisUntilFinished)
            }

            override fun onFinish() {

                viewModel.timerFinished()
            }
        }.start()
    }

    private fun setupTimeNumberPicker() {
        val values = GameTime.values().map { it.title }.toTypedArray()
        with(binding.numberPickerTime){
            minValue = 0
            maxValue = values.size - 1
            displayedValues = values
            wrapSelectorWheel = true
            setOnValueChangedListener { picker, oldVal, newVal ->
                val text = "Changed from " + values[oldVal] + " to " + values[newVal]
                Log.d("TESTEST", GameTime.values()[newVal].name)
                viewModel.timeSelected(GameTime.values()[newVal])
            }
            value = values.size-1
        }

    }
    private fun setupSpeedNumberPicker() {
        val values = Speed.values().map { it.title }.toTypedArray()
        with(binding.numberPickerSpeed){
            minValue = 0
            maxValue = values.size - 1
            displayedValues = values
            wrapSelectorWheel = true
            setOnValueChangedListener { picker, oldVal, newVal ->
                val text = "Changed from " + values[oldVal] + " to " + values[newVal]
                Log.d("TESTEST", GameTime.values()[newVal].name)
                viewModel.timeSelected(GameTime.values()[newVal])
            }
            value = values.size-1
        }

    }

}