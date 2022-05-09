package com.shamlou.fallingwords.ui.fallingWords

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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

    private val animatorSet = AnimatorSet()

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
        animatorSet.removeAllListeners()
    }

    override fun onResume() {
        super.onResume()
        Handler(Looper.getMainLooper()).postDelayed({
            viewModel.fragmentResume()
        },1000)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setUpView()
        observeViewModel()
    }

    private fun setUpView() {

        with(binding){

            buttonCorrect.setOnClickListener {
                animatorSet.cancel()
                viewModel.correctClicked()
            }

            buttonWrong.setOnClickListener {
                animatorSet.cancel()
                viewModel.wrongClicked()
            }
            buttonStartGame.setOnClickListener {
                viewModel.startGameClicked()
            }

            buttonResetGame.setOnClickListener {
                viewModel.resetGameClicked()
            }
        }
    }

    private fun observeViewModel() {

        //observes timeleft and show the last value in UI
        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.timeLeft.collect {

                    val minutes = (it / 1000).toInt() / 60
                    val seconds = (it / 1000).toInt() % 60
                    val timeLeftFormatted: String =
                        java.lang.String.format(Locale.getDefault(), "%02d       %02d", minutes, seconds)
                    binding.textViewCountDown.text = timeLeftFormatted
                }
            }
        }

        // observe [startGameButtonEnable] , words should be loaded,
        // and game speed and game time should be seleccted
        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.startGameButtonEnable.collect {

                    binding.buttonStartGame.isEnabled = it
                }
            }
        }

        // this will be observe new event every time there
        // should be new word falling down from top of the screen
        // starts anim from beginning, [startAnimEvent] is an event,
        // we update the text views, and play the animation
        // i used [animatorSet.playSequentially] so view can move back to the top of the view
        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.startAnimEvent.collect {
                    it?.getContentIfNotHandled()?.let {

                        //update texts
                        binding.textViewMovingText.text = it.first.text_spa
                        binding.textViewFixed.text = it.first.text_eng

                        animatorSet.playSequentially(
                            //if we dont use [- binding.textViewMovingText.height] it will move out of the screen
                            ObjectAnimator
                                .ofFloat(binding.textViewMovingText, "translationY", binding.root.height.toFloat() - binding.textViewMovingText.height)
                                .setDuration(it.second.duration),
                            ObjectAnimator
                                .ofFloat(binding.textViewMovingText, "translationY", 0f)
                                .setDuration(0)
                        )

                        // we add listener so we can find out animation finished or canceled
                        animatorSet.addListener(object : Animator.AnimatorListener {
                            override fun onAnimationStart(p0: Animator?) {
                            }

                            override fun onAnimationEnd(p0: Animator?) {

                                // remove listener so we can make sure last
                                // listeners are not making miss behaviors
                                animatorSet.removeAllListeners()
                                // inform view model about finishing animation
                                viewModel.animationFinished()
                            }

                            override fun onAnimationCancel(p0: Animator?) {

                                //when user clicked on correct or wrong button, we remove listeners
                                animatorSet.removeAllListeners()
                            }

                            override fun onAnimationRepeat(p0: Animator?) {
                            }

                        })
                        animatorSet.start()
                    }
                }
            }
        }

        //when user clicks on start game
        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.startTimerEvent.collect {
                    it?.getContentIfNotHandled()?.let {

                        startTimer(it)
                    }
                }
            }
        }

        // view state is the single source of truth of the UI of this fragment
        // we abserve it so we can make diffrent desisions on diffrent states [set up, gaming, result]
        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.viewState.collect {

                    //load the view group[contains all the views about the state]
                    binding.viewGroupGaming.isVisible = it is ViewState.Gaming
                    binding.viewGroupStartGame.isVisible = it is ViewState.SetUpGame
                    binding.viewGroupGameResult.isVisible = it is ViewState.Result
                    when (it) {
                        is ViewState.SetUpGame -> {

                            // set up pickers for game speed and game time,
                            // we also send current values position and we
                            // will set it as the current pos in picker so
                            // it can survive configure changes
                            setupTimeNumberPicker(GameTime.values().indexOf(it.gameTime))
                            setupSpeedNumberPicker(GameSpeed.values().indexOf(it.gameSpeed))
                            binding.textViewGameInfo.text =
                                "game speed : ${it.gameSpeed.title} \n game time : ${it.gameTime.title}"
                        }
                        is ViewState.Result -> {

                            //showing result after game finished in [ViewState.Result] state
                            binding.textViewGameResult.text =
                                "all questions : ${it.allQuestions} \n correct answers : ${it.currectAnswers} \n wrong answers : ${it.wrongAnswers}"
                        }
                    }
                }
            }
        }
    }

    //starts the counter with value of [timeLeft] in viewModel
    //i keep the time in viewmodel so it can survive configure changes
    private fun startTimer(time: Long) {

        object : CountDownTimer(time, 1000) {
            override fun onTick(millisUntilFinished: Long) {

                viewModel.setTimeLeft(millisUntilFinished)
            }

            override fun onFinish() {

                viewModel.timerFinished()
            }
        }.start()
    }

    // set up time picker, pos is default position
    private fun setupTimeNumberPicker(pos : Int) {
        val values = GameTime.values().map { it.title }.toTypedArray()
        with(binding.numberPickerTime) {
            minValue = 0
            maxValue = values.size - 1
            displayedValues = values
            wrapSelectorWheel = true
            setOnValueChangedListener { _, _, newVal ->
                viewModel.timeSelected(GameTime.values()[newVal])
            }
            value = pos
        }

    }
    // set up speed picker, pos is default position
    private fun setupSpeedNumberPicker(pos : Int) {
        val values = GameSpeed.values().map { it.title }.toTypedArray()
        with(binding.numberPickerSpeed) {
            minValue = 0
            maxValue = values.size - 1
            displayedValues = values
            wrapSelectorWheel = true
            setOnValueChangedListener { _, _, newVal ->
                viewModel.speedSelected(GameSpeed.values()[newVal])
            }
            value = pos
        }

    }

}