package com.id.verification

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.InputType
import android.util.Log
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.onfido.android.sdk.capture.ExitCode
import com.onfido.android.sdk.capture.Onfido
import com.onfido.android.sdk.capture.OnfidoConfig
import com.onfido.android.sdk.capture.OnfidoFactory
import com.onfido.android.sdk.capture.ui.camera.face.stepbuilder.FaceCaptureStepBuilder
import com.onfido.android.sdk.capture.ui.options.FlowStep
import com.onfido.workflow.OnfidoWorkflow
import com.onfido.workflow.WorkflowConfig
import org.json.JSONObject

class MainActivity : AppCompatActivity() {

    private var client: Onfido? = null
    private lateinit var workflowLauncher: ActivityResultLauncher<Intent>
    private lateinit var onfidoConfig: OnfidoConfig
    private lateinit var onfidoWorkflow: OnfidoWorkflow
    private var applicantId: String? = null
    private var isClientCreated = false
    private var firstName: String? = null
    private var lastName: String? = null
    private var isVerificationComplete = false
    private var isDialogShown = false



    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        requestPermissions()
        setupWorkflowLauncher()
    }

    private fun showInputDialog() {
        if (isDialogShown) {return}
        isDialogShown = true

        val builder = AlertDialog.Builder(this)
        builder.setTitle("Enter Your Name")
        builder.setCancelable(false)

        val inputLayout = LinearLayout(this)
        inputLayout.orientation = LinearLayout.VERTICAL

        val firstNameInput = EditText(this).apply {
            hint = "First Name"
            maxLines = 1
            imeOptions = EditorInfo.IME_ACTION_NEXT
            inputType = InputType.TYPE_CLASS_TEXT
        }
        inputLayout.addView(firstNameInput)

        val lastNameInput = EditText(this).apply {
            hint = "Last Name"
            maxLines = 1
            imeOptions = EditorInfo.IME_ACTION_DONE
            inputType = InputType.TYPE_CLASS_TEXT
        }
        inputLayout.addView(lastNameInput)

        builder.setView(inputLayout)

        builder.setPositiveButton("OK") { dialog, _ ->
            firstName = firstNameInput.text.toString()
            lastName = lastNameInput.text.toString()
            dialog.dismiss()
            isDialogShown = false

            if(firstName!!.isNotEmpty() && lastName!!.isNotEmpty()) {
                initializeOnfidoClient()
            }
        }

        builder.show()
    }



    private fun setupWorkflowLauncher() {
        workflowLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                Log.d("Workflow", "Success")
            } else {
                showToast("Verification failed")
            }
        }
    }

    private fun initializeOnfidoClient() {
        isClientCreated = true
        client = OnfidoFactory.create(this).client
        createApplicant(firstName!!, lastName!!)
    }


    private fun createApplicant(firstName: String, lastName: String) {

        val queue = Volley.newRequestQueue(this)
        val url = "https://api.onfido.com/v3/applicants"
        val apiToken = getString(R.string.api_token)

        val request = object : StringRequest(Method.POST, url,
            Response.Listener { response ->
                val jsonResponse = JSONObject(response)
                applicantId = jsonResponse.getString("id")
                generateSdkToken(applicantId!!)
            },
            Response.ErrorListener { _ ->
                showToast("Error creating applicant.")
            }) {
            override fun getHeaders(): MutableMap<String, String> {
                return hashMapOf(
                    "Authorization" to "Token token=$apiToken",
                    "Content-Type" to "application/json"

                )
            }

            override fun getBody(): ByteArray {
                val jsonBody = JSONObject()
                    .put("first_name", firstName)
                    .put("last_name", lastName)
                return jsonBody.toString().toByteArray()
            }
        }

        queue.add(request)
    }

    private fun generateSdkToken(applicantId: String) {

        val queue = Volley.newRequestQueue(this)
        val url = "https://api.onfido.com/v3/sdk_token"
        val apiToken = getString(R.string.api_token)

        val request = object : StringRequest(Method.POST, url,
            Response.Listener { response ->
                val jsonResponse = JSONObject(response)
                val sdkToken = jsonResponse.getString("token")

//                getApplicantConsents(applicantId)
                startWorkflowRun(applicantId, sdkToken)
            },
            Response.ErrorListener { _ ->
                showToast("Try again later")
            }) {
            override fun getHeaders(): MutableMap<String, String> {
                return hashMapOf(
                    "Authorization" to "Token token=$apiToken",
                    "Content-Type" to "application/json"
                )
            }

            override fun getBody(): ByteArray {
                val jsonBody = JSONObject()
                    .put("applicant_id", applicantId)
                    .put("application_id", packageName)
                return jsonBody.toString().toByteArray()
            }
        }

        queue.add(request)
    }

    private fun getApplicantConsents(applicantId: String) {
        val queue = Volley.newRequestQueue(this)
        val url = "https://api.onfido.com/v3/applicants/$applicantId/consents"
        val apiToken = getString(R.string.api_token)

        val request = object : StringRequest(Method.GET, url,
            Response.Listener { response ->
                val jsonResponse = JSONObject(response)
                Log.e("On fido", "Consents retrieved: $jsonResponse")
            },
            Response.ErrorListener { error ->
                Log.e("On fido", "Error retrieving consents: ${error.message}")
                error.networkResponse?.let { networkResponse ->
                    Log.e("On fido", "Response code: ${networkResponse.statusCode}")
                    Log.e("On fido", "Response body: ${String(networkResponse.data)}")
                }
            }) {
            override fun getHeaders(): MutableMap<String, String> {
                return hashMapOf(
                    "Authorization" to "Token token=$apiToken",
                    "Content-Type" to "application/json"
                )
            }
        }

        queue.add(request)
    }


    private fun startWorkflowRun(applicantId: String, sdkToken: String) {
        val queue = Volley.newRequestQueue(this)
        val url = "https://api.eu.onfido.com/v3.6/workflow_runs"
        val apiToken = getString(R.string.api_token)

        val requestBody = JSONObject()
            .put("workflow_id", getString(R.string.workflow))
            .put("applicant_id", applicantId)

        val request = object : StringRequest(Method.POST, url,
            Response.Listener { response ->
                val jsonResponse = JSONObject(response)
                val workflowRunId = jsonResponse.getString("id")
                startOnfidoFlow(sdkToken, workflowRunId)
            },
            Response.ErrorListener { _ ->
                showToast("Try again later")
            }) {
            override fun getHeaders(): MutableMap<String, String> {
                return hashMapOf(
                    "Authorization" to "Token token=$apiToken",
                    "Content-Type" to "application/json"
                )
            }

            override fun getBody(): ByteArray {
                return requestBody.toString().toByteArray()
            }
        }

        queue.add(request)
    }

    private fun startOnfidoFlow(sdkToken: String, workflowRunId: String) {

        val workflowConfig = WorkflowConfig.Builder(
            workflowRunId = workflowRunId,
            sdkToken = sdkToken
        ).build()

        onfidoWorkflow = OnfidoWorkflow.create(this@MainActivity)
        startActivityForResult(onfidoWorkflow.createIntent(workflowConfig), REQUEST_CODE)
//        workflowLauncher.launch(onfidoWorkflow.createIntent(workflowConfig))

        startFlow(sdkToken)
    }



    private fun startFlow(sdkToken: String) {
        client?.let { onfidoClient ->
            val flowSteps = arrayOf(
//                FlowStep.WELCOME,
                FlowStep.CAPTURE_DOCUMENT,
                FlowStep.CAPTURE_FACE,
                FlowStep.FINAL,
            )
            try {
                onfidoConfig = OnfidoConfig.builder(this@MainActivity)
                    .withSDKToken(sdkToken, tokenExpirationHandler = ExpirationHandler())
                    .withCustomFlow(flowSteps)
                    .build()


                onfidoClient.createIntent(onfidoConfig)
//                workflowLauncher.launch(intent)

            } catch (e: Exception) {
                showToast("Something went wrong")
            }
        } ?:  showToast("Try again later")
    }

    @Deprecated("This method has been deprecated in favor of using the Activity Result API\n      which brings increased type safety via an {@link ActivityResultContract} and the prebuilt\n      contracts for common intents available in\n      {@link androidx.activity.result.contract.ActivityResultContracts}, provides hooks for\n      testing, and allow receiving results in separate, testable classes independent from your\n      activity. Use\n      {@link #registerForActivityResult(ActivityResultContract, ActivityResultCallback)}\n      with the appropriate {@link ActivityResultContract} and handling the result in the\n      {@link ActivityResultCallback#onActivityResult(Object) callback}.")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE) {
            onfidoWorkflow.handleActivityResult(resultCode, data, object : OnfidoWorkflow.ResultListener {
                override fun onUserCompleted() {
                    exitApp()
                    isVerificationComplete = true
                }

                override fun onUserExited(exitCode: ExitCode) {
                    showToast("Exited")
                }

                override fun onException(exception: OnfidoWorkflow.WorkflowException) {
                    showToast("Verification failed")
//                    Log.e("Error", exception.message)
                }
            })
        }
    }

    private fun exitApp() {
        isDialogShown = true
        Utility.exitApp(this, false, object : AlertCallback {
            override fun response(result: Boolean) {
                if (result) {
                    finishAffinity()
                    isDialogShown = false
                }
            }
        })
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun requestPermissions() {
        val permissionsNeeded = mutableListOf<String>()

        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                permissionsNeeded.add(Manifest.permission.CAMERA)
                permissionsNeeded.add(Manifest.permission.RECORD_AUDIO)
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> {
                permissionsNeeded.add(Manifest.permission.CAMERA)
                permissionsNeeded.add(Manifest.permission.READ_EXTERNAL_STORAGE)
                permissionsNeeded.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                permissionsNeeded.add(Manifest.permission.RECORD_AUDIO)
            }
            else -> {
                permissionsNeeded.addAll(REQUIRED_PERMISSIONS) // For devices below Android 10
            }
        }

        permissionsNeeded.removeAll { ActivityCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED }

        if (permissionsNeeded.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, permissionsNeeded.toTypedArray(), REQUEST_PERMISSIONS)
        } else {
            showInputDialog()
        }
    }




    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_PERMISSIONS) {
            if (grantResults.isNotEmpty()) {
                if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                    showInputDialog()
                } else {
                    showInputDialog()
                }
            }
        }
    }




//    private fun showPermissionDeniedDialog() {
//
//        if (isDialogShown) {return}
//        isDialogShown = true
//
//        Utility.dialogPermissionDenied(this, false, object : AlertCallback {
//            override fun response(result: Boolean) {
//                if (result) {
//                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
//                    val uri = Uri.fromParts("package", packageName, null)
//                    intent.data = uri
//                    startActivity(intent)
//                    isDialogShown = false
//
//                    if (REQUIRED_PERMISSIONS.all { ActivityCompat.checkSelfPermission(this@MainActivity, it) == PackageManager.PERMISSION_GRANTED }) {
//                        showInputDialog()
//                    }
//
//                } else {
//                    finishAffinity()
//                    isDialogShown = false
//                }
//            }
//        })
//    }

    companion object {
        private const val REQUEST_PERMISSIONS = 1
        private val REQUIRED_PERMISSIONS = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
        const val REQUEST_CODE = 0x05
    }
    override fun onResume() {
        super.onResume()

        val allPermissionsGranted = REQUIRED_PERMISSIONS.all {
            val permissionStatus = ActivityCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
            Log.e("Permissions Check", "Permission: $it, Granted: $permissionStatus")
            permissionStatus
        }

        Log.e("Permissions", "All permissions granted: $allPermissionsGranted")

        if (allPermissionsGranted) {
            if (!isClientCreated && !isVerificationComplete && !isDialogShown) {
                showInputDialog()
            }
        } else {
//            if (!isDialogShown) {
//                val deniedPermissions = REQUIRED_PERMISSIONS.filter {
//                    ActivityCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
//                }
//                Log.e("Permissions", "Denied permissions: $deniedPermissions")
//
////                if (deniedPermissions.isNotEmpty()) {
////                    showPermissionDeniedDialog()
////                }
//            }
        }
    }






}
