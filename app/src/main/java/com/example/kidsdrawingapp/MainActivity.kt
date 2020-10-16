package com.example.kidsdrawingapp

import android.Manifest
import android.app.Activity
import android.app.Application
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.drm.DrmStore
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.media.MediaScannerConnection
import android.os.AsyncTask
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.View
import android.widget.ImageButton
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.startActivity
import androidx.core.view.get
import com.google.android.material.internal.ContextUtils.getActivity
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.activity_main.view.*
import kotlinx.android.synthetic.main.dialog_brush_size.*
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.net.URI
import java.nio.file.attribute.FileAttribute
import java.security.AccessController.getContext

class MainActivity : AppCompatActivity() {
    private var mImageButtonCurrentPaintColors: ImageButton?=null

//    lateinit var str: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        drawing_view.selectBrushSize(20.toFloat())

        mImageButtonCurrentPaintColors=paint_colors[1] as ImageButton
        mImageButtonCurrentPaintColors!!.setImageDrawable(
                ContextCompat.getDrawable(this,R.drawable.pallet_pressed)
        )
        ib_brush.setOnClickListener {
            showBrushSizeChooserDialog()
        }

        ib_gallery.setOnClickListener { view->
            if(isPermissionAllowed()){

                val PhotoClickIntent=Intent(Intent.ACTION_PICK,MediaStore.Images.Media.EXTERNAL_CONTENT_URI)

                startActivityForResult(PhotoClickIntent,GALLERY)
            }else {
                requestStoragePermission()
            }
        }

        ib_undo.setOnClickListener { view->
            drawing_view.undoChanges()
        }

        ib_save.setOnClickListener {
            if(isPermissionAllowed()){
                BitmapAsyncTask(getBitmapFromView(drawingView_container),this@MainActivity).execute()

            }else{
                requestStoragePermission()
            }
        }


        
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if(resultCode== Activity.RESULT_OK){
            if(requestCode== GALLERY) {
                try {
                    if (data!!.data != null) {
                        iv_background.visibility=View.VISIBLE
                        iv_background.setImageURI(data.data)
                    } else {
                        Toast.makeText(this, "Error in Parsing the image or it's corrupted", Toast.LENGTH_LONG).show()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    fun requestStoragePermission(){
        if(ActivityCompat.shouldShowRequestPermissionRationale(this, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE,Manifest.permission.WRITE_EXTERNAL_STORAGE).toString())){
            Toast.makeText(this,"Need Permission to add backgroun image",Toast.LENGTH_LONG).show()
        }else{
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE,Manifest.permission.WRITE_EXTERNAL_STORAGE), STORAGE_PERMISSION_CODE)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if(requestCode== STORAGE_PERMISSION_CODE){
            if(grantResults.isNotEmpty() && grantResults[0]==PackageManager.PERMISSION_GRANTED){
                Toast.makeText(this,"Permission granted now you can read the storage ",Toast.LENGTH_LONG).show()
            }else{
                Toast.makeText(this,"Oops you denied the permission",Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun showBrushSizeChooserDialog(){
        val brushdialog = Dialog(this)
        brushdialog.setContentView(R.layout.dialog_brush_size)
        val smallbtn = brushdialog.bt_small
        smallbtn.setOnClickListener {
            drawing_view.selectBrushSize(10.toFloat())
            brushdialog.dismiss()
        }
        val mediumbtn=brushdialog.bt_medium
        mediumbtn.setOnClickListener {
            drawing_view.selectBrushSize(20.toFloat())
            brushdialog.dismiss()
        }
        val largebtn=brushdialog.bt_large
        largebtn.setOnClickListener {
            drawing_view.selectBrushSize(30.toFloat())
            brushdialog.dismiss()
        }
         brushdialog.show()
    }

    fun paintClicked(view:View){
        if(view != mImageButtonCurrentPaintColors){
            val imageButton = view as ImageButton
            val colorTag=imageButton.tag.toString()
            drawing_view.selectColor(colorTag)
            imageButton.setImageDrawable(
                    ContextCompat.getDrawable(this,R.drawable.pallet_pressed)
            )
            mImageButtonCurrentPaintColors!!.setImageDrawable(
                    ContextCompat.getDrawable(this,R.drawable.pallet_normal)
            )

            mImageButtonCurrentPaintColors = view
        }
    }

    fun isPermissionAllowed(): Boolean{
        val result=ContextCompat.checkSelfPermission(this@MainActivity,Manifest.permission.READ_EXTERNAL_STORAGE)

        return result == PackageManager.PERMISSION_GRANTED
    }


    private fun getBitmapFromView(view:View):Bitmap{
        val returnedBitmap = Bitmap.createBitmap(view.width,view.height,Bitmap.Config.ARGB_8888)
        val canvas=Canvas(returnedBitmap)
        val bgdrawable=view.background
        if( bgdrawable != null){
            bgdrawable.draw(canvas)
        }else{
            canvas.drawColor(Color.WHITE)
        }

        view.draw(canvas)

        return returnedBitmap
    }

    private class BitmapAsyncTask(val mBitmap:Bitmap,val context:Context):AsyncTask<Any, Void, String>(){


        val mProgressDialog=Dialog(context)

        override fun onPreExecute() {
            super.onPreExecute()

            mProgressDialog.setContentView(R.layout.custom_progress_dialog)

            mProgressDialog.show()

        }

        override fun doInBackground(vararg params: Any?): String {
            var result=""

            if(mBitmap!=null){
                try {
                    val bytes= ByteArrayOutputStream()
                    mBitmap.compress(Bitmap.CompressFormat.PNG,90,bytes)
                    val f= File(context.externalCacheDir!!.absolutePath.toString()+File.separator+"KidsDrawingApp_"+System.currentTimeMillis()/1000+".png")
                    val fos= FileOutputStream(f)
                    fos.write(bytes.toByteArray())
                    fos.close()

                    result = f.absolutePath
                }catch (e: Exception){
                    result=""
                    e.printStackTrace()
                }
            }


            return result
        }


        override fun onPostExecute(result: String?) {
            super.onPostExecute(result)

            mProgressDialog.dismiss()
            if (result!=null){
                Toast.makeText(context,"Image saved successfully in : $result",Toast.LENGTH_SHORT).show()
            }else{
                Toast.makeText(context,"Something went wrong",Toast.LENGTH_SHORT).show()
            }

            MediaScannerConnection.scanFile(context,arrayOf(result),null){
                path, uri -> val shareIntent = Intent()
                shareIntent.action=Intent.ACTION_SEND
                shareIntent.putExtra(Intent.EXTRA_STREAM,uri)
                shareIntent.type="image/png"

   //             CurrentActivity.context.startActivity(startActivity(context,Intent.createChooser(shareIntent,"share to:"),null)
            }

        }



    }

    companion object{
        const val STORAGE_PERMISSION_CODE=1
        const val GALLERY=2
    }
}
