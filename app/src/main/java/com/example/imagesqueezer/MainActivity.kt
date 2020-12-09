package com.example.imagesqueezer

import android.app.Activity
import android.content.*
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.*
import android.widget.Button
import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar
import com.kbeanie.multipicker.api.ImagePicker
import com.kbeanie.multipicker.api.Picker
import com.kbeanie.multipicker.api.callbacks.ImagePickerCallback
import com.kbeanie.multipicker.api.entity.ChosenImage
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.IAdapter
import com.mikepenz.fastadapter.ISelectionListener
import com.mikepenz.fastadapter.adapters.ItemAdapter
import com.mikepenz.fastadapter.helpers.ActionModeHelper
import com.mikepenz.fastadapter.helpers.UndoHelper
import com.mikepenz.fastadapter.select.SelectExtension
import com.mikepenz.fastadapter.select.getSelectExtension
import java.io.File


class MainActivity : AppCompatActivity(), ImagePickerCallback,
    ActivityCompat.OnRequestPermissionsResultCallback {
    private lateinit var fastAdapter: FastAdapter<ChosenImageRvItem>
    private lateinit var imagePicker: ImagePicker
    private lateinit var recyclerViewResults: RecyclerView
    private lateinit var loadingProgressBar: ProgressBar
    private lateinit var importPicturesButton: Button
    private lateinit var permissionHandler: PermissionHandler
    private lateinit var mActionModeHelper: ActionModeHelper<ChosenImageRvItem>
    private lateinit var mUndoHelper: UndoHelper<*>
    private lateinit var selectExtension: SelectExtension<ChosenImageRvItem>
    private var toolbar: Toolbar? = null

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

        recyclerViewResults = findViewById(R.id.lvResults);
        loadingProgressBar = findViewById(R.id.progressBar_cyclic)
        importPicturesButton = findViewById(R.id.button_contentmain_pick)
        toolbar = findViewById<Toolbar>(R.id.toolbar)

        FileUtils.deleteDir(File(Environment.getExternalStorageDirectory().absolutePath + getString(R.string.mainactivity_pictures_path)))

        permissionHandler = PermissionHandler(this)
        permissionHandler.checkPermissions()
    }

    private fun setIntentForResultAndFinish() {
        val intent = Intent()
        if (recyclerViewResults.adapter == null) {
            finish()
            return
        }
        val results = recyclerViewResults.adapter as FastAdapter<ChosenImageRvItem>
        val files = ArrayList<Uri>()

        val first = retrieveFileFromChosenImageAdapter(0, results)
        recyclerViewResults.visibility = View.GONE
        loadingProgressBar.visibility = View.VISIBLE
        FileUtils.compressFile(first, results.itemCount == 1, this)
        var uri = FileUtils.retrieveUriFromFile(first, this)
        setClipDataUri(uri, intent)

        for (i in 1 until results.itemCount) {
            val file = retrieveFileFromChosenImageAdapter(i, results)
            FileUtils.compressFile(file, i == results.itemCount - 1, this)
            uri = FileUtils.retrieveUriFromFile(file, this)
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
        results: FastAdapter<ChosenImageRvItem>
    ): File {
        return File((results.getItem(atIndex) as ChosenImageRvItem).chosenImage.originalPath)
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

    override fun onResume() {
        super.onResume()
        permissionHandler.checkPermissions()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (!permissionHandler.allPermissionsGranted()) {
            for (permission in permissions) {
                if (permissionHandler.neverAskAgainSelected(this, permission)) {
                    permissionHandler.displayNeverAskAgainDialog(permission)
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == Picker.PICK_IMAGE_DEVICE || requestCode == REQUEST_PHOTO_FROM_GOOGLE_PHOTOS) {
                loadingProgressBar.visibility = View.VISIBLE
                importPicturesButton.visibility = View.GONE
                imagePicker.submit(data)
            }
        }
    }

    override fun onError(p0: String?) {
    }

    override fun onImagesChosen(images: List<ChosenImage>) {
        val itemAdapter = ItemAdapter<ChosenImageRvItem>()
        for (chosenImage in images) {
            itemAdapter.add(ChosenImageRvItem(chosenImage))
        }
        fastAdapter = FastAdapter.with(itemAdapter)
        fastAdapter.onPreClickListener =
            { _: View?, _: IAdapter<ChosenImageRvItem>, itemChosen: ChosenImageRvItem, _: Int ->
                //we handle the default onClick behavior for the actionMode. This will return null if it didn't do anything and you can handle a normal onClick
                val res = mActionModeHelper.onClick(itemChosen)
                res ?: false
            }

        fastAdapter.onPreLongClickListener =
            { _: View, _: IAdapter<ChosenImageRvItem>, _: ChosenImageRvItem, position: Int ->
                val actionMode = mActionModeHelper.onLongClick(this@MainActivity, position)
                if (actionMode != null) {
                    findViewById<View>(R.id.action_mode_bar).setBackgroundColor(ContextCompat.getColor(this,
                            R.color.purple_500))
                }
                actionMode != null
            }

        mUndoHelper = UndoHelper(fastAdapter, object : UndoHelper.UndoListener<ChosenImageRvItem> {
            override fun commitRemove(
                positions: Set<Int>,
                removed: ArrayList<FastAdapter.RelativeInfo<ChosenImageRvItem>>
            ) {
                Log.e("UndoHelper", "Positions: " + positions.toString() + " Removed: " + removed.size)
            }
        })

        selectExtension = fastAdapter.getSelectExtension()
        selectExtension.apply {
            isSelectable = true
            multiSelect = true
            selectOnLongClick = true
            selectionListener = object : ISelectionListener<ChosenImageRvItem> {
                override fun onSelectionChanged(itemChosen: ChosenImageRvItem, selected: Boolean) {
                    Log.e("FastAdapter", "SelectedCount: " + selectExtension.selections.size + " ItemsCount: " +
                            selectExtension.selectedItems.size)


                }
            }

        }

        mActionModeHelper = ActionModeHelper(fastAdapter, R.menu.cab, ActionBarCallBack())
        recyclerViewResults.adapter = fastAdapter
        recyclerViewResults.viewTreeObserver.addOnGlobalLayoutListener(object :
            ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                loadingProgressBar.visibility = View.GONE
                recyclerViewResults.viewTreeObserver
                        .removeOnGlobalLayoutListener(this)
            }
        })
    }

    private fun launchGooglePhotosPicker(callingActivity: Activity?) {
        if (callingActivity != null && isGooglePhotosInstalled(this)) {
            val intent = Intent()
            intent.action = Intent.ACTION_PICK
            intent.type = "image/*"
            val resolveInfoList = callingActivity.packageManager.queryIntentActivities(intent, 0)
            for (i in resolveInfoList.indices) {
                if (resolveInfoList[i] != null) {
                    val packageName = resolveInfoList[i]!!.activityInfo.packageName
                    if (GOOGLE_PHOTOS_PACKAGE_NAME == packageName) {
                        intent.component =
                            ComponentName(packageName, resolveInfoList[i]!!.activityInfo.name)
                        callingActivity.startActivityForResult(
                                intent,
                                REQUEST_PHOTO_FROM_GOOGLE_PHOTOS
                        )
                        return
                    }
                }
            }
        }
    }

    private fun isGooglePhotosInstalled(context: Context): Boolean {
        val packageManager = context.packageManager
        return try {
            packageManager.getPackageInfo(
                    GOOGLE_PHOTOS_PACKAGE_NAME,
                    PackageManager.GET_ACTIVITIES
            ) != null
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }


    internal inner class ActionBarCallBack : androidx.appcompat.view.ActionMode.Callback {
        override fun onCreateActionMode(mode: androidx.appcompat.view.ActionMode?, menu: Menu?): Boolean {
            return true
        }

        override fun onPrepareActionMode(mode: androidx.appcompat.view.ActionMode?, menu: Menu?): Boolean {
            return false
        }

        override fun onActionItemClicked(mode: androidx.appcompat.view.ActionMode?, item: MenuItem?): Boolean {
            when (item?.itemId) {
                R.id.item_delete -> {
                    mUndoHelper.remove(findViewById(android.R.id.content),
                            "Item removed",
                            "Undo",
                            Snackbar.LENGTH_LONG,
                            selectExtension.selections)
                            .addCallback(object : BaseTransientBottomBar.BaseCallback<Snackbar>() {
                                override fun onShown(transientBottomBar: Snackbar?) {
                                    super.onShown(transientBottomBar)
                                }

                                override fun onDismissed(transientBottomBar: Snackbar?, event: Int) {
                                    super.onDismissed(transientBottomBar, event)
                                    if (event != Snackbar.Callback.DISMISS_EVENT_TIMEOUT) {
                                        selectExtension.deselect()
                                    }
                                }
                            })
                    mode!!.finish()
                }
                R.id.item_selectall -> {
                    return if (!item.isChecked) {
                        performSelectAllClick(true, mode, item)
                        true
                    } else {
                        performSelectAllClick(false, mode, item)
                        true
                    }
                }
            }

            return true
        }

        override fun onDestroyActionMode(mode: androidx.appcompat.view.ActionMode?) {
        }

        private fun performSelectAllClick(checked: Boolean, mode: androidx.appcompat.view.ActionMode?, item: MenuItem?) {
            if (checked) {
                item?.icon = ResourcesCompat.getDrawable(resources, R.drawable.ic_checkbox_checked, theme)
                for (i in 0..fastAdapter.itemCount) {
                    selectExtension.select(i)
                }
                item?.isChecked = true
            } else {
                item?.icon = ResourcesCompat.getDrawable(resources, R.drawable.ic_checkbox_unchecked, theme)
                selectExtension.deselect()
                item?.isChecked = false
            }
            mode?.title = selectExtension.selectedItems.size.toString()
        }
    }

    companion object {
        private val TAG = MainActivity::class.simpleName
        private const val REQUEST_PHOTO_FROM_GOOGLE_PHOTOS = 2
        private const val GOOGLE_PHOTOS_PACKAGE_NAME = "com.google.android.apps.photos"
    }
}

