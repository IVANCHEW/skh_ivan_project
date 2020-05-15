package com.example.skh_ivan_app

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.ContentValues
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.text.InputType
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.core.view.isVisible
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.JsonArrayRequest
import com.android.volley.toolbox.JsonObjectRequest
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

    @RequiresApi(Build.VERSION_CODES.N)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val pb = view.findViewById<ProgressBar>(R.id.progressBar2)
        pb.isVisible = true
        queryAllWorkshopDates(UserManagement.params["username"].toString())
    }

    @RequiresApi(Build.VERSION_CODES.N)
    private fun queryAllWorkshopDates(hostName: String){
        Log.i(TAG, "Manual Log, queryAllWorkshopDates, called")
        val pb = view!!.findViewById<ProgressBar>(R.id.progressBar2)
        val url = "https://ivan-chew.outsystemscloud.com/Chew_Database/rest/RestAPI/Query_Host_All_Workshops_Dates?Host_Name=$hostName"
        val request = JsonArrayRequest(
            Request.Method.POST, url, null, Response.Listener { response ->
                if (response.length()==0){
                    Toast.makeText(context,"There are no workshop dates created by host.", Toast.LENGTH_LONG).show()
                }else{
                    Log.i(TAG, "Manual Log, queryAllWorkshopDates, number of workshops:" + response.length())
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

    private fun queryCreateWorkshopDate(hostName: String, objectID: Int, objectDateTime: String, objectSlots: Int){
        val params = HashMap<String,String>()
        params["Workshop_ID"] = objectID.toString()
        params["Date_Time"] = objectDateTime
        params["Slots_Available"] = objectSlots.toString()
        params["Username"] = hostName
        val jsonObject = JSONObject(params as Map<*, *>)
        Log.i(TAG, "Manual Log, queryCreateWorkshopDate, called. date: $jsonObject")
        val url = "https://ivan-chew.outsystemscloud.com/Chew_Database/rest/RestAPI/Create_Workshop_Date"
        val request = JsonObjectRequest(
            Request.Method.POST, url, jsonObject, Response.Listener { response ->
                    Toast.makeText(context,response.getString("status"), Toast.LENGTH_LONG).show()
                    reloadObjectList()
            }, Response.ErrorListener { error ->
                Log.i(TAG, "Manual Log, queryCreateWorkshopDate, request error callback: $error")
            }
        )
        VolleySingleton.getInstance(context!!).addToRequestQueue(request)
    }

    @RequiresApi(Build.VERSION_CODES.N)
    @SuppressLint("SetTextI18n")
    private fun createAlertDialogue(objectID: Int, objectTitle: String){
        Log.i(ContentValues.TAG, "Manual Log, createAlertDialogue, called")

        //General Variables
        var selectedYear = "2020"
        var selectedMonth = ""
        var selectedDay = ""
        var selectedHour = ""
        var selectedMinute = ""
        //Construct the alert dialogue
        val newObjectDialogue = AlertDialog.Builder(context)
        newObjectDialogue.setTitle("Create New Workshop Date ($objectTitle)")

        val dialogueLinearLayout = LinearLayout(context)
        dialogueLinearLayout.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT)
        dialogueLinearLayout.orientation = LinearLayout.VERTICAL
        val newObjectTextInput = Array(1){ EditText(context) }
        newObjectTextInput[0].hint = "Number of slots"

        for (i in newObjectTextInput.indices){
            newObjectTextInput[i].inputType = InputType.TYPE_CLASS_TEXT
            dialogueLinearLayout.addView(newObjectTextInput[i])
        }

        val buttonLayoutParam = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.MATCH_PARENT)
        buttonLayoutParam.setMargins(30, 0, 30, 0)

        val chooseDateButton = Button(context)
        chooseDateButton.text = "Select Date"
        chooseDateButton.setOnClickListener {
            val dateDialog = context?.let { DatePickerDialog(it) }
            dateDialog?.setOnDateSetListener { view, year, month, dayOfMonth ->
                selectedMonth = if (month + 1 < 10){
                    "0" + (month + 1).toString()
                }else{
                    (month +1).toString()
                }
                selectedDay = if (dayOfMonth < 10){
                    "0" + (dayOfMonth).toString()
                }else{
                    (dayOfMonth).toString()
                }
                selectedYear = year.toString()
                chooseDateButton.text = "$dayOfMonth-$selectedMonth-$year"
            }
            dateDialog!!.show()
        }
        chooseDateButton.layoutParams=buttonLayoutParam
        dialogueLinearLayout.addView(chooseDateButton)

        val chooseTimeButton = Button(context)
        chooseTimeButton.text = "Select Time"
        chooseTimeButton.setOnClickListener {
            val timeSetListener = TimePickerDialog.OnTimeSetListener { timePicker, hour, minute ->
                chooseTimeButton.text = "$hour : $minute"
                selectedHour = if (hour - GMT_STANDARD < 10){
                    "0" + (hour - GMT_STANDARD).toString()
                }else{
                    (hour - GMT_STANDARD).toString()
                }
                selectedMinute = if (minute < 10){
                    "0" + (minute).toString()
                }else{
                    (minute).toString()
                }
            }
            val timeDialog = TimePickerDialog(context,timeSetListener,8,0,false)
            timeDialog.show()
        }
        chooseTimeButton.layoutParams=buttonLayoutParam
        dialogueLinearLayout.addView(chooseTimeButton)

        newObjectDialogue.setView(dialogueLinearLayout)

        //Accounting the GMT time is required here
        Log.i(TAG, "Manual Log, createAlertDialogue, Querying for: " + UserManagement.params["username"])
        newObjectDialogue.setPositiveButton("Create"){ _, _ ->
            queryCreateWorkshopDate(UserManagement.params["username"].toString(), objectID, "$selectedYear-$selectedMonth-$selectedDay" +
                    "T" + (selectedHour) + ":$selectedMinute:00Z", newObjectTextInput[0].text.toString().toInt())
        }

        newObjectDialogue.setNeutralButton("Cancel"){_,_ ->
            Toast.makeText(context,"You cancelled the dialog.", Toast.LENGTH_SHORT).show()
        }

        newObjectDialogue.show()
    }

    @RequiresApi(Build.VERSION_CODES.N)
    @SuppressLint("SetTextI18n")
    private fun createWorkshopListCard(workshopStructure: JSONObject){
        val mainLayout = view!!.findViewById<LinearLayout>(R.id.card_List_Linear_Layout_SecondFragment)

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

        //Prepare title
        val workshopTitle = workshopObject.getString("Workshop_Name")
        val workshopID = workshopObject.getInt("Id")
        val headerTextview = TextView(context)
        headerTextview.text = workshopTitle
        headerTextview.textSize = 20F
        headerTextview.typeface = Typeface.DEFAULT_BOLD
        val headerLayout = LinearLayout(context)
        headerLayout.orientation = LinearLayout.HORIZONTAL

        //Prepare Button
        val addWorkshopDateButton = Button(context)
        val plusIcon = resources.getDrawable(android.R.drawable.ic_input_add)
        addWorkshopDateButton.setCompoundDrawablesWithIntrinsicBounds(plusIcon,null,null,null)
        val customViewListener = View.OnClickListener {
            Log.i(ContentValues.TAG, "On click listener called: $workshopID")
            createAlertDialogue(workshopID, workshopTitle)
        }
        addWorkshopDateButton.setOnClickListener(customViewListener)

        //Add to views
        headerLayout.addView(addWorkshopDateButton)
        headerLayout.addView(headerTextview)
        cardLayout.addView(headerLayout)

        try {
            val datesList = workshopStructure.getJSONArray("Dates")
            val bookedCountList = workshopStructure.getJSONArray("Slots_Booked")
            val cardTextViews = MutableList(datesList.length()) { TextView(context) }
            for (j in 0 until datesList.length()) {
                Log.i(TAG, "Manual Log, createWorkshopListCard, $workshopTitle, number of dates: " + datesList.length())
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

    private fun reloadObjectList(){
        val mainLayout = view!!.findViewById<LinearLayout>(R.id.card_List_Linear_Layout_SecondFragment)
        mainLayout.removeAllViews()
        val pb = view!!.findViewById<ProgressBar>(R.id.progressBar2)
        pb.isVisible = true
        queryAllWorkshopDates(UserManagement.params["username"].toString())
    }
}
