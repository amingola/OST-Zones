package com.example.ostzones

import android.widget.SeekBar
import android.widget.TextView

open class RgbSeekBarOnChangeListener(
    private val mapsActivity: MapsActivity,
    private val label: TextView
) : SeekBar.OnSeekBarChangeListener {

    init {
        label.text = "0"
    }

    override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
        label.text = progress.toString()
        mapsActivity.updateSelectedPolygonColor()
    }

    override fun onStartTrackingTouch(seekBar: SeekBar?) {}

    override fun onStopTrackingTouch(seekBar: SeekBar?) {}

}