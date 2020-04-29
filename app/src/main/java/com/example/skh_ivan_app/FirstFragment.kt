package com.example.skh_ivan_app

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Activity.RESULT_OK
import android.app.AlertDialog
import android.content.ContentValues.TAG
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Typeface
import android.media.Image
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.speech.RecognizerIntent
import android.text.InputType
import android.util.Base64
import android.util.Log
import android.util.TypedValue
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.cardview.widget.CardView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.view.isVisible
import androidx.core.view.marginBottom
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.JsonArrayRequest
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.applandeo.materialcalendarview.CalendarView
import com.applandeo.materialcalendarview.EventDay
import org.json.JSONArray
import org.json.JSONObject
import org.w3c.dom.Text
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.lang.Exception
import java.lang.String.format
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
class FirstFragment : Fragment() {

    private val REQUEST_CODE_SPEECH_INPUT = 100
    private val REQUEST_IMAGE_CAPTURE = 1
    private val YEAR_CONSTANT = 1900
    val bookParams = HashMap<String,String>()

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

        //Button to initiate speaker functions
        view.findViewById<Button>(R.id.button_speak).setOnClickListener{
            speak()
        }

        //Button to take picture
        view.findViewById<Button>(R.id.button_camera).setOnClickListener {
            //dispatchTakePictureIntent()
        }

        //Button to initiate web service functions
        view.findViewById<Button>(R.id.button_rest_api).setOnClickListener {

            //Show progress bar
            val loadingBar = view.findViewById<ProgressBar>(R.id.progressBar1)
            loadingBar.isVisible = true

            val textview_output = view.findViewById<TextView>(R.id.textview_debug)

            // Instantiate the RequestQueue.
            val queue = Volley.newRequestQueue(context)

            // To retrieve book data
            val url = "https://ivan-chew.outsystemscloud.com/Chew_Database/rest/RestAPI/Get_Book_List"

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
                        val bookObject = response.getJSONObject(i)

                        //Retrieve the book title from the object
                        val bookTitle = bookObject.getString("Book_Title")
                        val bookAuthor = bookObject.getString("Book_Author")
                        val bookPublisher = bookObject.getString("Publisher")
                        val bookId = bookObject.getString("Id")

                        //Retrieve the thumbnail AND create card
                        try {
                            //Decoding image from REST response
                            val picData = bookObject.getString("Cover")
                            val picByteArray = Base64.decode(picData,Base64.DEFAULT)
                            val decodeByte = ByteArrayInputStream(picByteArray)
                            val bookThumbnail = BitmapFactory.decodeStream(decodeByte)
                            val reSizedBookThumbnail = resizeBitmap(bookThumbnail, 200, 300)

                            createBookCard(bookTitle,bookAuthor,bookPublisher,reSizedBookThumbnail,bookId)

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

        //Button to create new book with prompt
        view.findViewById<Button>(R.id.button_new_book).setOnClickListener {

            //Construct the alert dialogue
            val newBookDialogue = AlertDialog.Builder(context)
            newBookDialogue.setTitle("Create New Book")

            val DialogueLinearLayout = LinearLayout(context)
            DialogueLinearLayout.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT)
            DialogueLinearLayout.orientation = LinearLayout.VERTICAL
            val newbookTextInput = Array(3){EditText(context)}
            newbookTextInput[0].setHint("Book title")
            newbookTextInput[1].setHint("Author")
            newbookTextInput[2].setHint("Publisher")

            for (i in newbookTextInput.indices){
                newbookTextInput[i].inputType = InputType.TYPE_CLASS_TEXT
                DialogueLinearLayout.addView(newbookTextInput[i])
            }

            newBookDialogue.setView(DialogueLinearLayout)

            newBookDialogue.setPositiveButton("Take Cover Picture"){ _, _ ->
                // Do something when user press the positive button
                Toast.makeText(context,"Please take a picture of the cover.",Toast.LENGTH_SHORT).show()
                bookParams["book_Title"] = newbookTextInput[0].text.toString()
                bookParams["book_Author"] = newbookTextInput[1].text.toString()
                bookParams["Publisher"] = newbookTextInput[2].text.toString()
                // Capture cover
                dispatchTakePictureIntent()
            }

            newBookDialogue.setNeutralButton("Cancel"){_,_ ->
                Toast.makeText(context,"You cancelled the dialog.",Toast.LENGTH_SHORT).show()
            }

            newBookDialogue.show()
        }
    }

    // Create a card view
    @SuppressLint("SetTextI18n")
    private fun createBookCard(bookTitle: String, bookAuthor: String, bookPublisher:String, bookCover:Bitmap, bookId:String){
        Log.i(TAG, "Manual Log, create book card")
        //Headers
        val bookLinearLayout = view!!.findViewById<LinearLayout>(R.id.book_Linear_Layout)

        // Create the parent layout
        val cardLayout = LinearLayout(context)
        cardLayout.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT)
        cardLayout.orientation = LinearLayout.HORIZONTAL

        // Add the book cover thumbnail
        val bookImageView = ImageView(context)
        bookImageView.setImageBitmap(bookCover)
        val imageViewLayoutParam = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT)
        imageViewLayoutParam.setMargins(20, 20, 0, 0)
        bookImageView.layoutParams = imageViewLayoutParam
        cardLayout.addView(bookImageView)

        // Prepare the text
        val textLayout = LinearLayout(context)
        textLayout.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT)
        textLayout.orientation = LinearLayout.VERTICAL

        val textViewArray = Array(3){TextView(context)}
        val textviewLayoutParam = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT)
        textviewLayoutParam.setMargins(10, 10, 0, 0)

        textViewArray[0].text = bookTitle
        textViewArray[1].text = "By $bookAuthor"
        textViewArray[2].text = "Publisher: $bookPublisher"

        for (i in textViewArray.indices) {
            textViewArray[i].layoutParams = textviewLayoutParam
            textLayout.addView(textViewArray[i])
        }

        //Prepare Button
        val rentButton = Button(context)
        rentButton.text = "Rent"
        rentButton.setTextColor(Color.WHITE)
        rentButton.setBackgroundColor(Color.parseColor("#15ace8"))
        rentButton.layoutParams = textviewLayoutParam
        rentButton.setOnClickListener {
            queryBookRentDates(bookId)
        }
        textLayout.addView(rentButton)

        //Add the generated card layout to the existing layout
        cardLayout.addView(textLayout)
        bookLinearLayout.addView(cardLayout)
    }

    private fun queryBookRentDates(bookId: String){
        Log.i(TAG, "Manual Log, query book rent dates with book id: $bookId")

        // Instantiate the RequestQueue.
        val queue = Volley.newRequestQueue(context)

        // To retrieve book data
        val url = "https://ivan-chew.outsystemscloud.com/Chew_Database/rest/RestAPI/Query_Rent_Dates?book_id=$bookId"

        val jsonArrayRequest = JsonArrayRequest(Request.Method.POST, url, null,
            Response.Listener { response ->
                val responseLength = response.length()
                Log.i(TAG, "Manual Log, rent dates query responded with $responseLength")
                if (responseLength==0){
                    Toast.makeText(context, "No rental available", Toast.LENGTH_SHORT).show()
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

    private fun showCalendar(availableSlots: JSONArray){

        val bookDetailsLayout = view!!.findViewById<LinearLayout>(R.id.book_Details_Layout)

        //Create the calendarView object, choose the correct view to create
        val bookCalendar = CalendarView(context!!)

        //Create an array of eventDay objects that will be passed into the view
        val availableEventDay: MutableList<EventDay> = ArrayList()
        val dataCalendar: MutableList<Calendar> = ArrayList()
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'")

        for (i in 0 until availableSlots.length()) {
            val slotObject = availableSlots.getJSONObject(i)
            val slotDateTimeString = slotObject.getString("Date_Time")
            Log.i(TAG, "Manual Log, Date: $slotDateTimeString")
            val slotDate = sdf.parse(slotDateTimeString)
            dataCalendar.add(Calendar.getInstance())
            dataCalendar[i].set(slotDate.year + YEAR_CONSTANT, slotDate.month, slotDate.date)
            availableEventDay.add(EventDay(dataCalendar[i], R.drawable.ic_dot))
        }

        //Modifying the calendarView object
        bookCalendar.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT)
        bookCalendar.setEvents(availableEventDay)

        //What does highlight days do?
        bookCalendar.setHighlightedDays(dataCalendar)
        bookDetailsLayout.addView(bookCalendar)

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

    // To obtain voice input and perform text to voice recognition
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

    // To create a new book data
    private fun submitBookDetails(bookCover: String){

        val queue = Volley.newRequestQueue(context)
        Log.i(TAG, "Manual Log, book submission function called")

        val url = "https://ivan-chew.outsystemscloud.com/Chew_Database/rest/RestAPI/Create_Book"

        //Submit through body
        val params = HashMap<String,String>()
        params["book_Title"] = bookParams["book_Title"].toString()
        params["book_Author"] = bookParams["book_Author"].toString()
        params["Publisher"] = bookParams["Publisher"].toString()
        params["Cover"] = bookCover
        val jsonObject = JSONObject(params as Map<*, *>)

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
                    submitBookDetails(imageData)
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

}