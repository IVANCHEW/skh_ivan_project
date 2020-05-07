package com.example.skh_ivan_app

import android.annotation.SuppressLint
import android.graphics.Typeface
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.isVisible
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.JsonArrayRequest
import com.android.volley.toolbox.Volley
import kotlinx.android.synthetic.main.fragment_second.view.*
import org.json.JSONObject
import java.lang.Exception
import java.text.SimpleDateFormat

/**
 * A simple [Fragment] subclass as the second destination in the navigation.
 */
class SecondFragment : Fragment() {

    val TAG = "Manual TAG"
    val GMT_STANDARD = 8

    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_second, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val pb = view.findViewById<ProgressBar>(R.id.progressBar2)
        pb.isVisible = true
        queryAllWorkshopDates("Ivan Chew")
    }

    private fun queryAllWorkshopDates(hostName: String){

        Log.i(TAG, "Manual Log, queryAllWorkshopDates, called")
        val pb = view!!.findViewById<ProgressBar>(R.id.progressBar2)
        val url = "https://ivan-chew.outsystemscloud.com/Chew_Database/rest/RestAPI/Query_Host_All_Workshops_Dates?Host_Name=$hostName"
        val request = JsonArrayRequest(
            Request.Method.POST, url, null, Response.Listener { response ->
                if (response.length()==0){
                    Toast.makeText(context,"There are no workshop dates created by host.", Toast.LENGTH_LONG).show()
                }else{
                    for (i in 0 until response.length()){
                        createWorkshopListCard(response.getJSONObject(i))
                    }
                    pb.isVisible = false
                }
            }, Response.ErrorListener { error ->
                Log.i(TAG, "Manual Log, queryAllWorkshopDates, request error callback: $error")
                pb.isVisible = false
            }
        )
        VolleySingleton.getInstance(context!!).addToRequestQueue(request)
    }

    @SuppressLint("SetTextI18n")
    private fun createWorkshopListCard(workshopStructure: JSONObject){
        val mainLayout = view!!.findViewById<LinearLayout>(R.id.object_List_Linear_Layout_SecondFragment)

        //Headers
        val cardLayoutParam = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT)
        cardLayoutParam.setMargins(30, 30, 0, 30)
        val textViewLayoutParam = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT)
        textViewLayoutParam.setMargins(60, 10, 0, 0)

        //Create Card
        val cardLayout = LinearLayout(context)
        cardLayout.orientation = LinearLayout.VERTICAL
        cardLayout.layoutParams = cardLayoutParam
        val workshopObject = workshopStructure.getJSONObject("Workshop")

        val workshopTitle = workshopObject.getString("Workshop_Name")
        val headerTextview = TextView(context)
        headerTextview.text = workshopTitle
        headerTextview.textSize = 20F
        headerTextview.typeface = Typeface.DEFAULT_BOLD
        cardLayout.addView(headerTextview)

        try {
            val datesList = workshopStructure.getJSONArray("Dates")
            val bookedCountList = workshopStructure.getJSONArray("Slots_Booked")
            val cardTextViews = MutableList(datesList.length()) { TextView(context) }
            for (j in 0 until datesList.length()) {
                val workshopDateObject = datesList.getJSONObject(j)
                val workshopDate = workshopDateObject.getString("Date_Time")
                val parsedDate = parseDateTime(workshopDate)
                val workshopSlotsAvailable = workshopDateObject.getString("Slots_Available")
                val workshopBookingObject = bookedCountList.getJSONObject(j)
                val workshopBookingCount = workshopBookingObject.getInt("Count")
                cardTextViews[j].text =
                    "$parsedDate (Bookings: $workshopBookingCount / Slots: $workshopSlotsAvailable)"
                cardTextViews[j].layoutParams = textViewLayoutParam
                cardTextViews[j].textSize = 17F
                cardLayout.addView(cardTextViews[j])
            }
        }catch (e: Exception){
            Log.i(TAG, "Manual Log, createWorkshopListCard error: " + e.message)
            val cardErrorText = TextView(context)
            cardErrorText.text = "No available dates for this workshop"
            cardErrorText.layoutParams = textViewLayoutParam
            cardErrorText.textSize = 17F
            cardLayout.addView(cardErrorText)
        }

        mainLayout.addView(cardLayout)
    }

    @SuppressLint("SimpleDateFormat")
    private fun parseDateTime(inputDateTime: String): String{
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'")
        val slotDate = sdf.parse(inputDateTime)
        val hourString: String
        val minuteString: String
        val dateString: String
        val monthString: String

        val hour: Int = slotDate.hours
        val minutes: Int = slotDate.minutes
        val date: Int = slotDate.date
        val month: Int = slotDate.month

        hourString = if (hour + GMT_STANDARD < 10) {
            "0" + (hour + GMT_STANDARD).toString()
        } else {
            (hour + GMT_STANDARD).toString()
        }

        minuteString = if (minutes < 10){
            "0$minutes"
        }else{
            minutes.toString()
        }

        dateString = if (date < 10){
            "0$date"
        }else{
            date.toString()
        }

        monthString = if (month + 1 < 10){
            "0" + (month + 1).toString()
        }else{
            (month +1).toString()
        }

        return "$dateString/$monthString,  $hourString : $minuteString"
    }
}
