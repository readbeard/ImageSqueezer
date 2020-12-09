package com.example.imagesqueezer

import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.IAdapter
import com.mikepenz.fastadapter.adapters.ItemAdapter
import com.mikepenz.fastadapter.helpers.ActionModeHelper
import com.mikepenz.fastadapter.helpers.UndoHelper
import com.mikepenz.fastadapter.select.getSelectExtension

class FastAdapterInitializr() {
    private lateinit var mActionModeHelper: ActionModeHelper<ChosenImageRvItem>

    fun initializeFastadapter(
        activity: AppCompatActivity, itemAdapter: ItemAdapter<ChosenImageRvItem>,
    ): FastAdapter<ChosenImageRvItem> {
        val fastAdapter = FastAdapter.with(itemAdapter)

        fastAdapter.onPreClickListener =
            { _: View?, _: IAdapter<ChosenImageRvItem>, itemChosen: ChosenImageRvItem, _: Int ->
                val res = mActionModeHelper.onClick(itemChosen)
                res ?: false
            }

        fastAdapter.onPreLongClickListener =
            { _: View, _: IAdapter<ChosenImageRvItem>, _: ChosenImageRvItem, position: Int ->
                val actionMode = mActionModeHelper.onLongClick(activity, position)
                if (actionMode != null) {
                    activity.findViewById<View>(R.id.action_mode_bar).setBackgroundColor(ContextCompat.getColor
                    (activity,
                            R.color.purple_500))
                }
                actionMode != null
            }

        val selectExtension = fastAdapter.getSelectExtension()
        selectExtension.apply {
            isSelectable = true
            multiSelect = true
            selectOnLongClick = true
        }

        mActionModeHelper = ActionModeHelper(fastAdapter, R.menu.cab, ActionBarCallBack(activity, fastAdapter))
        initializeUndoHelper(fastAdapter)
        return fastAdapter as FastAdapter<ChosenImageRvItem>
    }

    private fun initializeUndoHelper(fastAdapter: FastAdapter<ChosenImageRvItem>): UndoHelper<*> {
        return UndoHelper(fastAdapter, object : UndoHelper.UndoListener<ChosenImageRvItem> {
            override fun commitRemove(
                positions: Set<Int>,
                removed: ArrayList<FastAdapter.RelativeInfo<ChosenImageRvItem>>
            ) {
                Log.e("UndoHelper", "Positions: " + positions.toString() + " Removed: " + removed.size)
            }
        })
    }

}