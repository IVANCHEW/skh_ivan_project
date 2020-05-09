package com.example.skh_ivan_app

import android.app.Activity
import android.app.AlertDialog
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.wifi.WifiManager
import android.os.Bundle
import android.provider.MediaStore
import android.text.InputType
import android.util.Base64
import android.util.Log
import com.google.android.material.snackbar.Snackbar
import androidx.appcompat.app.AppCompatActivity
import android.view.Menu
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.navigation.findNavController
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import kotlinx.android.synthetic.main.activity_main.*
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.HashMap

class MainActivity : AppCompatActivity(){

    private val newObjectParams = HashMap<String,String>()
    private val REQUEST_IMAGE_CAPTURE = 1
    private val TAG = "MANUAL"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        fab.setOnClickListener { view ->
            queryObjectCreateNew()
            Snackbar.make(view, "Create new workshop!", Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show()
        }

        fab_see_bookings.setOnClickListener { view ->
            findNavController(R.id.nav_host_fragment)
                .navigate(R.id.action_FirstFragment_to_SecondFragment)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    private fun queryObjectCreateNew(){
        Log.i(ContentValues.TAG, "Manual Log, queryObjectCreateNew, called")
        //Construct the alert dialogue
        val newObjectDialogue = AlertDialog.Builder(this)
        newObjectDialogue.setTitle("Create New Workshop")

        val dialogueLinearLayout = LinearLayout(this)
        dialogueLinearLayout.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT)
        dialogueLinearLayout.orientation = LinearLayout.VERTICAL
        val newObjectTextInput = Array(4){ EditText(this) }
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
            Toast.makeText(this,"Please take a picture of the cover.", Toast.LENGTH_SHORT).show()
            newObjectParams["Workshop_Name"] = newObjectTextInput[0].text.toString()
            newObjectParams["Workshop_Cost"] = newObjectTextInput[1].text.toString()
            newObjectParams["Workshop_Length"] = newObjectTextInput[2].text.toString()
            newObjectParams["Workshop_Description"] = newObjectTextInput[3].text.toString()
            // Capture cover
            dispatchTakePictureIntent()
        }

        newObjectDialogue.setNeutralButton("Cancel"){_,_ ->
            Toast.makeText(this,"You cancelled the dialog.", Toast.LENGTH_SHORT).show()
        }

        newObjectDialogue.show()
    }

    private fun dispatchTakePictureIntent() {
        val pm = this.packageManager
        Log.i(ContentValues.TAG, "Manual Log, Camera function called")
        Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takePictureIntent ->
            Log.i(ContentValues.TAG, "Manual Log, start intent")
            takePictureIntent.resolveActivity(pm)?.also {
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode){

            //For image retrieval
            REQUEST_IMAGE_CAPTURE -> {
                Log.i(ContentValues.TAG, "Manual Log, received image capture response")
                if (resultCode == Activity.RESULT_OK){
                    val imageBitmap = data?.extras?.get("data") as Bitmap
                    Log.i(ContentValues.TAG, "Manual Log, processing image to string")
                    val byteStream = ByteArrayOutputStream()
                    imageBitmap.compress(Bitmap.CompressFormat.PNG,90,byteStream)
                    val byteArray = byteStream.toByteArray()
                    val imageData = Base64.encodeToString(byteArray, Base64.DEFAULT)
                    createNewObject(imageData)
                }
            }

        }
    }

    private fun createNewObject(imageString: String){

        Log.i(ContentValues.TAG, "Manual Log, book submission function called")

        val url = "https://ivan-chew.outsystemscloud.com/Chew_Database/rest/RestAPI/Create_New_Workshop"

        newObjectParams["Workshop_Cover"] = imageString

        val jsonObject = JSONObject(newObjectParams as Map<*, *>)

        Log.i(ContentValues.TAG, "Manual Log, request submitted: $url")

        val request = JsonObjectRequest(
            Request.Method.POST, url, jsonObject,
            Response.Listener { response ->
                Log.i(ContentValues.TAG, "Manual Log $response")
            },
            Response.ErrorListener { error ->
                Log.i(ContentValues.TAG, "Manual Log: $error")
            })

        VolleySingleton.getInstance(this).addToRequestQueue(request)
    }

}
