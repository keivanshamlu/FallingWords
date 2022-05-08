package com.shamlou.fallingwords.ui.fallingWords

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.shamlou.fallingwords.R
import com.shamlou.fallingwords.databinding.FragmentFallingWordsBinding
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel

class FragmentFallingWords : Fragment(R.layout.fragment_falling_words){

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

    private fun setUpView(){


    }

    private fun observeViewModel(){

        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.allWords.collect {

                    Toast.makeText(requireContext(), "${it.data?.size} - ${it.status.name}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}