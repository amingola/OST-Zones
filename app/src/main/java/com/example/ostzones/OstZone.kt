package com.example.ostzones

import com.google.android.gms.maps.model.Polygon

class OstZone(val polygon: Polygon, var name: String?) {

    var isSelected = false
}