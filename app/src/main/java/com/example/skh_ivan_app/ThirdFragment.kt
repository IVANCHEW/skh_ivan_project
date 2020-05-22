package com.example.skh_ivan_app

import android.content.ContentValues.TAG
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.RequiresApi
import org.json.JSONArray

class ThirdFragment : Fragment() {
    companion object {
        var workshopName = ""
        var workshopSlot = ""
        val list: MutableList<String> = ArrayList()

        fun populateObjects(jarray: JSONArray){
            Log.i(TAG, "Manual Log, populateObjects, called")
            list.clear()
            for (i in 0 until jarray.length()){
                val jsonObject = jarray.getJSONObject(i)
                val bookingUsername = jsonObject.getString("User")
                val bookingContact = jsonObject.getString("Contact_Number")
                val bookingQuantity = jsonObject.getInt("Quantity")
                Log.i(TAG, "Manual Log, populateObjects, $bookingUsername, $bookingContact, $bookingQuantity")
                list.add("$bookingUsername, ($bookingContact), Qty: $bookingQuantity")
            }
        }

    }

    val TAG = "Manual TAG"
    val GMT_STANDARD = 8

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_third, container, false)
    }

    @RequiresApi(Build.VERSION_CODES.N)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.i(TAG, "Manual Log, onViewCreated, Third Fragment Created")
        MainActivity.selectThirdFragment()
        view.findViewById<TextView>(R.id.title1TextView).text = workshopName
        view.findViewById<TextView>(R.id.title2TextView).text = workshopSlot
        val objectLayout = view.findViewById<LinearLayout>(R.id.object_List_Linear_Layout)
        val listTextView = MutableList(list.size) { TextView(context) }
        for (i in 0 until list.size){
            listTextView[i].text = list[i]
            objectLayout.addView(listTextView[i])
        }
    }

    fun addObject(inputText: String){

    }
}
