package com.example.imagesqueezer

import android.app.Activity
import android.content.ClipData
import android.content.ClipDescription
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.ListView
import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.preference.PreferenceManager
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.kbeanie.multipicker.api.ImagePicker
import com.kbeanie.multipicker.api.Picker
import com.kbeanie.multipicker.api.callbacks.ImagePickerCallback
import com.kbeanie.multipicker.api.entity.ChosenImage
import me.shouheng.compress.Compress
import me.shouheng.compress.listener.CompressListener
import me.shouheng.compress.strategy.Strategies
import java.io.File


class MainActivity : AppCompatActivity(), ImagePickerCallback,
    ActivityCompat.OnRequestPermissionsResultCallback {
    private lateinit var imagePicker: ImagePicker
    private lateinit var lvResults: ListView
    private lateinit var loadingProgressBar: ProgressBar
    private lateinit var importPicturesButton: Button

    private val requiredPermissions: Array<String?>
        get() = try {
            val info = this.packageManager
                .getPackageInfo(this.packageName, PackageManager.GET_PERMISSIONS)
            val ps = info.requestedPermissions
            if (ps != null && ps.isNotEmpty()) {
                ps
            } else {
                arrayOfNulls(0)
            }
        } catch (e: Exception) {
            arrayOfNulls(0)
        }

    private val runtimePermissions: Unit
        get() {
            val allNeededPermissions: MutableList<String?> = ArrayList()
            for (permission in requiredPermissions) {
                if (!isPermissionGranted(this, permission)) {
                    allNeededPermissions.add(permission)
                }
            }
            if (allNeededPermissions.isNotEmpty()) {
                ActivityCompat.requestPermissions(
                    this,
                    allNeededPermissions.toTypedArray(),
                    PERMISSION_REQUESTS
                )
            }
        }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(findViewById(R.id.toolbar))

        findViewById<FloatingActionButton>(R.id.fab).setOnClickListener { view ->
            setIntentForResultAndFinish()
        }

        initializeImagePicker()

        findViewById<Button>(R.id.button_contentmain_pick).setOnClickListener {
            imagePicker.pickImage()
        }

        lvResults = findViewById(R.id.lvResults);
        loadingProgressBar = findViewById(R.id.progressBar_cyclic)
        importPicturesButton = findViewById(R.id.button_contentmain_pick)

        deleteDir(File(Environment.getExternalStorageDirectory().absolutePath + getString(R.string.mainactivity_pictures_path)))

        if (!allPermissionsGranted()) {
            runtimePermissions
        }
    }

    private fun setIntentForResultAndFinish() {
        val intent = Intent()
        if (lvResults.adapter == null) {
            finish()
            return
        }
        val results: MediaResultsAdapter = lvResults.adapter as MediaResultsAdapter
        val files = ArrayList<Uri>()

        val first = retrieveFileFromChosenImageAdapter(0, results)
        lvResults.visibility = View.GONE
        loadingProgressBar.visibility = View.VISIBLE
        compressFile(first, results.count == 1)
        var uri = retrieveUriFromFile(first)
        setClipDataUri(uri, intent)

        for (i in 1 until results.count) {
            val file = retrieveFileFromChosenImageAdapter(i, results)
            compressFile(file, i == results.count - 1)
            uri = retrieveUriFromFile(file)
            setClipDataUri(uri, intent)
            files.add(uri)
        }

        if (first.parentFile?.listFiles() != null) {
            for (f in first.parentFile.listFiles()) {
                if (f.name.contains(getString(R.string.mainactivity_thumbnail_postfix))) {
                    f.delete()
                }
            }
        }

        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        setResult(Activity.RESULT_OK, intent)
    }

    private fun retrieveFileFromChosenImageAdapter(
        atIndex: Int,
        results: MediaResultsAdapter
    ): File {
        return File((results.getItem(atIndex) as ChosenImage).originalPath)
    }

    private fun compressFile(file: File, last: Boolean) {
        val bmOptions = BitmapFactory.Options()
        val image = BitmapFactory.decodeFile(file.absolutePath, bmOptions)
        val compress = Compress.with(this, file)
        val resolutionHeight = getResolutionHeightParameter(this)
        val resolutionWidth = getResolutionWidthParameter(this)

        Log.e(TAG, "selected resolution width: $resolutionWidth and height $resolutionHeight and quality ${getQualityParameter(this)}")
        compress
            .setQuality(getQualityParameter(this))
            .setTargetDir(file.parentFile?.absolutePath)
            .setCompressListener(object : CompressListener {
                override fun onStart() {
                    Log.d(
                        TAG,
                        getString(R.string.mainactivity_compressionstarted, file.absolutePath)
                    )
                }

                override fun onSuccess(result: File?) {
                    if (result == null) {
                        Log.e(TAG, getString(R.string.mainactivity_compressionsuccess_null))
                        return
                    }
                    Log.d(TAG, getString(R.string.compressionsuccess, result.absolutePath))
                    result.renameTo(file)
                    if (last) {
                        finish()
                    }
                }

                override fun onError(throwable: Throwable?) {
                    Log.e(
                        TAG,
                        getString(
                            R.string.mainactivity_compressionerror,
                            file.absolutePath,
                            throwable?.message
                        )
                    )
                }
            })
            .strategy(Strategies.compressor())
            .setMaxWidth(if (image.width > image.height) resolutionWidth else resolutionHeight)
            .setMaxHeight(if (image.width > image.height) resolutionHeight else resolutionWidth)
            .launch()

    }

    private fun retrieveUriFromFile(file: File): Uri {
        return FileProvider.getUriForFile(
            this@MainActivity,
            BuildConfig.APPLICATION_ID + ".provider",
            file
        )
    }

    private fun setClipDataUri(uri: Uri, intent: Intent) {
        val mimeTypes = arrayOf(getString(R.string.mainactivity_image_mimetypes))
        if (intent.clipData == null) {
            intent.clipData = ClipData(
                ClipDescription("uri", mimeTypes),
                ClipData.Item(uri)
            )
        } else {
            intent.clipData!!.addItem(ClipData.Item(uri))
        }
    }

    private fun deleteDir(dir: File): Boolean {
        if (dir.isDirectory) {
            val children = dir.list() ?: return true
            for (i in children.indices) {
                val success = deleteDir(File(dir, children[i]))
                if (!success) {
                    return false
                }
            }
        }

        return dir.delete()
    }

    private fun allPermissionsGranted(): Boolean {
        for (permission in requiredPermissions) {
            if (!isPermissionGranted(this, permission)) {
                return false
            }
        }
        return true
    }


    private fun initializeImagePicker() {
        imagePicker = ImagePicker(this)
        imagePicker.allowMultiple()
        imagePicker.setImagePickerCallback(this)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        return when (item.itemId) {
            R.id.action_settings -> {
                val settingsIntent = Intent(this, SettingsActivity::class.java)
                startActivity(settingsIntent)
                return true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == Picker.PICK_IMAGE_DEVICE) {
                loadingProgressBar.visibility = View.VISIBLE
                importPicturesButton.visibility = View.GONE
                imagePicker.submit(data)
            }
        }
    }

    override fun onError(p0: String?) {
    }

    override fun onImagesChosen(images: List<ChosenImage>) {
        val adapter = MediaResultsAdapter(images, this)
        adapter.lifecycleOwner = this
        lvResults.adapter = adapter
        adapter.finishedLoadingLiveData.observe(this, {
            if (adapter.finishedLoadingLiveData.value == 1) {
                loadingProgressBar.visibility = View.GONE
            }
        })

    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        Log.i(TAG, getString(R.string.mainactivity_permissiongranted))
        if (allPermissionsGranted()) {
            //TODO implement
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    companion object {
        private val TAG = MainActivity::class.simpleName
        private const val PERMISSION_REQUESTS = 1
        private const val LOWEST_RESOLUTION_HEIGHT = 600f
        private const val LOWEST_RESOLUTION_WIDTH = 800f
        private const val FULL_HD_RESOLUTION_HEIGHT = 720f
        private const val FULL_HD_RESOLUTION_WIDTH = 1280f
        private const val SD_RESOLUTION_HEIGHT = 480f
        private const val SD_RESOLUTION_WIDTH = FULL_HD_RESOLUTION_HEIGHT

        private fun isPermissionGranted(
            context: Context,
            permission: String?
        ): Boolean {
            if (ContextCompat.checkSelfPermission(context, permission!!)
                == PackageManager.PERMISSION_GRANTED
            ) {
                Log.i(
                    TAG,
                    context.getString(R.string.mainactivity_permissiongranted_specific, permission)
                )
                return true
            }
            Log.i(
                TAG,
                context.getString(R.string.mainactivity_permissionnotgranted_specific, permission)
            )
            return false
        }

        //TODO: move those methods to a utils class

        private fun getResolutionHeightParameter(context: Context): Float {
            when (PreferenceManager.getDefaultSharedPreferences(context).getString("resolution", "null")) {
                context.getString(R.string.preferences_full_hd_res) -> {
                    return FULL_HD_RESOLUTION_HEIGHT
                }
                context.getString(R.string.preferences_sd_res) -> {
                    return SD_RESOLUTION_HEIGHT
                }
                context.getString(R.string.preferences_lowest_res) -> {
                    return LOWEST_RESOLUTION_HEIGHT
                }
            }

            return FULL_HD_RESOLUTION_HEIGHT
        }

        private fun getResolutionWidthParameter(context: Context): Float {
            when (PreferenceManager.getDefaultSharedPreferences(context).getString("resolution", "null")) {
                context.getString(R.string.preferences_full_hd_res) -> {
                    return FULL_HD_RESOLUTION_WIDTH
                }
                context.getString(R.string.preferences_sd_res) -> {
                    return SD_RESOLUTION_WIDTH
                }
                context.getString(R.string.preferences_lowest_res)-> {
                    return LOWEST_RESOLUTION_WIDTH
                }
            }
            return FULL_HD_RESOLUTION_WIDTH
        }

        private fun getQualityParameter(context: Context): Int {
            return PreferenceManager.getDefaultSharedPreferences(context).getInt("quality", 75)
        }
    }
}