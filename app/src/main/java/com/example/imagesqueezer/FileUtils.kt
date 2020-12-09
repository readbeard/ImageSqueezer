package com.example.imagesqueezer

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import me.shouheng.compress.Compress
import me.shouheng.compress.listener.CompressListener
import me.shouheng.compress.strategy.Strategies
import java.io.File

class FileUtils {
    companion object {
        private val TAG = FileUtils::class.simpleName

        fun compressFile(file: File, last: Boolean, activity: AppCompatActivity) {
            val bmOptions = BitmapFactory.Options()
            val image = BitmapFactory.decodeFile(file.absolutePath, bmOptions)
            val compress = Compress.with(activity, file)
            val resolution = PreferenceUtils(activity).getResolutionParameter()
            val quality = PreferenceUtils(activity).getQualityParameter()

            compress
                    .setQuality(quality)
                    .setTargetDir(file.parentFile?.absolutePath)
                    .setCompressListener(object : CompressListener {
                        override fun onStart() {
                            Log.d(TAG, activity.getString(R.string.mainactivity_compressionstarted, file
                                    .absolutePath))
                        }

                        override fun onSuccess(result: File?) {
                            if (result == null) {
                                Log.e(TAG, activity.getString(R.string.mainactivity_compressionsuccess_null))
                                return
                            }
                            Log.d(TAG, activity.getString(R.string.compressionsuccess, result.absolutePath))
                            result.renameTo(file)
                            if (last) {
                                activity.finish()
                            }
                        }

                        override fun onError(throwable: Throwable?) {
                            Log.e(TAG, activity.getString(
                                            R.string.mainactivity_compressionerror,
                                            file.absolutePath,
                                            throwable?.message
                                    ))
                        }
                    })
                    .strategy(Strategies.compressor())
                    .setMaxWidth(if (image.width > image.height) resolution.first else resolution.second)
                    .setMaxHeight(if (image.width > image.height) resolution.second else resolution.first)
                    .launch()
        }

        fun retrieveUriFromFile(file: File, context: Context): Uri {
            return FileProvider.getUriForFile(
                    context,
                    BuildConfig.APPLICATION_ID + ".provider",
                    file
            )
        }

        fun deleteDir(dir: File): Boolean {
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
    }
}