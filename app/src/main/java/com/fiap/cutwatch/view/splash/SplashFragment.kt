package com.fiap.cutwatch.view.splash

import android.animation.Animator
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.fiap.cutwatch.R
import com.fiap.cutwatch.databinding.FragmentSplashBinding

class SplashFragment : Fragment() {
    private var binding: FragmentSplashBinding? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {

        binding = FragmentSplashBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViewComponent()
        //fullScreen()
    }

    private fun initViewComponent() {
        binding?.splashAnimation?.addAnimatorListener(object : Animator.AnimatorListener {
            override fun onAnimationStart(animation: Animator) {
                //Nothing to do
            }

            override fun onAnimationEnd(animation: Animator) {
                findNavController().navigate(R.id.action_splashFragment_to_loginFragment)
            }

            override fun onAnimationCancel(animation: Animator) {
                //Nothing to do
            }

            override fun onAnimationRepeat(animation: Animator) {
                //Nothing to do
            }
        })
    }
}