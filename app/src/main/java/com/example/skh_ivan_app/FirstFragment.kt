package com.example.skh_ivan_app

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Activity.RESULT_OK
import android.app.AlertDialog
import android.content.ContentValues.TAG
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
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
import com.android.volley.toolbox.Volley
import com.applandeo.materialcalendarview.CalendarView
import com.applandeo.materialcalendarview.EventDay
import com.applandeo.materialcalendarview.listeners.OnDayClickListener
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.lang.Exception
import java.text.SimpleDateFormat
import java.util.*
import java.util.Calendar.*
import kotlin.collections.ArrayList

/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
class FirstFragment : Fragment(), OnDayClickListener {

    private val REQUEST_CODE_SPEECH_INPUT = 100
    private val REQUEST_IMAGE_CAPTURE = 1
    private val YEAR_CONSTANT = 1900
    private val newObjectParams = HashMap<String,String>()
    private var ACTIVE_WORKSHOP_ID = ""
    private val GMT_STANDARD = 8

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

        super.onViewCreated(view, savedInstanceState)

        //Button to create new object with prompt
        view.findViewById<Button>(R.id.button_pull_data).setOnClickListener {
            queryWorkshopList()
        }
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

    private fun queryObjectDates(objectId: String){
        Log.i(TAG, "Manual Log, query object id: $objectId")

        //Set active workshop id
        ACTIVE_WORKSHOP_ID = objectId

        // Instantiate the RequestQueue.
        val queue = Volley.newRequestQueue(context)

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
            },
            Response.ErrorListener { error ->
                Log.e(TAG, "Manual Log $error")
                queue.stop()
            })

        queue.add(jsonArrayRequest)
    }

    private fun queryObjectTime(dateQuery: String){
        // Instantiate the RequestQueue.
        Log.i(TAG, "Manual Log, query object time: $dateQuery")
        val queue = Volley.newRequestQueue(context)

        // To retrieve book data
        val url =
            "https://ivan-chew.outsystemscloud.com/Chew_Database/rest/RestAPI/Query_Workshop_Time?workshop_id=$ACTIVE_WORKSHOP_ID&date=$dateQuery"

        val jsonArrayRequest = JsonArrayRequest(Request.Method.POST, url, null,
            Response.Listener { response ->
                val responseLength = response.length()
                Log.i(TAG, "Manual Log, response JSON array length: $responseLength")
                if (responseLength==0){
                    Toast.makeText(context, "No available slots", Toast.LENGTH_SHORT).show()
                } else {
                    showTimeOptions(response)
                }
            },
            Response.ErrorListener { error ->
                Log.e(TAG, "Manual Log $error")
                queue.stop()
            })

        queue.add(jsonArrayRequest)
    }

    private fun queryObjectBooking(objectId: Int, bookingQuantity: Int, bookingUsername: String,
                                   bookingContactNumber: String, bookingEmail: String){
        Log.i(TAG, "Manual Log, query object id: $objectId")

        // Instantiate the RequestQueue.
        val queue = Volley.newRequestQueue(context)

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
                queue.stop()
            })

        queue.add(stringRequest)
    }

    private fun queryObjectCreateNew(){
        Log.i(TAG, "Manual Log, queryObjectCreateNew, called")
        //Construct the alert dialogue
        val newObjectDialogue = AlertDialog.Builder(context)
        newObjectDialogue.setTitle("Create New Workshop")

        val dialogueLinearLayout = LinearLayout(context)
        dialogueLinearLayout.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT)
        dialogueLinearLayout.orientation = LinearLayout.VERTICAL
        val newObjectTextInput = Array(4){EditText(context)}
        newObjectTextInput[0].hint = "Title"
        newObjectTextInput[1].hint = "Cost"
        newObjectTextInput[2].hint = "Length (hours)"
        newObjectTextInput[3].hint = "Description"

        for (i in newObjectTextInput.indices){
            newObjectTextInput[i].inputType = InputType.TYPE_CLASS_TEXT
            dialogueLinearLayout.addView(newObjectTextInput[i])
        }

        newObjectDialogue.setView(dialogueLinearLayout)

        newObjectDialogue.setPositiveButton("Take Cover Picture"){ _, _ ->
            // Do something when user press the positive button
            Toast.makeText(context,"Please take a picture of the cover.",Toast.LENGTH_SHORT).show()
            newObjectParams["Workshop_Name"] = newObjectTextInput[0].text.toString()
            newObjectParams["Workshop_Cost"] = newObjectTextInput[1].text.toString()
            newObjectParams["Workshop_Length"] = newObjectTextInput[2].text.toString()
            newObjectParams["Workshop_Description"] = newObjectTextInput[3].text.toString()
            // Capture cover
            dispatchTakePictureIntent()
        }

        newObjectDialogue.setNeutralButton("Cancel"){_,_ ->
            Toast.makeText(context,"You cancelled the dialog.",Toast.LENGTH_SHORT).show()
        }

        newObjectDialogue.show()
    }

    private fun queryWorkshopList(){
        //Show progress bar
        val loadingBar = view!!.findViewById<ProgressBar>(R.id.progressBar1)
        loadingBar.isVisible = true

        val textview_output = view!!.findViewById<TextView>(R.id.textview_debug)
        val cardLayout = view!!.findViewById<LinearLayout>(R.id.object_List_Linear_Layout)
        val viewWidth = cardLayout.width
        val viewLength = viewWidth * 224 / 400

        // Instantiate the RequestQueue.
        val queue = Volley.newRequestQueue(context)

        // To retrieve book data
        //val url = "https://ivan-chew.outsystemscloud.com/Chew_Database/rest/RestAPI/Get_Book_List"
        val url = "https://ivan-chew.outsystemscloud.com/Chew_Database/rest/RestAPI/Get_Available_Workshops"

        val jsonArrayRequest = JsonArrayRequest(Request.Method.POST, url, null,
            Response.Listener { response ->

                // class Book(val bookTitle: String, val publisher: String, val bookAuthor: String)
                textview_output.text = "Response received"
                val responseLength = response.length()
                Log.i(TAG, "Manual Log, total data: $responseLength raw: $response")
                queue.stop()

                //Create this array to store each textview's ID
                for (i in 0..(responseLength-1)){

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
                textview_output.text = "Error"
                Log.e(TAG, "Manual Log $error")
                queue.stop()
            }
        )

        // Add the request to the RequestQueue.
        queue.add(jsonArrayRequest)
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
        Log.i(TAG, "Manual Log, show time slots available")

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
            val slotObjectId = slotObject.getInt("Id")
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

            selectTimeRadioButton[i].text = "$hourString : $minuteString"
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
        editTextArray[1].setText("Ivan Chew")
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

    // To initiate camera to take picture
    private fun dispatchTakePictureIntent() {
        val pm = context!!.packageManager
        Log.i(TAG, "Manual Log, Camera function called")
        Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takePictureIntent ->
            Log.i(TAG, "Manual Log, start intent")
            takePictureIntent.resolveActivity(pm)?.also {
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)
            }
        }
    }

    // NOT IN USE To obtain voice input and perform text to voice recognition
    /*
    private fun speak(){
        Log.i(TAG, "Manual Log, Speak function called")
        val mIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        mIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        mIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
        mIntent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Hi speak something")
        Toast.makeText(context, "Start receiving speech", Toast.LENGTH_SHORT).show()
        try{
            // If there is no error in showing speechTotext dialogue
            startActivityForResult(mIntent, REQUEST_CODE_SPEECH_INPUT)
        } catch (e: Exception){
            // If there is error
            Toast.makeText(context, e.message, Toast.LENGTH_SHORT).show()
        }
    }
     */

    // To create a new book data
    private fun createNewObject(imageString: String){

        val queue = Volley.newRequestQueue(context)
        Log.i(TAG, "Manual Log, book submission function called")

        val url = "https://ivan-chew.outsystemscloud.com/Chew_Database/rest/RestAPI/Create_New_Workshop"

        newObjectParams["Workshop_Cover"] = imageString

        val jsonObject = JSONObject(newObjectParams as Map<*, *>)

        Log.i(TAG, "Manual Log, request submitted: $url")

        val request = JsonObjectRequest(
            Request.Method.POST, url, jsonObject,
            Response.Listener { response ->
                Log.i(TAG, "Manual Log $response")
            },
            Response.ErrorListener { error ->
                Log.i(TAG, "Manual Log: $error")
            })

        queue.add(request)
    }

    private fun resizeBitmap(bitmap:Bitmap, width:Int, height:Int):Bitmap{
        return Bitmap.createScaledBitmap(bitmap, width, height,false)
    }

    // Listeners and callback for receiving speech and picture
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        val outputText = view?.findViewById<TextView>(R.id.textview_debug)
        when (requestCode){

            //For image retrieval
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

            //For speech retrieval
            REQUEST_CODE_SPEECH_INPUT -> {
                if (resultCode== Activity.RESULT_OK && null != data){

                    //get text from result
                    val result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                    if (outputText != null) {
                        outputText.text = result[0]
                    }

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