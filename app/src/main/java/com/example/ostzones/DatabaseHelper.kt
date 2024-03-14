import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.example.ostzones.OstZone
import com.google.android.gms.maps.model.LatLng
import org.json.JSONArray
import org.json.JSONObject

class DatabaseHelper(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_VERSION = 1
        private const val DATABASE_NAME = "OstZones"
        private const val TABLE_NAME = "ost_zones"
        private const val ROW_ID = "rowId"
        private const val KEY_ID = "id"
        private const val KEY_POINTS = "points"
        private const val KEY_POLYGON_OPTIONS = "polygon_options"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createTableQuery =
            "CREATE TABLE $TABLE_NAME ($KEY_ID INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, $KEY_POINTS TEXT, $KEY_POLYGON_OPTIONS TEXT)"
        db.execSQL(createTableQuery)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_NAME")
        onCreate(db)
    }

    fun saveOstZone(ostZone: OstZone): OstZone {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            ostZone.id?.let { put(KEY_ID, it) }
            put(KEY_POINTS, serializePoints(ostZone.polygonPoints))
            put(KEY_POLYGON_OPTIONS, serializePolygonOptions(ostZone.polygonOptions))
        }
        val row = db.insert(TABLE_NAME, null, values)
        db.close()
        return ostZone.apply{ id = row }
    }

    fun updateOstZone(ostZone: OstZone): OstZone{
        val db = this.writableDatabase
        val values = ContentValues().apply {
            ostZone.id?.let { put(KEY_ID, it) }
            put(KEY_POINTS, serializePoints(ostZone.polygonPoints))
            put(KEY_POLYGON_OPTIONS, serializePolygonOptions(ostZone.polygonOptions))
        }
        db.update(TABLE_NAME,  values, "$KEY_ID = ?", arrayOf(ostZone.id.toString()))
        db.close()
        return ostZone
    }

    fun saveAllOstZones(usersOstZones: MutableCollection<OstZone>) {
        clearAllData()
        val db = this.writableDatabase

        for(ostZone in usersOstZones){
            val values = ContentValues().apply {
                //TODO update this with saving the other data in OstZone (not just the polygon)
                ostZone.id?.let { put(KEY_ID, it) }
                put(KEY_POINTS, serializePoints(ostZone.polygonPoints))
                put(KEY_POLYGON_OPTIONS, serializePolygonOptions(ostZone.polygonOptions))
            }
            db.insert(TABLE_NAME, null, values)
        }
        db.close()
    }

    //TODO update this with retrieving the other data in OstZone (not just the polygon)
    fun getAllOstZones(): List<OstZone> {
        val allOstZones = mutableListOf<OstZone>()
        val db = this.readableDatabase
        val cursor = db.rawQuery("SELECT * FROM $TABLE_NAME", null)

        if (cursor.moveToFirst()) {
            do {
                allOstZones.add(createOstZoneFromCursor(cursor))
            } while (cursor.moveToNext())
        }

        cursor.close()
        return allOstZones
    }

    fun removePolygon(id: Long): Int{
        val db = this.writableDatabase
        val result = db.delete(TABLE_NAME, "$KEY_ID = ?", arrayOf(id.toString()))
        db.close()
        return result
    }

    @SuppressLint("Range")
    private fun createOstZoneFromCursor(cursor: Cursor): OstZone {
        val pointsJson = cursor.getString(cursor.getColumnIndex(KEY_POINTS))
        val polygonOptionsJson = cursor.getString(cursor.getColumnIndex(KEY_POLYGON_OPTIONS))
        val points = deserializePoints(pointsJson)
        val polygonOptions = deserializePolygonOptions(polygonOptionsJson)
        return OstZone(points, polygonOptions, "The Circle").apply {
            id = cursor.getLong(cursor.getColumnIndex(KEY_ID))
        }
    }

    private fun serializePoints(points: List<LatLng>): String {
        val jsonArray = JSONArray()
        points.forEach { point ->
            jsonArray.put("${point.latitude},${point.longitude}")
        }
        return jsonArray.toString()
    }

    private fun serializePolygonOptions(polygonOptions: HashMap<String, Any>): String {
        val jsonObject = JSONObject()
        polygonOptions.forEach { opt ->
            jsonObject.put(opt.key, opt.value)
        }
        return jsonObject.toString()
    }

    private fun deserializePoints(pointsJson: String): List<LatLng> {
        val jsonArray = JSONArray(pointsJson)
        val points = mutableListOf<LatLng>()
        for (i in 0 until jsonArray.length()) {
            val pointString = jsonArray.getString(i)
            val (lat, lng) = pointString.split(",").map { it.toDouble() }
            points.add(LatLng(lat, lng))
        }
        return points
    }

    private fun deserializePolygonOptions(polygonOptionsJson: String): HashMap<String, Any> {
        val jsonObject = JSONObject(polygonOptionsJson)
        val polygonOptions = hashMapOf<String, Any>()
        for(key in jsonObject.keys()){
            polygonOptions[key] = jsonObject[key]
        }
        return polygonOptions
    }

    fun clearAllData() {
        val db = this.writableDatabase
        db.delete(TABLE_NAME, null, null)
        db.close()
    }
}