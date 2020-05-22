package com.example.skh_ivan_app

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Typeface
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Bundle
import android.speech.RecognizerIntent
import android.text.InputType
import android.util.Base64
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
import com.android.volley.toolbox.StringRequest
import com.applandeo.materialcalendarview.CalendarView
import com.applandeo.materialcalendarview.EventDay
import com.applandeo.materialcalendarview.listeners.OnDayClickListener
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.iid.FirebaseInstanceId
import com.google.firebase.messaging.FirebaseMessaging
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.lang.Exception
import java.net.NetworkInterface
import java.text.SimpleDateFormat
import java.util.*
import java.util.Calendar.*
import kotlin.collections.ArrayList

/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
class FirstFragment : Fragment(), OnDayClickListener {

    private val REQUEST_CODE_SPEECH_INPUT = 100
    private var ACTIVE_WORKSHOP_ID = ""
    private val GMT_STANDARD = 8
    private var FIRST_LOAD = true

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?

    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_first, container, false)
    }

    @RequiresApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        queryMACAddress()
        firebaseManager()
        MainActivity.selectFirstFragment()
        super.onViewCreated(view, savedInstanceState)
        Log.i(TAG, "Manual Log, onViewCreated, called")
        view.viewTreeObserver.addOnGlobalLayoutListener {
            Log.i(TAG, "Manual Log, addOnGlobalLayoutListener, called")
            if(FIRST_LOAD){
                FIRST_LOAD = false
                queryWorkshopList()
            }
        }
    }

    override fun onPause() {
        Log.i(TAG, "Manual Log, firstFragment onPause, called")
        super.onPause()
    }

    override fun onResume() {
        Log.i(TAG, "Manual Log, firstFragment onResume, called")
        FIRST_LOAD = true
        super.onResume()
    }

    private fun firebaseManager(){
        Log.i(TAG, "Manual Log, firebaseManager, called")
        FirebaseInstanceId.getInstance().instanceId
            .addOnCompleteListener(OnCompleteListener { task ->
                if (!task.isSuccessful) {
                    Log.i(TAG, "getInstanceId failed", task.exception)
                    return@OnCompleteListener
                }
                val token = task.result?.token
                val msg = getString(R.string.msg_token_fmt, token)
                Log.i(TAG, msg)
            })
        //FirebaseMessaging.getInstance().subscribeToTopic("GENERAL");
    }

    // Create a card view
    @SuppressLint("SetTextI18n")
    private fun createWorkshopCard(workshopName: String,workshopType: String,workshopCost: Double,
                                   workshopLength: Double, workshopDescription: String,
                                   reSizedWorkshopThumbnail: Bitmap,
                                   workshopID: String){
        Log.i(TAG, "Manual Log, create card")
        //Headers
        val cardLinearLayout = view!!.findViewById<LinearLayout>(R.id.object_List_Linear_Layout)

        //-Header: parameter used by button and textview
        val textviewLayoutParam = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.MATCH_PARENT)
        textviewLayoutParam.setMargins(10, 10, 0, 0)

        //-Header: parameter used by butt Imageview
        val imageViewLayoutParam = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT)
        imageViewLayoutParam.setMargins(0, 0, 0, 0)

        //-Header: parameter used by cardlayout
        val cardLayoutParam = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT)
        cardLayoutParam.setMargins(0, 30, 0, 30)

        // Create the parent layout
        val cardLayout = LinearLayout(context)
        cardLayout.layoutParams = cardLayoutParam
        cardLayout.orientation = LinearLayout.VERTICAL

        // Add the book cover thumbnail
        val cardImageView = ImageView(context)
        cardImageView.setImageBitmap(reSizedWorkshopThumbnail)
        cardImageView.layoutParams = imageViewLayoutParam
        val customImageViewListener = View.OnClickListener {
            Log.i(TAG, "On click listener called: $workshopID")
            queryObjectDates(workshopID)
        }
        cardImageView.setOnClickListener(customImageViewListener)
        cardLayout.addView(cardImageView)

        // Prepare the text
        val textLayout = LinearLayout(context)
        textLayout.layoutParams = textviewLayoutParam
        textLayout.orientation = LinearLayout.VERTICAL

        val textViewArray = Array(3){TextView(context)}

        textViewArray[0].text = workshopName
        textViewArray[1].text = workshopDescription
        textViewArray[2].text = "(Cost: $workshopCost SGD) (Duration: $workshopLength hours)"

        for (i in textViewArray.indices) {
            textViewArray[i].layoutParams = textviewLayoutParam
            textLayout.addView(textViewArray[i])
        }

        //Add the generated card layout to the existing layout
        cardLayout.addView(textLayout)
        cardLinearLayout.addView(cardLayout)
    }

    private fun createRegistrationButtons(){
        val userLayout = view!!.findViewById<LinearLayout>(R.id.User_Linear_Layout)

        val createUserButton = Button(context)
        createUserButton.text = "New User"
        createUserButton.setOnClickListener{
            createNewUser()
        }

        val registerDeviceButton = Button(context)
        registerDeviceButton.text = "Register Device"
        registerDeviceButton.setOnClickListener {
            registerDevice()
        }

        val buttonLayoutParam = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT)
        createUserButton.layoutParams = buttonLayoutParam
        registerDeviceButton.layoutParams = buttonLayoutParam
        userLayout.addView(createUserButton)
        userLayout.addView(registerDeviceButton)
    }

    @SuppressLint("SetTextI18n")
    private fun createNewUser(){
        Log.i(TAG, "Manual Log, createNewUser, called")

        //Build dialogue options
        val newObjectDialogue = AlertDialog.Builder(context)
        newObjectDialogue.setTitle("New User Creation")
        val dialogueLinearLayout = LinearLayout(context)
        dialogueLinearLayout.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT)
        dialogueLinearLayout.orientation = LinearLayout.VERTICAL

        //-Specify input options
        val editTextArray = Array(6 ){EditText(context)}
        val editTextParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT)
        editTextParams.setMargins(50, 0, 100, 0)
        editTextArray[0].setHint("Username")
        editTextArray[1].setHint("Password")
        editTextArray[2].setHint("Re-enter Password")
        editTextArray[3].setHint("Your name")
        editTextArray[4].setHint("Contact Number")
        editTextArray[5].setHint("Email")

        editTextArray[2].inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        editTextArray[1].inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD

        //TODO For testing
        editTextArray[0].setText("BearBearChew")
        editTextArray[1].setText("helloworld")
        editTextArray[2].setText("helloworld")
        editTextArray[3].setText("Ivan Chew")
        editTextArray[4].setText("94357200")
        editTextArray[5].setText("ivanchew92@gmail.com")

        for (i in editTextArray.indices){
            editTextArray[i].layoutParams = editTextParams
            dialogueLinearLayout.addView(editTextArray[i])
        }

        newObjectDialogue.setView(dialogueLinearLayout)
        newObjectDialogue.setNeutralButton("Cancel"){_,_ ->
            Toast.makeText(context,"You cancelled the dialog.",Toast.LENGTH_SHORT).show()
            createRegistrationButtons()
        }
        newObjectDialogue.setPositiveButton("Create"){ _, _ ->

            //TODO Create password valdiation function

            queryCreateNewUser(editTextArray[0].text.toString(),
                editTextArray[3].text.toString(),
                editTextArray[1].text.toString(),
                editTextArray[5].text.toString(),
                editTextArray[4].text.toString())
        }
        newObjectDialogue.show()
    }

    private fun createWelcomeMessage(username: String){
        val userLayout = view!!.findViewById<LinearLayout>(R.id.User_Linear_Layout)
        userLayout.removeAllViews()
        val welcomeMessage = TextView(context)
        welcomeMessage.text = "Welcome $username"
        welcomeMessage.typeface = Typeface.DEFAULT_BOLD
        userLayout.addView(welcomeMessage)
    }

    private fun registerDevice(){
        Log.i(TAG, "Manual Log, registerDevice, called")

        //Build dialogue options
        val newObjectDialogue = AlertDialog.Builder(context)
        newObjectDialogue.setTitle("New device registration, please log in")
        val dialogueLinearLayout = LinearLayout(context)
        dialogueLinearLayout.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT)
        dialogueLinearLayout.orientation = LinearLayout.VERTICAL

        //-Specify input options
        val editTextArray = Array(2 ){EditText(context)}
        val editTextParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT)
        editTextParams.setMargins(50, 0, 100, 0)
        editTextArray[0].setHint("Username")
        editTextArray[1].setHint("Password")

        for (i in editTextArray.indices){
            editTextArray[i].layoutParams = editTextParams
            dialogueLinearLayout.addView(editTextArray[i])
        }

        newObjectDialogue.setView(dialogueLinearLayout)
        newObjectDialogue.setNeutralButton("Cancel"){_,_ ->
            Toast.makeText(context,"You cancelled the dialog.",Toast.LENGTH_SHORT).show()
            createRegistrationButtons()
        }
        newObjectDialogue.setPositiveButton("Register"){ _, _ ->
            queryCreateNewDeviceRegistration(editTextArray[0].text.toString(),
                editTextArray[1].text.toString())
        }
        newObjectDialogue.show()
    }

    @SuppressLint("SetTextI18n")
    private fun promptUserLogin(){
        Log.i(TAG, "Manual Log, promptUserLogin, called")

        val newObjectDialogue = AlertDialog.Builder(context)
        newObjectDialogue.setNeutralButton("Cancel"){_,_ ->
            Toast.makeText(context,"You cancelled the dialog.", Toast.LENGTH_SHORT).show()
            createRegistrationButtons()
        }
        newObjectDialogue.setTitle("Device is currently not registered with a user")
        val dialogueObject = newObjectDialogue.create()

        val dialogueLinearLayout = LinearLayout(context)
        dialogueLinearLayout.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT)
        dialogueLinearLayout.orientation = LinearLayout.VERTICAL

        val buttonLayoutParam = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.MATCH_PARENT)
        buttonLayoutParam.setMargins(30, 0, 30, 0)

        val newUserButton = Button(context)
        newUserButton.text = "Create new user account"
        newUserButton.setOnClickListener{
            createNewUser()
            dialogueObject.dismiss()
        }
        newUserButton.layoutParams=buttonLayoutParam
        dialogueLinearLayout.addView(newUserButton)

        val registerDeviceButton = Button(context)
        registerDeviceButton.text = "Register new device to user"
        registerDeviceButton.setOnClickListener {
            registerDevice()
            dialogueObject.dismiss()
        }
        registerDeviceButton.layoutParams=buttonLayoutParam
        dialogueLinearLayout.addView(registerDeviceButton)

        dialogueObject.setView(dialogueLinearLayout)

        dialogueObject.show()
    }

    private fun getMacAddr(): String {
        try {
            val all = Collections.list(NetworkInterface.getNetworkInterfaces())
            for (nif in all) {
                if (!nif.getName().equals("wlan0", ignoreCase=true)) continue

                val macBytes = nif.getHardwareAddress() ?: return ""

                val res1 = StringBuilder()
                for (b in macBytes) {
                    //res1.append(Integer.toHexString(b & 0xFF) + ":");
                    res1.append(String.format("%02X:", b))
                }

                if (res1.length > 0) {
                    res1.deleteCharAt(res1.length - 1)
                }
                return res1.toString()
            }
        } catch (ex: Exception) {
        }

        return "02:00:00:00:00:00"
    }

    private fun queryMACAddress(){

        val macAddress = getMacAddr()
        Log.i(TAG, "Manual Log, queryMACAddress, address: $macAddress")

        // To retrieve book data
        val url = "https://ivan-chew.outsystemscloud.com/Chew_Database/rest/RestAPI/Query_MAC_Address?MAC_Address=$macAddress"

        val jsonObjectRequest = JsonObjectRequest(Request.Method.POST, url, null,
            Response.Listener { response ->
                Log.i(TAG, "Manual Log, queryMACAddress, response: $response")
                val responseStatus = response.getString("Status")
                if (responseStatus=="501"){
                    Log.i(TAG, "Manual Log, queryMACAddress, unregistered device")
                    promptUserLogin()
                }else if (responseStatus=="502"){
                    Log.e(TAG, "Manual Log, queryMACAddress, error: database error")
                    //TODO Initiate block
                }else if (responseStatus=="ok"){
                    Log.i(TAG, "Manual Log, queryMACAddress, ok: existing user")
                    val userName = response.getString("Username")
                    UserManagement.setUserName(userName)
                    Log.i(TAG, "Manual Log, queryMACAddress, userManagement class initiated: " + UserManagement.params["username"].toString())
                    createWelcomeMessage(userName)
                }else{
                    Log.e(TAG, "Manual Log, queryMACAddress, error: unknown response")
                    //TODO Initiate block
                }
            },
            Response.ErrorListener { error ->
                Log.e(TAG, "Manual Log $error")
            })
        VolleySingleton.getInstance(context!!).addToRequestQueue(jsonObjectRequest)
    }

    private fun queryCreateNewUser(username: String, name: String, password: String, email: String, mobile:String){
        Log.i(TAG, "Manual Log, queryCreateNewUser, called")
        val params = HashMap<String,String>()
        params["Username"] = username
        params["Name"] = name
        params["Password"] = password
        params["Email"] = email
        params["Mobile_Number"] = mobile
        val jsonObject = JSONObject(params as Map<*, *>)
        val url = "https://ivan-chew.outsystemscloud.com/Chew_Database/rest/RestAPI/Create_New_Workshop_User"
        val jsonObjectRequest = JsonObjectRequest(Request.Method.POST, url, jsonObject,
            Response.Listener { response ->
                Log.i(TAG, "Manual Log, queryCreateNewUser, response: $response")
                if (response.getString("status")!="ok"){
                    Toast.makeText(context, response.getString("status"), Toast.LENGTH_SHORT).show()
                    createRegistrationButtons()
                } else {
                    Toast.makeText(context, "New user created", Toast.LENGTH_SHORT).show()
                    queryCreateNewDeviceRegistration(username, password)
                }
            },
            Response.ErrorListener { error ->
                Log.e(TAG, "Manual Log, queryObjectTime, $error")
            })
        VolleySingleton.getInstance(context!!).addToRequestQueue(jsonObjectRequest)
    }

    private fun queryCreateNewDeviceRegistration(username: String, password: String){
        Log.i(TAG, "Manual Log, queryCreateNewDeviceRegistration, called")
        val params = HashMap<String,String>()
        params["Username"] = username
        params["Password"] = password
        params["MAC"] = getMacAddr()
        val jsonObject = JSONObject(params as Map<*, *>)
        val url = "https://ivan-chew.outsystemscloud.com/Chew_Database/rest/RestAPI/Create_New_Device_Register"
        val jsonObjectRequest = JsonObjectRequest(Request.Method.POST, url, jsonObject,
            Response.Listener { response ->
                Log.i(TAG, "Manual Log, queryCreateNewDeviceRegistration, response: $response")
                if (response.getString("status")!="ok"){
                    Toast.makeText(context, response.getString("status"), Toast.LENGTH_SHORT).show()
                    createRegistrationButtons()
                } else {
                    Toast.makeText(context, "Device Registered", Toast.LENGTH_SHORT).show()
                    createWelcomeMessage(username)
                }
            },
            Response.ErrorListener { error ->
                Log.e(TAG, "Manual Log, queryObjectTime, $error")
            })
        VolleySingleton.getInstance(context!!).addToRequestQueue(jsonObjectRequest)
    }

    private fun queryObjectDates(objectId: String){
        Log.i(TAG, "Manual Log, query object id: $objectId")

        //show loading screen
        val progressBar = ProgressBar(context)
        val progressBarAlertBuilder = AlertDialog.Builder(context)
        progressBarAlertBuilder.setTitle("Loading Available Dates")
        progressBarAlertBuilder.setView(progressBar)
        val progressBarAlert = progressBarAlertBuilder.create()
        progressBarAlert.show()

        //Set active workshop id
        ACTIVE_WORKSHOP_ID = objectId

        // To retrieve book data
        val url = "https://ivan-chew.outsystemscloud.com/Chew_Database/rest/RestAPI/Query_Workshop_Dates?workshop_id=$ACTIVE_WORKSHOP_ID"

        val jsonArrayRequest = JsonArrayRequest(Request.Method.POST, url, null,
            Response.Listener { response ->
                val responseLength = response.length()
                Log.i(TAG, "Manual Log, response JSON array length: $responseLength")
                if (responseLength==0){
                    Toast.makeText(context, "No available dates", Toast.LENGTH_SHORT).show()
                } else {
                    showCalendar(response)
                }
                progressBarAlert.dismiss()
            },
            Response.ErrorListener { error ->
                Log.e(TAG, "Manual Log $error")
                progressBarAlert.dismiss()
            })
        VolleySingleton.getInstance(context!!).addToRequestQueue(jsonArrayRequest)
    }

    private fun queryObjectTime(dateQuery: String){
        // Instantiate the RequestQueue.
        Log.i(TAG, "Manual Log, query object time: $dateQuery")

        // To retrieve book data
        val url =
            "https://ivan-chew.outsystemscloud.com/Chew_Database/rest/RestAPI/Query_Workshop_Time?workshop_id=$ACTIVE_WORKSHOP_ID&date=$dateQuery"

        val jsonArrayRequest = JsonArrayRequest(Request.Method.POST, url, null,
            Response.Listener { response ->
                val responseLength = response.length()
                Log.i(TAG, "Manual Log, response JSON array length: $responseLength, response: $response")
                if (responseLength==0){
                    Toast.makeText(context, "No available slots", Toast.LENGTH_SHORT).show()
                } else {
                    showTimeOptions(response)
                }
            },
            Response.ErrorListener { error ->
                Log.e(TAG, "Manual Log $error")
            })
        VolleySingleton.getInstance(context!!).addToRequestQueue(jsonArrayRequest)
    }

    private fun queryObjectBooking(objectId: Int, bookingQuantity: Int, bookingUsername: String,
                                   bookingContactNumber: String, bookingEmail: String){
        Log.i(TAG, "Manual Log, query object id: $objectId")

        // To retrieve book data
        val url = "https://ivan-chew.outsystemscloud.com/Chew_Database/rest/RestAPI/" +
                "Create_Workshop_Booking?" +
                "Workshop_date_id=$objectId&" +
                "Quantity=$bookingQuantity&" +
                "Username=$bookingUsername" +
                "&Contact_Number=$bookingContactNumber&" +
                "Email=$bookingEmail"

        val stringRequest = StringRequest(Request.Method.GET, url,
            Response.Listener<String> { response ->
                Log.i(TAG, "Manual Log, queryObjectTime, response JSON array length: $response")
                if (response!="ok"){
                    Toast.makeText(context, "Error in response", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Booking Completed", Toast.LENGTH_SHORT).show()
                }
            },
            Response.ErrorListener { error ->
                Log.e(TAG, "Manual Log, queryObjectTime, $error")
            })
        VolleySingleton.getInstance(context!!).addToRequestQueue(stringRequest)
    }

    private fun queryWorkshopList(){
        //Show progress bar
        Log.i(TAG, "Manual Log, queryWorkshopList, called")
        val loadingBar = view!!.findViewById<ProgressBar>(R.id.progressBar1)
        loadingBar.isVisible = true

        val cardLayout = view!!.findViewById<LinearLayout>(R.id.object_List_Linear_Layout)
        val viewWidth = cardLayout.width
        val viewLength = viewWidth * 224 / 400

        // To retrieve book data
        val url = "https://ivan-chew.outsystemscloud.com/Chew_Database/rest/RestAPI/Get_Available_Workshops"

        val jsonArrayRequest = JsonArrayRequest(Request.Method.POST, url, null,
            Response.Listener { response ->

                // class Book(val bookTitle: String, val publisher: String, val bookAuthor: String)
                val responseLength = response.length()
                Log.i(TAG, "Manual Log, total data: $responseLength raw: $response")

                //Create this array to store each textview's ID
                for (i in 0 until responseLength){

                    //Obtain a single book object from the response
                    val workshopObject = response.getJSONObject(i)

                    //Retrieve the book title from the object
                    val workshopName = workshopObject.getString("Workshop_Name")
                    val workshopType = workshopObject.getString("Workshop_Type")
                    val workshopCost = workshopObject.getDouble("Workshop_Cost")
                    val workshopLength = workshopObject.getDouble("Workshop_Length")
                    val workshopDescription = workshopObject.getString("Workshop_Description")
                    val workshopID = workshopObject.getString("Id")

                    //Retrieve the thumbnail AND create card
                    try {
                        //Decoding image from REST response
                        val picData = workshopObject.getString("Workshop_Cover")
                        val picByteArray = Base64.decode(picData,Base64.DEFAULT)
                        val decodeByte = ByteArrayInputStream(picByteArray)
                        val workshopThumbnail = BitmapFactory.decodeStream(decodeByte)
                        val reSizedWorkshopThumbnail = resizeBitmap(workshopThumbnail, viewWidth, viewLength)

                        createWorkshopCard(workshopName,workshopType,workshopCost,workshopLength, workshopDescription, reSizedWorkshopThumbnail,workshopID)

                        Log.i(TAG, "Manual Log, image creation card $i")
                    } catch (e: Exception){
                        Log.i(TAG, "Manual Log, image creation error: " + e.message)
                    }
                }

                loadingBar.isVisible = false
            },
            Response.ErrorListener { error ->
                Log.e(TAG, "Manual Log $error")
            }
        )

        // Add the request to the RequestQueue.
        VolleySingleton.getInstance(context!!).addToRequestQueue(jsonArrayRequest)
    }

    @SuppressLint("SimpleDateFormat")
    private fun showCalendar(availableSlots: JSONArray){

        val calendarDialogue = AlertDialog.Builder(context)
        calendarDialogue.setTitle("Available dates")

        //Create the calendarView object, choose the correct view to create
        val objectCalendar = CalendarView(context!!)

        //Create an array of eventDay objects that will be passed into the view
        val availableEventDay: MutableList<EventDay> = ArrayList()
        val dataCalendar: MutableList<Calendar> = ArrayList()
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'")

        //Specify the dates to annotate on the calendar view
        for (i in 0 until availableSlots.length()) {
            val slotObject = availableSlots.getJSONObject(i)
            val slotDateTimeString = slotObject.getString("Date_Time")
            Log.i(TAG, "Manual Log, Date: $slotDateTimeString")
            val slotDate = sdf.parse(slotDateTimeString)
            dataCalendar.add(Calendar.getInstance())
            dataCalendar[i].time = slotDate!!
            availableEventDay.add(EventDay(dataCalendar[i], R.drawable.ic_dot))
        }
        objectCalendar.setEvents(availableEventDay)
        objectCalendar.setOnDayClickListener(this)

        //Modifying the calendarView display
        objectCalendar.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT)

        //Display the calendar
        calendarDialogue.setView(objectCalendar)
        calendarDialogue.setNeutralButton("Cancel"){_,_ ->
            Toast.makeText(context,"You cancelled the dialog.",Toast.LENGTH_SHORT).show()
        }
        calendarDialogue.show()
    }

    @SuppressLint("SetTextI18n")
    private fun showTimeOptions(availableSlots: JSONArray){
        Log.i(TAG, "Manual Log, showTimeOptions, $availableSlots")

        //Headers
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'")

        //Build dialogue options
        val selectTimeDialogue = AlertDialog.Builder(context)
        selectTimeDialogue.setTitle("Select Time Slot")
        val dialogueLinearLayout = LinearLayout(context)
        dialogueLinearLayout.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT)
        dialogueLinearLayout.orientation = LinearLayout.VERTICAL

        //-Specify time options
        val selectTimeRadioButton = Array(availableSlots.length()){RadioButton(context)}
        val selectTimeRadioGroup = RadioGroup(context)
        val RadioButtonParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT)
        RadioButtonParams.setMargins(50, 0, 0, 0)
        for (i in 0 until availableSlots.length()){
            val slotObject = availableSlots.getJSONObject(i)
            val slotDateTimeString = slotObject.getString("Date_Time")
            var slotAvailable = 0

            val slotObjectId = slotObject.getInt("Id")
            try {
                slotAvailable = slotObject.getInt("Slots_Available")
            } catch (e: Exception){
                Log.i(TAG, "Manual Log, showTimeOptions, JSON error does not have the property: Slots_Available ")
            }

            Log.i(TAG, "Manual Log, showTimeOptions, radio button creation id: $slotObjectId")
            val slotDate = sdf.parse(slotDateTimeString)

            var hourString = ""
            var minuteString = ""
            if (slotDate.hours + GMT_STANDARD < 10) {
                hourString = "0" + (slotDate.hours + GMT_STANDARD).toString()
            } else {
                hourString = (slotDate.hours + GMT_STANDARD).toString()
            }
            if (slotDate.minutes < 10){
                minuteString = "0" + slotDate.minutes.toString()
            }else{
                minuteString = slotDate.minutes.toString()
            }

            selectTimeRadioButton[i].text = "$hourString : $minuteString     (Slots: $slotAvailable)"
            selectTimeRadioButton[i].textSize = 20.0F
            selectTimeRadioButton[i].id = slotObjectId
            selectTimeRadioButton[i].layoutParams = RadioButtonParams
            selectTimeRadioGroup.addView(selectTimeRadioButton[i])
        }

        //-Specify input options
        val editTextArray = Array(4){EditText(context)}
        val editTextParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT)
        editTextParams.setMargins(50, 0, 100, 0)
        editTextArray[0].setHint("Qty")
        editTextArray[1].setHint("Username")
        editTextArray[2].setHint("Contact Number")
        editTextArray[3].setHint("Email")

        //TEST: Temporary testing values
        editTextArray[0].setText("3")
        editTextArray[1].setText(UserManagement.params["username"])
        editTextArray[2].setText("94357250")
        editTextArray[3].setText("ivanchew@pickmeet.com")

        /*
        selectTimeRadioGroup.setOnCheckedChangeListener { group, checkedId ->
            Log.i(TAG, "Manual Log, showTimeOptions, radio button selected: $checkedId")
        }
        */

        dialogueLinearLayout.addView(selectTimeRadioGroup)
        for (i in editTextArray.indices){
            editTextArray[i].layoutParams = editTextParams
            dialogueLinearLayout.addView(editTextArray[i])
        }
        selectTimeDialogue.setView(dialogueLinearLayout)
        selectTimeDialogue.setNeutralButton("Cancel"){_,_ ->
            Toast.makeText(context,"You cancelled the dialog.",Toast.LENGTH_SHORT).show()
        }
        selectTimeDialogue.setPositiveButton("Book"){ _, _ ->
            Log.i(TAG, "Manual Log, showTimeoptions, booking button: " + selectTimeRadioGroup.checkedRadioButtonId)
            queryObjectBooking(selectTimeRadioGroup.checkedRadioButtonId,editTextArray[0].text.toString().toInt(),
                editTextArray[1].text.toString(),
                editTextArray[2].text.toString(),
                editTextArray[3].text.toString())
        }
        selectTimeDialogue.show()
    }

    private fun resizeBitmap(bitmap:Bitmap, width:Int, height:Int):Bitmap{
        return Bitmap.createScaledBitmap(bitmap, width, height,false)
    }

    // Listeners and callback for receiving speech and picture
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode){


            //Moved to main activity, not in use. For image retrieval
            /*
            REQUEST_IMAGE_CAPTURE -> {
                Log.i(TAG, "Manual Log, received image capture response")
                if (resultCode == RESULT_OK){
                    val imageBitmap = data?.extras?.get("data") as Bitmap
                    Log.i(TAG, "Manual Log, processing image to string")
                    val byteStream = ByteArrayOutputStream()
                    imageBitmap.compress(Bitmap.CompressFormat.PNG,90,byteStream)
                    val byteArray = byteStream.toByteArray()
                    val imageData = Base64.encodeToString(byteArray, Base64.DEFAULT)
                    createNewObject(imageData)
                }
            }
            */

            //For speech retrieval
            REQUEST_CODE_SPEECH_INPUT -> {
                if (resultCode== Activity.RESULT_OK && null != data){

                    //get text from result
                    val result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                    Log.i(TAG, "Manual Log, codeSpeechInputResult, " + result[0])

                }
            }
        }
    }

    override fun onDayClick(eventDay: EventDay) {
        queryObjectTime(eventDay.calendar.get(YEAR).toString()
                + "-" + (eventDay.calendar.get(MONTH)+1).toString()
                + "-" + eventDay.calendar.get(DATE).toString())
    }

}