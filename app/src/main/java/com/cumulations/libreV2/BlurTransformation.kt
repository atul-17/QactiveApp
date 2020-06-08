package com.cumulations.libreV2

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import androidx.renderscript.Allocation
import androidx.renderscript.Element
import androidx.renderscript.RSRuntimeException
import androidx.renderscript.RenderScript
import androidx.renderscript.ScriptIntrinsicBlur

import com.squareup.picasso.Transformation

class BlurTransformation(private val context: Context,
                         private val mRadius: Int = MAX_RADIUS,
                         private val mSampling: Int = DEFAULT_DOWN_SAMPLING) : Transformation {

//    private val mContext: Context = context.applicationContext

    companion object {
        const val MAX_RADIUS = 25
        const val DEFAULT_DOWN_SAMPLING = 1
    }

    override fun transform(source: Bitmap): Bitmap {

        val scaledWidth = source.width / mSampling
        val scaledHeight = source.height / mSampling

        var bitmap = Bitmap.createBitmap(scaledWidth, scaledHeight, Bitmap.Config.ARGB_8888)

        val canvas = Canvas(bitmap)
        canvas.scale(1 / mSampling.toFloat(), 1 / mSampling.toFloat())
        val paint = Paint()
        paint.flags = Paint.FILTER_BITMAP_FLAG
        canvas.drawBitmap(source, 0f, 0f, paint)

        /*For adding gradient background*/
        /*int actualWidth = source.getWidth();
//        We need 1/4th height to be gradient
        int actualHeight = source.getHeight();
        float gradientHeight = actualHeight/4;
        Paint paint2 = new Paint();
        int topColor = 0xFFFFFFFF;
        int bottomColor = 0x00FFFFFF;
        LinearGradient shader = new LinearGradient(0, gradientHeight, 0, actualHeight, topColor, bottomColor, Shader.TileMode.REPEAT);
        paint.setShader(shader);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_IN));
        canvas.drawRect(0, 0, actualWidth, gradientHeight, paint2);*/

        try {
            bitmap = getBlurredBitmap(context, bitmap, mRadius)
        } catch (e: RSRuntimeException) {
            e.printStackTrace()
        }

        source.recycle()

        return bitmap
    }

    override fun key(): String {
        return "BlurTransformation(radius=$mRadius, sampling=$mSampling)"
    }

    @Throws(RSRuntimeException::class)
    private fun getBlurredBitmap(context: Context, bitmap: Bitmap, radius: Int): Bitmap {
        var renderScript: RenderScript? = null
        var input: Allocation? = null
        var output: Allocation? = null
        var blur: ScriptIntrinsicBlur? = null
        try {
            renderScript = RenderScript.create(context)
            renderScript!!.messageHandler = RenderScript.RSMessageHandler()
            input = Allocation.createFromBitmap(renderScript, bitmap, Allocation.MipmapControl.MIPMAP_NONE,
                    Allocation.USAGE_SCRIPT)
            output = Allocation.createTyped(renderScript, input!!.type)
            blur = ScriptIntrinsicBlur.create(renderScript, Element.U8_4(renderScript))

            blur!!.setInput(input)
            blur.setRadius(radius.toFloat())
            blur.forEach(output!!)
            output.copyTo(bitmap)
        } finally {
            renderScript?.destroy()
            input?.destroy()
            output?.destroy()
            blur?.destroy()
        }

        return bitmap
    }
}