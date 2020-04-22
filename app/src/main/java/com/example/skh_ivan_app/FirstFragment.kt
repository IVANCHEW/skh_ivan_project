package com.example.skh_ivan_app

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.navigation.fragment.findNavController

/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
class FirstFragment : Fragment() {

    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_first, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<Button>(R.id.button_count).setOnClickListener {
            val myToast = Toast.makeText(context, "Count!", Toast.LENGTH_SHORT)
            myToast.show()
            countMe(view)
            // findNavController().navigate(R.id.action_FirstFragment_to_SecondFragment)
        }

        view.findViewById<Button>(R.id.button_toast).setOnClickListener {
            val myToast = Toast.makeText(context, "Hello Toast!", Toast.LENGTH_SHORT)
            myToast.show()
        }

        val setZero = view.findViewById<TextView>(R.id.textview_count)
        var zeroHolder = 0
        setZero.text = zeroHolder.toString()
    }

    private fun countMe(view: View){
        val showCountTextView = view.findViewById<TextView>(R.id.textview_count)
        val countString = showCountTextView.text.toString()
        var count = countString.toInt()
        count++
        showCountTextView.text = count.toString()
    }

}